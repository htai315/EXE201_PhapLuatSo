package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.legal.dto.LegalDocumentDTO;
import com.htai.exe201phapluatso.legal.dto.UploadLegalDocumentRequest;
import com.htai.exe201phapluatso.legal.dto.UploadLegalDocumentResponse;
import com.htai.exe201phapluatso.legal.service.LegalDocumentService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/legal/documents")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    public LegalDocumentController(LegalDocumentService legalDocumentService) {
        this.legalDocumentService = legalDocumentService;
    }

    /**
     * Upload legal document (Admin only)
     * POST /api/legal/documents/upload
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UploadLegalDocumentResponse> uploadDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentName") String documentName,
            @RequestParam(value = "documentCode", required = false) String documentCode,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "issuingBody", required = false) String issuingBody,
            @RequestParam(value = "effectiveDate", required = false) String effectiveDate
    ) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        String userEmail = principal.email();

        UploadLegalDocumentRequest request = new UploadLegalDocumentRequest(
                documentName,
                documentCode,
                documentType,
                issuingBody,
                effectiveDate
        );

        UploadLegalDocumentResponse response = legalDocumentService.uploadDocument(
                userEmail,
                file,
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all legal documents (Admin only)
     * GET /api/legal/documents
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LegalDocumentDTO>> getAllDocuments() {
        List<LegalDocumentDTO> documents = legalDocumentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    /**
     * Get paginated legal documents with search (Admin only)
     * GET /api/legal/documents/paginated?page=0&size=10&search=luật
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDocumentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        Page<LegalDocumentDTO> documentsPage = legalDocumentService.getDocumentsPaginated(page, size, search);
        
        Map<String, Object> response = Map.of(
                "documents", documentsPage.getContent(),
                "currentPage", documentsPage.getNumber(),
                "totalPages", documentsPage.getTotalPages(),
                "totalElements", documentsPage.getTotalElements(),
                "hasNext", documentsPage.hasNext(),
                "hasPrevious", documentsPage.hasPrevious()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get documents statistics (Admin only)
     * GET /api/legal/documents/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDocumentsStats() {
        Map<String, Object> stats = legalDocumentService.getDocumentsStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Delete legal document (Admin only)
     * DELETE /api/legal/documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        legalDocumentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
