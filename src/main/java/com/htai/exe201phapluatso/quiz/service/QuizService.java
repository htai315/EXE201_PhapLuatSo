package com.htai.exe201phapluatso.quiz.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.credit.service.CreditService;
import com.htai.exe201phapluatso.quiz.dto.CreateQuestionRequest;
import com.htai.exe201phapluatso.quiz.dto.CreateQuizSetRequest;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestion;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestionOption;
import com.htai.exe201phapluatso.quiz.entity.QuizSet;
import com.htai.exe201phapluatso.quiz.dto.QuestionResponse;
import com.htai.exe201phapluatso.quiz.repo.QuizAttemptAnswerRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizAttemptRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizQuestionOptionRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizQuestionRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizSetRepo;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service quản lý Quiz Sets và Questions
 * OPTIMIZED: Sử dụng batch queries để tránh N+1 problem
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizSetRepo quizSetRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizQuestionOptionRepo optionRepo;
    private final QuizAttemptAnswerRepo attemptAnswerRepo;
    private final QuizAttemptRepo attemptRepo;
    private final UserRepo userRepo;
    private final EntityManager entityManager;
    private final CreditService creditService;

    public QuizService(
            QuizSetRepo quizSetRepo,
            QuizQuestionRepo questionRepo,
            QuizQuestionOptionRepo optionRepo,
            QuizAttemptAnswerRepo attemptAnswerRepo,
            QuizAttemptRepo attemptRepo,
            UserRepo userRepo,
            EntityManager entityManager,
            CreditService creditService
    ) {
        this.quizSetRepo = quizSetRepo;
        this.questionRepo = questionRepo;
        this.optionRepo = optionRepo;
        this.attemptAnswerRepo = attemptAnswerRepo;
        this.attemptRepo = attemptRepo;
        this.userRepo = userRepo;
        this.entityManager = entityManager;
        this.creditService = creditService;
    }
    
    @Transactional
    public QuizSet createQuizSet(Long userId, CreateQuizSetRequest req) {
        User user = requireActiveStudent(userId);

        QuizSet set = new QuizSet();
        set.setCreatedBy(user);
        // Sanitize input
        set.setTitle(sanitize(req.title()));
        set.setDescription(sanitize(req.description()));
        set.setStatus("DRAFT");
        set.setVisibility("PRIVATE");
        set.setCreatedAt(LocalDateTime.now());

        return quizSetRepo.save(set);
    }

    @Transactional
    public void addQuestion(Long userId, Long quizSetId, CreateQuestionRequest req) {
        QuizSet quizSet = quizSetRepo.findById(quizSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ đề"));

        if (!quizSet.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không phải chủ sở hữu bộ đề này");
        }

        validateOptions(req.options());

        List<QuizQuestion> existingQuestions = questionRepo.findByQuizSetIdOrderBySortOrderAsc(quizSetId);
        int nextSortOrder = existingQuestions.isEmpty() 
                ? 0 
                : existingQuestions.get(existingQuestions.size() - 1).getSortOrder() + 1;

        QuizQuestion question = new QuizQuestion();
        question.setQuizSet(quizSet);
        question.setQuestionText(sanitize(req.questionText()));
        question.setExplanation(sanitize(req.explanation()));
        question.setSortOrder(nextSortOrder);
        question = questionRepo.save(question);

        List<QuizQuestionOption> options = new ArrayList<>();
        for (var opt : req.options()) {
            QuizQuestionOption option = new QuizQuestionOption();
            option.setQuestion(question);
            option.setOptionKey(opt.optionKey().trim().toUpperCase());
            option.setOptionText(sanitize(opt.optionText()));
            option.setCorrect(opt.isCorrect());
            options.add(option);
        }
        optionRepo.saveAll(options);

        quizSet.setUpdatedAt(LocalDateTime.now());
        quizSetRepo.save(quizSet);
    }

    @Transactional(readOnly = true)
    public List<QuizSet> getQuizSetsForUser(Long userId) {
        requireActiveStudent(userId);
        return quizSetRepo.findByCreatedById(userId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<QuizSet> getQuizSetsForUserPaginated(
            Long userId, 
            int page, 
            int size
    ) {
        requireActiveStudent(userId);
        
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(
                        page, 
                        size, 
                        org.springframework.data.domain.Sort.by("updatedAt").descending()
                );
        
        return quizSetRepo.findByCreatedById(userId, pageable);
    }

    /**
     * Batch count questions cho nhiều quiz sets - OPTIMIZED
     * Tránh N+1 query khi hiển thị danh sách quiz sets
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> countQuestionsForQuizSets(List<Long> quizSetIds) {
        if (quizSetIds == null || quizSetIds.isEmpty()) {
            return Map.of();
        }
        
        return questionRepo.countByQuizSetIds(quizSetIds).stream()
                .collect(Collectors.toMap(
                        row -> toLong(row[0]),
                        row -> toLong(row[1])
                ));
    }

    @Transactional(readOnly = true)
    public QuizSet getOwnedQuizSet(Long userId, Long quizSetId) {
        QuizSet quizSet = quizSetRepo.findById(quizSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ đề"));

        if (!quizSet.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không phải chủ sở hữu bộ đề này");
        }

        return quizSet;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsForSet(Long userId, Long quizSetId) {
        getOwnedQuizSet(userId, quizSetId);

        // FIX N+1: Sử dụng JOIN FETCH để load questions + options trong 1 query
        List<QuizQuestion> questions = questionRepo.findByQuizSetIdWithOptions(quizSetId);
        
        return questions.stream()
                .map(q -> QuestionResponse.from(q, q.getOptions()))
                .toList();
    }

    /**
     * Get questions with options for PDF export
     */
    @Transactional(readOnly = true)
    public List<QuizQuestion> getQuestionsWithOptionsForExport(Long userId, Long quizSetId) {
        getOwnedQuizSet(userId, quizSetId);
        return questionRepo.findByQuizSetIdWithOptions(quizSetId);
    }

    @Transactional
    public void deleteQuizSet(Long userId, Long quizSetId) {
        log.info("Đang xóa bộ đề {} bởi user {}", quizSetId, userId);
        
        QuizSet quizSet = getOwnedQuizSet(userId, quizSetId);
        log.info("Tìm thấy bộ đề: {}", quizSet.getTitle());
        
        try {
            // SQL Server không cho phép multiple cascade paths
            // Giải pháp: Xóa attempt_answers trước khi cascade xóa questions
            List<Long> questionIds = questionRepo.findByQuizSetIdOrderBySortOrderAsc(quizSetId)
                    .stream()
                    .map(QuizQuestion::getId)
                    .toList();
            
            log.info("Tìm thấy {} câu hỏi trong bộ đề", questionIds.size());
            
            if (!questionIds.isEmpty()) {
                log.info("Đang xóa câu trả lời cho các câu hỏi: {}", questionIds);
                attemptAnswerRepo.deleteByQuestionIds(questionIds);
                entityManager.flush();
                log.info("Đã xóa câu trả lời thành công");
            }
            
            entityManager.clear();
            
            log.info("Đang xóa bộ đề {}", quizSetId);
            quizSetRepo.deleteById(quizSetId);
            entityManager.flush();
            log.info("Đã xóa bộ đề thành công");
            
        } catch (Exception e) {
            log.error("Lỗi khi xóa bộ đề {}: {}", quizSetId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void deleteQuestion(Long userId, Long quizSetId, Long questionId) {
        QuizSet quizSet = getOwnedQuizSet(userId, quizSetId);

        QuizQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));

        if (!question.getQuizSet().getId().equals(quizSet.getId())) {
            throw new BadRequestException("Câu hỏi không thuộc bộ đề này");
        }

        attemptAnswerRepo.deleteByQuestionId(questionId);
        questionRepo.delete(question);
    }

    @Transactional
    public void updateQuestion(Long userId, Long quizSetId, Long questionId, CreateQuestionRequest req) {
        QuizSet quizSet = getOwnedQuizSet(userId, quizSetId);

        QuizQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));

        if (!question.getQuizSet().getId().equals(quizSet.getId())) {
            throw new BadRequestException("Câu hỏi không thuộc bộ đề này");
        }

        validateOptions(req.options());

        question.setQuestionText(sanitize(req.questionText()));
        question.setExplanation(sanitize(req.explanation()));
        question.setUpdatedAt(LocalDateTime.now());
        questionRepo.save(question);

        optionRepo.deleteByQuestionId(questionId);

        List<QuizQuestionOption> newOptions = new ArrayList<>();
        for (var opt : req.options()) {
            QuizQuestionOption option = new QuizQuestionOption();
            option.setQuestion(question);
            option.setOptionKey(opt.optionKey().trim().toUpperCase());
            option.setOptionText(sanitize(opt.optionText()));
            option.setCorrect(opt.isCorrect());
            newOptions.add(option);
        }
        optionRepo.saveAll(newOptions);
    }

    @Transactional(readOnly = true)
    public long countQuestionsInSet(Long userId, Long quizSetId) {
        getOwnedQuizSet(userId, quizSetId);
        return questionRepo.countByQuizSetId(quizSetId);
    }

    // ==================== HELPERS ====================
    
    private User requireActiveStudent(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    private void validateOptions(List<CreateQuestionRequest.OptionDto> options) {
        if (options == null || options.size() != 4) {
            throw new BadRequestException("Phải có đúng 4 đáp án");
        }

        Set<String> keys = options.stream()
                .map(o -> o.optionKey().trim().toUpperCase())
                .collect(Collectors.toSet());

        if (!keys.equals(Set.of("A", "B", "C", "D"))) {
            throw new BadRequestException("Đáp án phải có các key: A, B, C, D");
        }

        long correctCount = options.stream()
                .filter(CreateQuestionRequest.OptionDto::isCorrect)
                .count();

        if (correctCount != 1) {
            throw new BadRequestException("Phải có đúng 1 đáp án đúng");
        }
    }

    /**
     * Sanitize input: trim và xử lý null
     */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.trim();
    }

    /**
     * Convert Object to Long (handles BigDecimal, Integer, etc.)
     */
    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
