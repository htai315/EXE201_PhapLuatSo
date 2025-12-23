package com.htai.exe201phapluatso.ai.controller;

import com.htai.exe201phapluatso.ai.dto.GenerateQuestionsRequest;
import com.htai.exe201phapluatso.ai.dto.GenerateQuestionsResponse;
import com.htai.exe201phapluatso.ai.service.AIQuizService;
import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/quiz")
public class AIQuizController {

    private final AIQuizService aiQuizService;

    public AIQuizController(AIQuizService aiQuizService) {
        this.aiQuizService = aiQuizService;
    }

    @PostMapping("/generate-from-document")
    public ResponseEntity<GenerateQuestionsResponse> generateQuestions(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("quizSetName") String quizSetName,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam(value = "questionCount", defaultValue = "15") int questionCount
    ) {
        // Get email from AuthUserPrincipal
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        String userEmail = principal.email();

        GenerateQuestionsRequest request = new GenerateQuestionsRequest(
                quizSetName,
                description,
                questionCount
        );

        GenerateQuestionsResponse response = aiQuizService.generateQuestionsFromDocument(
                userEmail,
                file,
                request
        );

        return ResponseEntity.ok(response);
    }
}
