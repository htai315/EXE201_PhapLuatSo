package com.htai.exe201phapluatso.quiz.service;

import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.quiz.dto.PagedExamHistoryResponse;
import com.htai.exe201phapluatso.quiz.dto.ExamDtos.*;
import com.htai.exe201phapluatso.quiz.entity.*;
import com.htai.exe201phapluatso.quiz.repo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý logic làm bài quiz (exam/practice)
 * OPTIMIZED: Sử dụng JOIN FETCH để tránh N+1 query problem
 */
@Service
public class QuizExamService {

    private static final int MAX_HISTORY_ITEMS = 10;

    private final QuizSetRepo quizSetRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizAttemptRepo attemptRepo;
    private final QuizAttemptAnswerRepo answerRepo;
    private final UserRepo userRepo;

    public QuizExamService(
            QuizSetRepo quizSetRepo,
            QuizQuestionRepo questionRepo,
            QuizAttemptRepo attemptRepo,
            QuizAttemptAnswerRepo answerRepo,
            UserRepo userRepo
    ) {
        this.quizSetRepo = quizSetRepo;
        this.questionRepo = questionRepo;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
    }

    /**
     * Bắt đầu làm bài quiz - OPTIMIZED: 1 query thay vì N+1
     */
    @Transactional(readOnly = true)
    public StartExamResponse startExam(Long userId, Long quizSetId) {
        QuizSet quizSet = requireCanPractice(userId, quizSetId);

        // FIX N+1: Sử dụng JOIN FETCH để load questions + options trong 1 query
        List<QuizQuestion> questions = questionRepo.findByQuizSetIdWithOptions(quizSetId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Bộ đề hiện chưa có câu hỏi nào");
        }

        // Options đã được fetch sẵn qua JOIN FETCH
        List<ExamQuestionDto> questionDtos = questions.stream()
                .map(question -> {
                    List<ExamOptionDto> optionDtos = question.getOptions().stream()
                            .map(o -> new ExamOptionDto(o.getOptionKey(), o.getOptionText()))
                            .toList();
                    return new ExamQuestionDto(
                            question.getId(),
                            question.getQuestionText(),
                            question.getExplanation(),
                            optionDtos
                    );
                })
                .toList();

        return new StartExamResponse(
                quizSet.getId(),
                quizSet.getTitle(),
                questionDtos.size(),
                questionDtos
        );
    }

    /**
     * Nộp bài quiz - OPTIMIZED: 1 query thay vì N+1
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

        // Build options map từ data đã fetch sẵn
        Map<Long, List<QuizQuestionOption>> optionsByQuestion = questions.stream()
                .collect(Collectors.toMap(
                        QuizQuestion::getId,
                        QuizQuestion::getOptions
                ));

        int totalQuestions = questions.size();
        int correctCount = 0;
        List<WrongQuestionDto> wrongs = new ArrayList<>();

        // Map answer by question id for quick lookup
        Map<Long, SubmitExamRequest.AnswerDto> answersByQid = req.answers().stream()
                .collect(Collectors.toMap(SubmitExamRequest.AnswerDto::questionId, a -> a, (a, b) -> a));

        // Evaluate each question (unanswered counts as wrong)
        for (QuizQuestion question : questions) {
            SubmitExamRequest.AnswerDto ans = answersByQid.get(question.getId());
            String selectedKey = ans != null ? normalizeKey(ans.selectedOptionKey()) : null;

            List<QuizQuestionOption> opts = optionsByQuestion.get(question.getId());
            if (opts == null || opts.isEmpty()) {
                continue;
            }

            Optional<QuizQuestionOption> correctOpt = opts.stream()
                    .filter(QuizQuestionOption::isCorrect)
                    .findFirst();

            if (correctOpt.isEmpty()) {
                continue;
            }

            String correctKey = correctOpt.get().getOptionKey();
            boolean isCorrect = selectedKey != null && correctKey.equalsIgnoreCase(selectedKey);
            if (isCorrect) {
                correctCount++;
            } else {
                wrongs.add(new WrongQuestionDto(
                        question.getId(),
                        question.getQuestionText(),
                        correctKey,
                        selectedKey
                ));
            }
        }

        int scorePercent = (int) Math.round((correctCount * 100.0) / totalQuestions);
        double scoreOutOf10 = Math.round((correctCount * 100.0) / totalQuestions) / 10.0;

        // Save attempt + answers
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setQuizSet(quizSet);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCorrectCount(correctCount);
        attempt.setScorePercent(scorePercent);
        attempt = attemptRepo.save(attempt);

        List<QuizAttemptAnswer> answers = new ArrayList<>();
        for (QuizQuestion question : questions) {
            SubmitExamRequest.AnswerDto ans = answersByQid.get(question.getId());
            String selectedKey = ans != null ? normalizeKey(ans.selectedOptionKey()) : null;

            if (selectedKey == null) {
                continue;
            }

            List<QuizQuestionOption> opts = optionsByQuestion.get(question.getId());
            if (opts == null || opts.isEmpty()) {
                continue;
            }
            Optional<QuizQuestionOption> correctOpt = opts.stream()
                    .filter(QuizQuestionOption::isCorrect)
                    .findFirst();
            boolean isCorrect = correctOpt.isPresent() &&
                    correctOpt.get().getOptionKey().equalsIgnoreCase(selectedKey);

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

        return new SubmitExamResponse(
                attempt.getId(),
                totalQuestions,
                correctCount,
                scorePercent,
                scoreOutOf10,
                wrongs
        );
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


