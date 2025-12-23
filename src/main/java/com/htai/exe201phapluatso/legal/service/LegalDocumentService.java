package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.legal.dto.LegalDocumentDTO;
import com.htai.exe201phapluatso.legal.dto.UploadLegalDocumentRequest;
import com.htai.exe201phapluatso.legal.dto.UploadLegalDocumentResponse;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.entity.LegalDocument;
import com.htai.exe201phapluatso.legal.repo.LegalArticleRepo;
import com.htai.exe201phapluatso.legal.repo.LegalDocumentRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LegalDocumentService {

    private final LegalDocumentRepo documentRepo;
    private final LegalArticleRepo articleRepo;
    private final LegalDocumentParserService parserService;
    private final UserRepo userRepo;

    private static final String UPLOAD_DIR = "uploads/legal/";

    public LegalDocumentService(
            LegalDocumentRepo documentRepo,
            LegalArticleRepo articleRepo,
            LegalDocumentParserService parserService,
            UserRepo userRepo
    ) {
        this.documentRepo = documentRepo;
        this.articleRepo = articleRepo;
        this.parserService = parserService;
        this.userRepo = userRepo;
    }

    @Transactional
    public UploadLegalDocumentResponse uploadDocument(
            String userEmail,
            MultipartFile file,
            UploadLegalDocumentRequest request
    ) {
        // 1. Get user
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 2. Save file to disk
        String filePath = saveFile(file);

        // 3. Parse PDF and extract articles
        List<LegalArticle> articles = parserService.parseDocument(file);

        // 4. Create document entity
        LegalDocument document = new LegalDocument();
        document.setDocumentName(request.documentName());
        document.setDocumentCode(request.documentCode());
        document.setDocumentType(request.documentType());
        document.setIssuingBody(request.issuingBody());
        
        if (request.effectiveDate() != null && !request.effectiveDate().isEmpty()) {
            document.setEffectiveDate(LocalDate.parse(request.effectiveDate()));
        }
        
        document.setFilePath(filePath);
        document.setTotalArticles(articles.size());
        document.setCreatedBy(user);
        document.setCreatedAt(LocalDateTime.now());

        // 5. Link articles to document
        for (LegalArticle article : articles) {
            article.setDocument(document);
        }
        document.setArticles(articles);

        // 6. Save to database
        document = documentRepo.save(document);

        // 7. Return response
        return new UploadLegalDocumentResponse(
                document.getId(),
                document.getDocumentName(),
                document.getTotalArticles(),
                "Đã import thành công " + document.getTotalArticles() + " điều luật"
        );
    }

    /**
     * Save uploaded file to disk
     */
    private String saveFile(MultipartFile file) {
        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".pdf";
            String filename = UUID.randomUUID() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return UPLOAD_DIR + filename;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file: " + e.getMessage());
        }
    }

    /**
     * Get all documents
     */
    public List<LegalDocumentDTO> getAllDocuments() {
        return documentRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get document by ID
     */
    public LegalDocument getDocumentById(Long id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy văn bản pháp luật"));
    }

    /**
     * Delete document
     */
    @Transactional
    public void deleteDocument(Long id) {
        LegalDocument document = getDocumentById(id);
        
        // Delete file from disk
        try {
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't fail the operation
            System.err.println("Could not delete file: " + e.getMessage());
        }
        
        // Delete from database (cascade will delete articles)
        documentRepo.delete(document);
    }

    /**
     * Convert entity to DTO
     */
    private LegalDocumentDTO toDTO(LegalDocument document) {
        return new LegalDocumentDTO(
                document.getId(),
                document.getDocumentName(),
                document.getDocumentCode(),
                document.getDocumentType(),
                document.getIssuingBody(),
                document.getEffectiveDate(),
                document.getTotalArticles(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }
}
