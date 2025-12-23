package com.htai.exe201phapluatso.credit.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.credit.dto.CreditBalanceResponse;
import com.htai.exe201phapluatso.credit.service.CreditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for credit management
 * Provides endpoints for checking credit balance
 */
@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    /**
     * Get current user's credit balance
     * GET /api/credits/balance
     * 
     * @param authentication Spring Security authentication
     * @return Credit balance response
     */
    @GetMapping("/balance")
    public ResponseEntity<CreditBalanceResponse> getCreditBalance(Authentication authentication) {
        // Extract userId directly from AuthUserPrincipal
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();
        
        CreditBalanceResponse balance = creditService.getCreditBalance(userId);
        return ResponseEntity.ok(balance);
    }
}
