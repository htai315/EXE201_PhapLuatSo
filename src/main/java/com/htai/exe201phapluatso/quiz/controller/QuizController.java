package com.htai.exe201phapluatso.quiz.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.common.exception.UnauthorizedException;
import com.htai.exe201phapluatso.quiz.dto.CreateQuestionRequest;
import com.htai.exe201phapluatso.quiz.dto.CreateQuizSetRequest;
import com.htai.exe201phapluatso.quiz.dto.QuestionResponse;
import com.htai.exe201phapluatso.quiz.dto.QuizSetResponse;
import com.htai.exe201phapluatso.quiz.dto.PagedQuizSetsResponse;
import com.htai.exe201phapluatso.quiz.dto.PagedExamHistoryResponse;
import com.htai.exe201phapluatso.quiz.dto.ExamDtos.*;
import com.htai.exe201phapluatso.quiz.entity.QuizSet;
import com.htai.exe201phapluatso.quiz.service.QuizExamService;
import com.htai.exe201phapluatso.quiz.service.QuizService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho Quiz Management
 * OPTIMIZED: Sử dụng batch queries để tránh N+1 problem
 */
@RestController
@RequestMapping("/api/quiz-sets")
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);
    
    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 50;

    private final QuizService quizService;
    private final QuizExamService quizExamService;

    public QuizController(QuizService quizService, QuizExamService quizExamService) {
        this.quizService = quizService;
        this.quizExamService = quizExamService;
    }

    @PostMapping
    public ResponseEntity<QuizSetResponse> createQuizSet(
            Authentication auth,
            @Valid @RequestBody CreateQuizSetRequest req
    ) {
        Long userId = getUserId(auth);
        var quizSet = quizService.createQuizSet(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(QuizSetResponse.from(quizSet));
    }

    /**
     * Lấy danh sách quiz sets của user - OPTIMIZED với batch query
     */
    @GetMapping("/my")
    public ResponseEntity<List<QuizSetResponse>> getMyQuizSets(Authentication auth) {
        Long userId = getUserId(auth);
        var quizSets = quizService.getQuizSetsForUser(userId);
        
        // FIX N+1: Batch query để lấy question counts
        List<Long> quizSetIds = quizSets.stream().map(QuizSet::getId).toList();
        Map<Long, Long> questionCounts = quizService.countQuestionsForQuizSets(quizSetIds);
        
        var responses = quizSets.stream()
                .map(set -> mapToQuizSetResponse(set, questionCounts.getOrDefault(set.getId(), 0L)))
                .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Lấy danh sách quiz sets có phân trang - OPTIMIZED với batch query
     */
    @GetMapping("/my/paginated")
    public ResponseEntity<PagedQuizSetsResponse> getMyQuizSetsPaginated(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Long userId = getUserId(auth);
        
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1 || size > MAX_PAGE_SIZE) {
            log.warn("Invalid page size requested: {}, using default: {}", size, DEFAULT_PAGE_SIZE);
            size = DEFAULT_PAGE_SIZE;
        }
        
        var pagedQuizSets = quizService.getQuizSetsForUserPaginated(userId, page, size);
        
        // FIX N+1: Batch query để lấy question counts
        List<Long> quizSetIds = pagedQuizSets.getContent().stream()
                .map(QuizSet::getId)
                .toList();
        Map<Long, Long> questionCounts = quizService.countQuestionsForQuizSets(quizSetIds);
        
        var content = pagedQuizSets.getContent().stream()
                .map(set -> mapToQuizSetResponse(set, questionCounts.getOrDefault(set.getId(), 0L)))
                .toList();
        
        var response = PagedQuizSetsResponse.from(
                content,
                pagedQuizSets.getNumber(),
                pagedQuizSets.getSize(),
                pagedQuizSets.getTotalElements(),
                pagedQuizSets.getTotalPages()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method để map QuizSet entity sang response DTO
     */
    private QuizSetResponse mapToQuizSetResponse(QuizSet set, long questionCount) {
        return new QuizSetResponse(
                set.getId(),
                set.getTitle(),
                set.getDescription(),
                set.getStatus(),
                set.getVisibility(),
                set.getCreatedBy().getId(),
                set.getCreatedAt(),
                set.getUpdatedAt(),
                questionCount
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizSetResponse> getQuizSet(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = getUserId(auth);
        var quizSet = quizService.getOwnedQuizSet(userId, id);
        return ResponseEntity.ok(QuizSetResponse.from(quizSet));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<Void> addQuestion(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody CreateQuestionRequest req
    ) {
        Long userId = getUserId(auth);
        quizService.addQuestion(userId, id, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = getUserId(auth);
        var questions = quizService.getQuestionsForSet(userId, id);
        return ResponseEntity.ok(questions);
    }

    // -------- Exam (practice) APIs --------

    @GetMapping("/{id}/exam")
    public ResponseEntity<StartExamResponse> startExam(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = getUserId(auth);
        StartExamResponse res = quizExamService.startExam(userId, id);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/exam/submit")
    public ResponseEntity<SubmitExamResponse> submitExam(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody SubmitExamRequest req
    ) {
        Long userId = getUserId(auth);
        SubmitExamResponse res = quizExamService.submitExam(userId, id, req);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/exam/history")
    public ResponseEntity<ExamHistoryResponse> history(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = getUserId(auth);
        ExamHistoryResponse res = quizExamService.getHistory(userId, id);
        return ResponseEntity.ok(res);
    }

    /**
     * Lấy lịch sử làm bài của user có phân trang (tất cả quiz sets)
     */
    @GetMapping("/exam/history")
    public ResponseEntity<PagedExamHistoryResponse> allHistory(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getUserId(auth);
        
        // Validate pagination
        if (page < 0) page = 0;
        if (size < 1 || size > MAX_PAGE_SIZE) size = 10;
        
        PagedExamHistoryResponse res = quizExamService.getAllHistory(userId, page, size);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuizSet(
            Authentication auth,
            @PathVariable Long id
    ) {
        Long userId = getUserId(auth);
        quizService.deleteQuizSet(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{quizSetId}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            Authentication auth,
            @PathVariable Long quizSetId,
            @PathVariable Long questionId
    ) {
        Long userId = getUserId(auth);
        quizService.deleteQuestion(userId, quizSetId, questionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{quizSetId}/questions/{questionId}")
    public ResponseEntity<Void> updateQuestion(
            Authentication auth,
            @PathVariable Long quizSetId,
            @PathVariable Long questionId,
            @Valid @RequestBody CreateQuestionRequest req
    ) {
        Long userId = getUserId(auth);
        quizService.updateQuestion(userId, quizSetId, questionId, req);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        AuthUserPrincipal principal = (AuthUserPrincipal) auth.getPrincipal();
        return principal.userId();
    }
}
