package com.htai.exe201phapluatso.admin.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.payment.entity.Payment;
import com.htai.exe201phapluatso.payment.repo.PaymentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for exporting admin data to CSV format
 * Supports UTF-8 with BOM for Vietnamese characters in Excel
 */
@Service
public class AdminCsvExportService {

    private static final Logger logger = LoggerFactory.getLogger(AdminCsvExportService.class);
    
    private static final int MAX_EXPORT_RECORDS = 10000;
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    private final UserRepo userRepo;
    private final PaymentRepo paymentRepo;

    public AdminCsvExportService(UserRepo userRepo, PaymentRepo paymentRepo) {
        this.userRepo = userRepo;
        this.paymentRepo = paymentRepo;
    }

    /**
     * Export users to CSV with optional filters
     * @param search Search by email or name
     * @param status Filter by status: active, banned
     * @return CSV file content as byte array
     */
    public byte[] exportUsersToCsv(String search, String status) {
        logger.info("Exporting users to CSV - search: {}, status: {}", search, status);
        
        // Build specification for filtering
        Specification<User> spec = buildUserSpecification(search, status);
        
        // Check record count first
        long totalCount = userRepo.count(spec);
        if (totalCount > MAX_EXPORT_RECORDS) {
            throw new BadRequestException(
                String.format("Số lượng bản ghi (%d) vượt quá giới hạn %d. Vui lòng áp dụng bộ lọc để giảm số lượng.", 
                    totalCount, MAX_EXPORT_RECORDS)
            );
        }
        
        // Fetch all matching users
        List<User> users = userRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return generateUsersCsv(users);
    }

    /**
     * Export payments to CSV with optional date range filter
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return CSV file content as byte array
     */
    public byte[] exportPaymentsToCsv(LocalDate fromDate, LocalDate toDate) {
        logger.info("Exporting payments to CSV - from: {}, to: {}", fromDate, toDate);
        
        // Build specification for filtering (without fetch for count)
        Specification<Payment> countSpec = buildPaymentCountSpecification(fromDate, toDate);
        
        // Check record count first
        long totalCount = paymentRepo.count(countSpec);
        if (totalCount > MAX_EXPORT_RECORDS) {
            throw new BadRequestException(
                String.format("Số lượng bản ghi (%d) vượt quá giới hạn %d. Vui lòng áp dụng bộ lọc ngày để giảm số lượng.", 
                    totalCount, MAX_EXPORT_RECORDS)
            );
        }
        
        // Fetch all matching payments with user and plan (with fetch)
        Specification<Payment> fetchSpec = buildPaymentFetchSpecification(fromDate, toDate);
        List<Payment> payments = paymentRepo.findAll(fetchSpec, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return generatePaymentsCsv(payments);
    }

    // ==================== PRIVATE METHODS ====================

    private Specification<User> buildUserSpecification(String search, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("fullName")), searchPattern)
                ));
            }
            
            // Status filter
            if (status != null && !status.trim().isEmpty()) {
                if ("active".equalsIgnoreCase(status)) {
                    predicates.add(cb.isTrue(root.get("active")));
                } else if ("banned".equalsIgnoreCase(status)) {
                    predicates.add(cb.isFalse(root.get("active")));
                }
            }
            
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Payment> buildPaymentCountSpecification(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Date range filter only - no fetch for count query
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }
            
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Payment> buildPaymentFetchSpecification(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Only fetch for non-count queries
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("user", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("plan", jakarta.persistence.criteria.JoinType.LEFT);
            }
            
            // Date range filter
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }
            
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private byte[] generateUsersCsv(List<User> users) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            
            // Write UTF-8 BOM
            baos.write(UTF8_BOM);
            
            // Write header
            writer.println("ID,Email,Họ tên,Provider,Email đã xác thực,Trạng thái,Ngày tạo");
            
            // Write data rows
            for (User user : users) {
                writer.println(String.join(",",
                    escapeCsvField(String.valueOf(user.getId())),
                    escapeCsvField(user.getEmail()),
                    escapeCsvField(user.getFullName()),
                    escapeCsvField(user.getProvider()),
                    escapeCsvField(user.isEmailVerified() ? "Có" : "Không"),
                    escapeCsvField(user.isActive() ? "Hoạt động" : "Bị khóa"),
                    escapeCsvField(formatDateTime(user.getCreatedAt()))
                ));
            }
            
            writer.flush();
            logger.info("Generated users CSV with {} records", users.size());
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating users CSV", e);
            throw new RuntimeException("Lỗi khi tạo file CSV: " + e.getMessage());
        }
    }

    private byte[] generatePaymentsCsv(List<Payment> payments) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            
            // Write UTF-8 BOM
            baos.write(UTF8_BOM);
            
            // Write header
            writer.println("ID,Mã đơn hàng,Email người dùng,Gói,Số tiền,Trạng thái,Phương thức,Ngày tạo,Ngày thanh toán");
            
            // Write data rows
            for (Payment payment : payments) {
                String userEmail = payment.getUser() != null ? payment.getUser().getEmail() : "N/A";
                String planCode = payment.getPlan() != null ? payment.getPlan().getCode() : "N/A";
                String amount = payment.getAmount() != null ? payment.getAmount().toString() : "0";
                String statusVi = translatePaymentStatus(payment.getStatus());
                
                writer.println(String.join(",",
                    escapeCsvField(String.valueOf(payment.getId())),
                    escapeCsvField(payment.getOrderId()),
                    escapeCsvField(userEmail),
                    escapeCsvField(planCode),
                    escapeCsvField(amount),
                    escapeCsvField(statusVi),
                    escapeCsvField(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A"),
                    escapeCsvField(formatDateTime(payment.getCreatedAt())),
                    escapeCsvField(formatDateTime(payment.getPaidAt()))
                ));
            }
            
            writer.flush();
            logger.info("Generated payments CSV with {} records", payments.size());
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating payments CSV", e);
            throw new RuntimeException("Lỗi khi tạo file CSV: " + e.getMessage());
        }
    }

    /**
     * Escape CSV field to handle commas, quotes, and newlines
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // If field contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String translatePaymentStatus(String status) {
        if (status == null) return "N/A";
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> "Thành công";
            case "PENDING" -> "Đang chờ";
            case "FAILED" -> "Thất bại";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}
