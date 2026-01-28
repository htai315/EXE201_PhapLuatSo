package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.legal.dto.LegalDocumentDTO;
import com.htai.exe201phapluatso.legal.dto.UploadLegalDocumentRequest;
import com.htai.exe201phapluatso.legal.dto.UploadLegalDocumentResponse;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import com.htai.exe201phapluatso.legal.entity.LegalDocument;
import com.htai.exe201phapluatso.legal.repo.ChatMessageRepo;
import com.htai.exe201phapluatso.legal.repo.LegalArticleRepo;
import com.htai.exe201phapluatso.legal.repo.LegalDocumentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LegalDocumentService {

    private static final Logger log = LoggerFactory.getLogger(LegalDocumentService.class);

    private final LegalDocumentRepo documentRepo;
    private final LegalArticleRepo articleRepo;
    private final LegalDocumentParserService parserService;
    private final UserRepo userRepo;
    private final VectorSearchService vectorSearchService;
    private final ChatMessageRepo chatMessageRepo;
    private final com.htai.exe201phapluatso.common.service.CloudinaryService cloudinaryService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB for legal documents
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf"
    );

    public LegalDocumentService(
            LegalDocumentRepo documentRepo,
            LegalArticleRepo articleRepo,
            LegalDocumentParserService parserService,
            UserRepo userRepo,
            VectorSearchService vectorSearchService,
            ChatMessageRepo chatMessageRepo,
            com.htai.exe201phapluatso.common.service.CloudinaryService cloudinaryService
    ) {
        this.documentRepo = documentRepo;
        this.articleRepo = articleRepo;
        this.parserService = parserService;
        this.userRepo = userRepo;
        this.vectorSearchService = vectorSearchService;
        this.chatMessageRepo = chatMessageRepo;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public UploadLegalDocumentResponse uploadDocument(
            String userEmail,
            MultipartFile file,
            UploadLegalDocumentRequest request
    ) {
        // 1. Validate file
        validateFile(file);
        
        // 2. Get user
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // 3. Save file to Cloudinary
        String filePath = saveFile(file);

        // 4. Parse PDF and extract articles
        // Note: Parser service might need input stream, which is fine from MultipartFile
        List<LegalArticle> articles = parserService.parseDocument(file);

        // 5. Create document entity
        LegalDocument document = new LegalDocument();
        document.setDocumentName(sanitizeInput(request.documentName()));
        document.setDocumentCode(sanitizeInput(request.documentCode()));
        document.setDocumentType(sanitizeInput(request.documentType()));
        document.setIssuingBody(sanitizeInput(request.issuingBody()));
        
        if (request.effectiveDate() != null && !request.effectiveDate().isEmpty()) {
            document.setEffectiveDate(LocalDate.parse(request.effectiveDate()));
        }
        
        document.setFilePath(filePath);
        document.setTotalArticles(articles.size());
        document.setCreatedBy(user);
        document.setCreatedAt(LocalDateTime.now());

        // 6. Link articles to document
        for (LegalArticle article : articles) {
            article.setDocument(document);
        }
        document.setArticles(articles);

        // 7. Save to database
        document = documentRepo.save(document);
        
        log.info("Document uploaded: {} with {} articles by user {}", 
                document.getDocumentName(), document.getTotalArticles(), userEmail);

        // 8. Generate embeddings for new articles (async-like, in background)
        generateEmbeddingsForDocument(document);

        // 9. Return response
        return new UploadLegalDocumentResponse(
                document.getId(),
                document.getDocumentName(),
                document.getTotalArticles(),
                "Đã import thành công " + document.getTotalArticles() + " điều luật"
        );
    }

    /**
     * Generate embeddings for all articles in a document
     * This runs after document is saved to generate vector embeddings for semantic search
     */
    private void generateEmbeddingsForDocument(LegalDocument document) {
        try {
            log.info("Generating embeddings for {} articles in document {}", 
                document.getTotalArticles(), document.getId());
            
            for (LegalArticle article : document.getArticles()) {
                try {
                    vectorSearchService.generateAndSaveEmbedding(article.getId());
                } catch (Exception e) {
                    log.warn("Failed to generate embedding for article {}: {}", 
                        article.getId(), e.getMessage());
                    // Continue with other articles even if one fails
                }
            }
            
            log.info("Finished generating embeddings for document {}", document.getId());
        } catch (Exception e) {
            log.error("Error generating embeddings for document {}: {}", 
                document.getId(), e.getMessage());
            // Don't fail the upload if embedding generation fails
        }
    }
    
    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File không được vượt quá 50MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Chỉ hỗ trợ file PDF");
        }
        
        // Validate filename to prevent path traversal
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            throw new BadRequestException("Tên file không hợp lệ");
        }
    }
    
    /**
     * Sanitize input to prevent XSS
     */
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .trim();
    }

    /**
     * Save uploaded file to Cloudinary
     */
    private String saveFile(MultipartFile file) {
        try {
            String url = cloudinaryService.uploadFile(file, "legal_docs");
            log.info("File saved to Cloudinary: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Error saving file to Cloudinary", e);
            throw new BadRequestException("Không thể lưu file. Vui lòng thử lại.");
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
     * Get paginated documents with search
     */
    public Page<LegalDocumentDTO> getDocumentsPaginated(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<LegalDocument> documentsPage;
        if (search != null && !search.trim().isEmpty()) {
            documentsPage = documentRepo.findByDocumentNameContainingIgnoreCaseOrDocumentCodeContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        } else {
            documentsPage = documentRepo.findAll(pageable);
        }
        
        return documentsPage.map(this::toDTO);
    }

    /**
     * Get documents statistics
     */
    public Map<String, Object> getDocumentsStats() {
        long totalDocuments = documentRepo.count();
        long totalArticles = articleRepo.count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", totalDocuments);
        stats.put("totalArticles", totalArticles);
        
        return stats;
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
        
        // Delete file from Cloudinary
        cloudinaryService.deleteFile(document.getFilePath());
        
        // Get article IDs to delete citations first
        List<Long> articleIds = document.getArticles().stream()
                .map(LegalArticle::getId)
                .collect(Collectors.toList());
        
        // Delete citations from chat_message_citations table first (to avoid FK constraint)
        if (!articleIds.isEmpty()) {
            chatMessageRepo.deleteCitationsByArticleIds(articleIds);
            log.info("Deleted citations for {} articles", articleIds.size());
        }
        
        // Delete articles
        articleRepo.deleteAll(document.getArticles());
        document.getArticles().clear();
        
        // Delete from database
        documentRepo.delete(document);
        log.info("Document deleted: {}", id);
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
