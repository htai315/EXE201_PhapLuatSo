package com.htai.exe201phapluatso.quiz.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.quiz.dto.PagedExamHistoryResponse;
import com.htai.exe201phapluatso.quiz.dto.ExamDtos.*;
import com.htai.exe201phapluatso.quiz.dto.ExamSessionData;
import com.htai.exe201phapluatso.quiz.entity.*;
import com.htai.exe201phapluatso.quiz.repo.*;
import com.htai.exe201phapluatso.quiz.session.ExamSessionStoreManager;
import com.htai.exe201phapluatso.quiz.validation.QuizDurationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý logic làm bài quiz (exam/practice)
 * OPTIMIZED: Sử dụng JOIN FETCH để tránh N+1 query problem
 * ENHANCED: Random câu hỏi và shuffle đáp án
 * SECURED: Lưu mapping đáp án đúng server-side để tránh gian lận
 * DISTRIBUTED: Sử dụng Redis session storage với in-memory fallback
 */
@Service
public class QuizExamService {

    private static final Logger log = LoggerFactory.getLogger(QuizExamService.class);

    private static final int MAX_HISTORY_ITEMS = 10;
    private static final List<String> OPTION_KEYS = List.of("A", "B", "C", "D");

    private final QuizSetRepo quizSetRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizAttemptRepo attemptRepo;
    private final QuizAttemptAnswerRepo answerRepo;
    private final UserRepo userRepo;
    private final ExamSessionStoreManager sessionStoreManager;

    public QuizExamService(
            QuizSetRepo quizSetRepo,
            QuizQuestionRepo questionRepo,
            QuizAttemptRepo attemptRepo,
            QuizAttemptAnswerRepo answerRepo,
            UserRepo userRepo,
            ExamSessionStoreManager sessionStoreManager
    ) {
        this.quizSetRepo = quizSetRepo;
        this.questionRepo = questionRepo;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.sessionStoreManager = sessionStoreManager;
    }
    
    /**
     * Scheduled task để cleanup expired sessions - chạy mỗi 10 phút
     * Chỉ cleanup in-memory fallback store (Redis tự động cleanup qua TTL)
     */
    @Scheduled(fixedRate = 600000) // 10 phút
    public void cleanupExpiredExamSessions() {
        int removed = sessionStoreManager.cleanupExpiredInMemorySessions();
        if (removed > 0) {
            log.info("Cleaned up {} expired exam sessions from in-memory store", removed);
        }
    }

    /**
     * Bắt đầu làm bài quiz - OPTIMIZED & ENHANCED & SECURED
     * - Random thứ tự câu hỏi
     * - Shuffle đáp án A, B, C, D
     * - Lưu mapping đáp án đúng server-side (Redis hoặc in-memory)
     * - Track startedAt để tính thời gian làm bài chính xác
     */
    @Transactional(readOnly = true)
    public StartExamResponse startExam(Long userId, Long quizSetId) {
        QuizSet quizSet = requireCanPractice(userId, quizSetId);

        // FIX N+1: Sử dụng JOIN FETCH để load questions + options trong 1 query
        List<QuizQuestion> questions = questionRepo.findByQuizSetIdWithOptions(quizSetId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Bộ đề hiện chưa có câu hỏi nào");
        }

        // ENHANCED: Random thứ tự câu hỏi
        List<QuizQuestion> shuffledQuestions = new ArrayList<>(questions);
        Collections.shuffle(shuffledQuestions);

        // SECURED: Tạo mapping đáp án đúng và options đã shuffle
        Map<Long, String> correctKeyMapping = new HashMap<>();
        Map<Long, List<ExamOptionDto>> shuffledOptionsMapping = new HashMap<>();
        
        // ENHANCED: Shuffle đáp án cho mỗi câu hỏi
        List<ExamQuestionDto> questionDtos = shuffledQuestions.stream()
                .map(q -> createShuffledQuestionDto(q, correctKeyMapping, shuffledOptionsMapping))
                .toList();
        
        // Lưu session với mapping đáp án đúng, options đã shuffle và startedAt
        String sessionKey = ExamSessionStoreManager.buildSessionKey(userId, quizSetId);
        ExamSessionData sessionData = new ExamSessionData(correctKeyMapping, shuffledOptionsMapping);
        sessionStoreManager.save(sessionKey, sessionData);
        
        log.debug("Started exam session for user {} on quiz {}. Redis available: {}", 
                userId, quizSetId, sessionStoreManager.isRedisAvailable());

        // Lấy duration từ quiz set (default 45 phút)
        int durationMinutes = quizSet.getDurationMinutes() != null 
                ? quizSet.getDurationMinutes() 
                : QuizDurationValidator.DEFAULT_DURATION_MINUTES;

        return new StartExamResponse(
                quizSet.getId(),
                quizSet.getTitle(),
                questionDtos.size(),
                durationMinutes,
                questionDtos
        );
    }

