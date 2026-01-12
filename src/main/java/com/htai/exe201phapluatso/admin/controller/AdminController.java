package com.htai.exe201phapluatso.admin.controller;

import com.htai.exe201phapluatso.admin.dto.*;
import com.htai.exe201phapluatso.admin.service.AdminActivityLogService;
import com.htai.exe201phapluatso.admin.service.AdminCreditService;
import com.htai.exe201phapluatso.admin.service.AdminCsvExportService;
import com.htai.exe201phapluatso.admin.service.AdminService;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller for Admin Dashboard
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    // Whitelist allowed sort fields to prevent injection
    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of(
            "createdAt", "email", "fullName", "active", "enabled"
    );
    private static final Set<String> ALLOWED_PAYMENT_SORT_FIELDS = Set.of(
            "createdAt", "amount", "status", "paidAt"
    );

    private final AdminService adminService;
    private final AdminActivityLogService adminActivityLogService;
    private final AdminCsvExportService adminCsvExportService;
    private final AdminCreditService adminCreditService;

    public AdminController(AdminService adminService, AdminActivityLogService adminActivityLogService, 
                          AdminCsvExportService adminCsvExportService, AdminCreditService adminCreditService) {
        this.adminService = adminService;
        this.adminActivityLogService = adminActivityLogService;
        this.adminCsvExportService = adminCsvExportService;
        this.adminCreditService = adminCreditService;
    }

    /**
     * Validate and sanitize sort field against whitelist
     */
    private String validateSortField(String sort, Set<String> allowedFields, String defaultField) {
        if (sort == null || !allowedFields.contains(sort)) {
            return defaultField;
        }
        return sort;
    }

    // ==================== DASHBOARD STATISTICS ====================

    /**
     * GET /api/admin/stats
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getDashboardStats() {
        AdminStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/stats/revenue
     * Get revenue chart data
     * @param from Start date (default: 30 days ago)
     * @param to End date (default: today)
     */
    @GetMapping("/stats/revenue")
    public ResponseEntity<List<RevenueByDate>> getRevenueChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        
        List<RevenueByDate> data = adminService.getRevenueChart(from, to);
        return ResponseEntity.ok(data);
    }

    /**
     * GET /api/admin/stats/user-growth
     * Get user growth chart data
     * @param from Start date (default: 30 days ago)
     * @param to End date (default: today)
     */
    @GetMapping("/stats/user-growth")
    public ResponseEntity<List<UserGrowth>> getUserGrowthChart(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        
        List<UserGrowth> data = adminService.getUserGrowthChart(from, to);
        return ResponseEntity.ok(data);
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * GET /api/admin/users
     * Get all users with pagination and search
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param search Search query (email or name)
     * @param status Filter by status: active, banned (optional)
     * @param sort Sort field (default: createdAt)
     * @param direction Sort direction (default: DESC)
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        // Validate sort field against whitelist
        String validatedSort = validateSortField(sort, ALLOWED_USER_SORT_FIELDS, "createdAt");
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, validatedSort));
        
        Page<AdminUserListResponse> users = adminService.getAllUsers(pageable, search, status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users.getContent());
        response.put("currentPage", users.getNumber());
        response.put("totalItems", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());
        response.put("hasNext", users.hasNext());
        response.put("hasPrevious", users.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/users/{id}
     * Get user detail by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(@PathVariable Long id) {
        AdminUserDetailResponse user = adminService.getUserDetail(id);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/admin/users/{id}/ban
     * Ban a user
     */
    @PostMapping("/users/{id}/ban")
    public ResponseEntity<Map<String, String>> banUser(
            @PathVariable Long id,
            @Valid @RequestBody BanUserRequest request,
            @CurrentUser User adminUser
    ) {
        if (adminUser == null) {
            throw new com.htai.exe201phapluatso.common.exception.BadRequestException("Admin user not found in session");
        }
        adminService.banUser(id, request.getReason(), adminUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User banned successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/users/{id}/unban
     * Unban a user
     */
    @PostMapping("/users/{id}/unban")
    public ResponseEntity<Map<String, String>> unbanUser(
            @PathVariable Long id,
            @CurrentUser User adminUser
    ) {
        if (adminUser == null) {
            throw new com.htai.exe201phapluatso.common.exception.BadRequestException("Admin user not found in session");
        }
        adminService.unbanUser(id, adminUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User unbanned successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/admin/users/{id}
     * Delete a user (soft delete)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            @CurrentUser User adminUser
    ) {
        adminService.deleteUser(id, adminUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/users/export
     * Export users to CSV file
     * @param search Search by email or name (optional)
     * @param status Filter by status: active, banned (optional)
     */
    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsersToCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        byte[] csvContent = adminCsvExportService.exportUsersToCsv(search, status);
        
        String filename = "users_export_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(csvContent);
    }

    // ==================== PAYMENT MANAGEMENT ====================

    /**
     * GET /api/admin/payments
     * Get all payments with pagination
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sort Sort field (default: createdAt)
     * @param direction Sort direction (default: DESC)
     */
    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        // Validate sort field against whitelist
        String validatedSort = validateSortField(sort, ALLOWED_PAYMENT_SORT_FIELDS, "createdAt");
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, validatedSort));
        
        Page<AdminPaymentListResponse> payments = adminService.getAllPayments(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("payments", payments.getContent());
        response.put("currentPage", payments.getNumber());
        response.put("totalItems", payments.getTotalElements());
        response.put("totalPages", payments.getTotalPages());
        response.put("hasNext", payments.hasNext());
        response.put("hasPrevious", payments.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/payments/stats
     * Get payment statistics
     */
    @GetMapping("/payments/stats")
    public ResponseEntity<AdminPaymentStatsResponse> getPaymentStats() {
        AdminPaymentStatsResponse stats = adminService.getPaymentStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/payments/export
     * Export payments to CSV file
     * @param from Start date (optional, ISO format: yyyy-MM-dd)
     * @param to End date (optional, ISO format: yyyy-MM-dd)
     */
    @GetMapping("/payments/export")
    public ResponseEntity<byte[]> exportPaymentsToCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] csvContent = adminCsvExportService.exportPaymentsToCsv(from, to);
        
        String filename = "payments_export_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(csvContent);
    }

    // ==================== ACTIVITY LOGS ====================

    /**
     * GET /api/admin/activity-logs
     * Get all activity logs with pagination
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     */
    @GetMapping("/activity-logs")
    public ResponseEntity<Map<String, Object>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var logs = adminActivityLogService.getAllLogs(pageable);
        
        // Map to DTO to avoid lazy loading issues
        var logResponses = logs.getContent().stream()
                .map(log -> {
                    AdminActivityLogResponse dto = new AdminActivityLogResponse();
                    dto.setId(log.getId());
                    dto.setActionType(log.getActionType());
                    dto.setTargetType(log.getTargetType());
                    dto.setTargetId(log.getTargetId());
                    dto.setDescription(log.getDescription());
                    dto.setIpAddress(log.getIpAddress());
                    dto.setCreatedAt(log.getCreatedAt());
                    
                    if (log.getAdminUser() != null) {
                        dto.setAdminUserId(log.getAdminUser().getId());
                        dto.setAdminUserName(log.getAdminUser().getFullName());
                        dto.setAdminUserEmail(log.getAdminUser().getEmail());
                    }
                    return dto;
                })
                .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("logs", logResponses);
        response.put("currentPage", logs.getNumber());
        response.put("totalItems", logs.getTotalElements());
        response.put("totalPages", logs.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    // ==================== CREDIT MANAGEMENT ====================

    /**
     * POST /api/admin/users/{id}/credits/add
     * Add credits to a user
     */
    @PostMapping("/users/{id}/credits/add")
    public ResponseEntity<Map<String, String>> addCreditsToUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminCreditAdjustRequest request,
            @CurrentUser User adminUser
    ) {
        if (adminUser == null) {
            throw new com.htai.exe201phapluatso.common.exception.BadRequestException("Admin user not found in session");
        }
        adminCreditService.addCredits(id, request.getChatCredits(), request.getQuizGenCredits(), 
                request.getReason(), adminUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã thêm credits thành công");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/users/{id}/credits/remove
     * Remove credits from a user
     */
    @PostMapping("/users/{id}/credits/remove")
    public ResponseEntity<Map<String, String>> removeCreditsFromUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminCreditAdjustRequest request,
            @CurrentUser User adminUser
    ) {
        if (adminUser == null) {
            throw new com.htai.exe201phapluatso.common.exception.BadRequestException("Admin user not found in session");
        }
        adminCreditService.removeCredits(id, request.getChatCredits(), request.getQuizGenCredits(), 
                request.getReason(), adminUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã trừ credits thành công");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/credits/analytics
     * Get credit usage analytics
     * @param from Start date (default: 30 days ago)
     * @param to End date (default: today)
     */
    @GetMapping("/credits/analytics")
    public ResponseEntity<CreditAnalyticsResponse> getCreditAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        
        CreditAnalyticsResponse analytics = adminCreditService.getCreditAnalytics(from, to);
        return ResponseEntity.ok(analytics);
    }
}