    /**
     * Tạo DTO câu hỏi với đáp án đã được shuffle
     * Gán lại key A, B, C, D theo thứ tự mới
     * SECURED: Không gửi correctOptionKey về frontend
     */
    private ExamQuestionDto createShuffledQuestionDto(
            QuizQuestion question, 
            Map<Long, String> correctKeyMapping,
            Map<Long, List<ExamOptionDto>> shuffledOptionsMapping
    ) {
        List<QuizQuestionOption> originalOptions = new ArrayList<>(question.getOptions());
        
        // Shuffle đáp án
        Collections.shuffle(originalOptions);
        
        // Gán lại key A, B, C, D theo thứ tự mới
        List<ExamOptionDto> shuffledOptions = new ArrayList<>();
        String newCorrectKey = null;
        
        for (int i = 0; i < originalOptions.size() && i < OPTION_KEYS.size(); i++) {
            QuizQuestionOption opt = originalOptions.get(i);
            String newKey = OPTION_KEYS.get(i);
            
            shuffledOptions.add(new ExamOptionDto(newKey, opt.getOptionText()));
            
            // Track đáp án đúng với key mới
            if (opt.isCorrect()) {
                newCorrectKey = newKey;
            }
        }
        
        // Lưu mapping đáp án đúng và options đã shuffle server-side
        if (newCorrectKey != null) {
            correctKeyMapping.put(question.getId(), newCorrectKey);
        }
        shuffledOptionsMapping.put(question.getId(), shuffledOptions);
        
        // SECURED: Không gửi correctOptionKey về frontend (null)
        return new ExamQuestionDto(
                question.getId(),
                question.getQuestionText(),
                question.getExplanation(),
                shuffledOptions,
                null  // Ẩn đáp án đúng - sẽ validate server-side
        );
    }

    /**
     * Nộp bài quiz - OPTIMIZED & SECURED
     * Validate đáp án từ server-side mapping thay vì tin tưởng frontend
     * Track startedAt từ session để tính thời gian làm bài chính xác
     */
    @Transactional
    public SubmitExamResponse submitExam(Long userId, Long quizSetId, SubmitExamRequest req) {
        QuizSet quizSet = requireCanPractice(userId, quizSetId);

        if (req == null || req.answers() == null || req.answers().isEmpty()) {
            throw new BadRequestException("Danh sách câu trả lời không hợp lệ");
        }

        // FIX N+1: Load questions với options trong 1 query
        List<QuizQuestion> questions = questionRepo.findByQuizSetIdWithOptions(quizSetId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Bộ đề hiện chưa có câu hỏi nào");
        }

        // SECURED: Lấy mapping đáp án đúng từ session store
        String sessionKey = ExamSessionStoreManager.buildSessionKey(userId, quizSetId);
        Optional<ExamSessionData> sessionOpt = sessionStoreManager.get(sessionKey);
        
        // FIX: Reject submit khi session không tồn tại hoặc expired
        if (sessionOpt.isEmpty()) {
            throw new BadRequestException("Phiên thi đã hết hạn. Vui lòng bắt đầu lại bài thi.");
        }
        
        ExamSessionData session = sessionOpt.get();
        Map<Long, String> correctKeyMapping = session.correctKeyMapping();
        Map<Long, List<ExamOptionDto>> shuffledOptionsMapping = session.shuffledOptionsMapping();
        LocalDateTime startedAt = session.startedAt();

        // Build map question by id
        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        int totalQuestions = questions.size();
        int correctCount = 0;
        List<WrongQuestionDto> wrongs = new ArrayList<>();

        // Map answer by question id for quick lookup
        Map<Long, SubmitExamRequest.AnswerDto> answersByQid = req.answers().stream()
                .collect(Collectors.toMap(SubmitExamRequest.AnswerDto::questionId, a -> a, (a, b) -> a));

        // Evaluate each question
        for (QuizQuestion question : questions) {
            SubmitExamRequest.AnswerDto ans = answersByQid.get(question.getId());
            String selectedKey = ans != null ? normalizeKey(ans.selectedOptionKey()) : null;
            
            // SECURED: Lấy correctKey từ server-side mapping
            String correctKey = correctKeyMapping.get(question.getId());

            List<QuizQuestionOption> opts = question.getOptions();
            if (opts == null || opts.isEmpty() || correctKey == null) {
                continue;
            }

            // So sánh với correctKey từ server-side mapping
            boolean isCorrect = selectedKey != null && correctKey.equalsIgnoreCase(selectedKey);
            
            if (isCorrect) {
                correctCount++;
            } else {
                // Lấy options đã shuffle từ session để hiển thị đúng thứ tự
                List<ExamOptionDto> optionDtos = shuffledOptionsMapping.getOrDefault(
                        question.getId(), 
                        buildOriginalOptions(opts)
                );
                
                wrongs.add(new WrongQuestionDto(
                        question.getId(),
                        question.getQuestionText(),
                        correctKey,
                        selectedKey,
                        question.getExplanation(),
                        optionDtos
                ));
            }
        }

        int scorePercent = (int) Math.round((correctCount * 100.0) / totalQuestions);
        double scoreOutOf10 = Math.round((correctCount * 100.0) / totalQuestions) / 10.0;

        // Save attempt + answers với startedAt chính xác từ session
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuizSet(quizSet);
        attempt.setStartedAt(startedAt); // FIX: Sử dụng startedAt từ session
        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCorrectCount(correctCount);
        attempt.setScorePercent(scorePercent);
        attempt = attemptRepo.save(attempt);

        List<QuizAttemptAnswer> answers = new ArrayList<>();
        for (QuizQuestion question : questions) {
            SubmitExamRequest.AnswerDto ans = answersByQid.get(question.getId());
            String selectedKey = ans != null ? normalizeKey(ans.selectedOptionKey()) : null;
            String correctKey = correctKeyMapping.get(question.getId());

            if (selectedKey == null) {
                continue;
            }

            boolean isCorrect = correctKey != null && correctKey.equalsIgnoreCase(selectedKey);

            QuizAttemptAnswer aa = new QuizAttemptAnswer();
            aa.setAttempt(attempt);
            aa.setQuestion(question);
            aa.setSelectedOptionKey(selectedKey);
            aa.setCorrect(isCorrect);
            answers.add(aa);
        }
        if (!answers.isEmpty()) {
            answerRepo.saveAll(answers);
        }
        
        // Xóa session sau khi submit thành công
        sessionStoreManager.delete(sessionKey);
        log.debug("Submitted exam for user {} on quiz {}. Score: {}/{}",
                userId, quizSetId, correctCount, totalQuestions);

        return new SubmitExamResponse(
                attempt.getId(),
                totalQuestions,
                correctCount,
                scorePercent,
                scoreOutOf10,
                wrongs
        );
    }
    
    /**
     * Build options gốc từ DB
     */
    private List<ExamOptionDto> buildOriginalOptions(List<QuizQuestionOption> opts) {
        return opts.stream()
                .map(o -> new ExamOptionDto(o.getOptionKey(), o.getOptionText()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamHistoryResponse getHistory(Long userId, Long quizSetId) {
        QuizSet quizSet = quizSetRepo.findById(quizSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ đề"));

        if (!quizSet.getCreatedBy().getId().equals(userId)
                && !"PUBLIC".equalsIgnoreCase(quizSet.getVisibility())) {
            throw new ForbiddenException("Bạn không có quyền xem lịch sử bộ đề này");
        }

        List<QuizAttempt> attempts = attemptRepo
                .findTop10ByUserIdAndQuizSetIdOrderByFinishedAtDesc(userId, quizSetId);

        List<ExamHistoryItemDto> items = attempts.stream()
                .map(a -> {
                    double scoreOutOf10 = Math.round((a.getCorrectCount() * 100.0) / a.getTotalQuestions()) / 10.0;
                    return new ExamHistoryItemDto(
                            a.getId(),
                            a.getFinishedAt(),
                            a.getTotalQuestions(),
                            a.getCorrectCount(),
                            a.getScorePercent(),
                            scoreOutOf10
                    );
                })
                .toList();

        return new ExamHistoryResponse(
                quizSet.getId(),
                quizSet.getTitle(),
                items
        );
    }

    /**
     * Lấy lịch sử làm bài của user có phân trang - OPTIMIZED: 1 query với JOIN FETCH
     */
    @Transactional(readOnly = true)
    public PagedExamHistoryResponse getAllHistory(Long userId, int page, int size) {
        Page<QuizAttempt> pagedAttempts = attemptRepo.findByUserIdWithQuizSet(
                userId, 
                PageRequest.of(page, size)
        );

        List<PagedExamHistoryResponse.AttemptItem> items = pagedAttempts.getContent().stream()
                .map(a -> {
                    double scoreOutOf10 = Math.round((a.getCorrectCount() * 100.0) / a.getTotalQuestions()) / 10.0;
                    return new PagedExamHistoryResponse.AttemptItem(
                            a.getId(),
                            a.getQuizSet().getId(),
                            a.getQuizSet().getTitle(),
                            a.getFinishedAt(),
                            a.getTotalQuestions(),
                            a.getCorrectCount(),
                            a.getScorePercent(),
                            scoreOutOf10
                    );
                })
                .toList();

        return new PagedExamHistoryResponse(
                items,
                pagedAttempts.getNumber(),
                pagedAttempts.getSize(),
                pagedAttempts.getTotalElements(),
                pagedAttempts.getTotalPages(),
                pagedAttempts.isFirst(),
                pagedAttempts.isLast()
        );
    }

    private QuizSet requireCanPractice(Long userId, Long quizSetId) {
        QuizSet quizSet = quizSetRepo.findById(quizSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ đề"));

        // Cho phép: chủ sở hữu hoặc bộ đề PUBLIC
        if (!quizSet.getCreatedBy().getId().equals(userId)
                && !"PUBLIC".equalsIgnoreCase(quizSet.getVisibility())) {
            throw new ForbiddenException("Bạn không có quyền làm bộ đề này");
        }

        return quizSet;
    }

    private String normalizeKey(String key) {
        if (key == null) return null;
        return key.trim().toUpperCase(Locale.ROOT);
    }
}
