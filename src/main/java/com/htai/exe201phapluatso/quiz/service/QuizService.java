package com.htai.exe201phapluatso.quiz.service;

import com.htai.exe201phapluatso.auth.entity.Subscription;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.SubscriptionRepo;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.ForbiddenException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.quiz.dto.CreateQuestionRequest;
import com.htai.exe201phapluatso.quiz.dto.CreateQuizSetRequest;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestion;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestionOption;
import com.htai.exe201phapluatso.quiz.entity.QuizSet;
import com.htai.exe201phapluatso.quiz.dto.QuestionResponse;
import com.htai.exe201phapluatso.quiz.repo.QuizAttemptAnswerRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizQuestionOptionRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizQuestionRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizSetRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizSetRepo quizSetRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizQuestionOptionRepo optionRepo;
    private final QuizAttemptAnswerRepo attemptAnswerRepo;
    private final UserRepo userRepo;
    private final SubscriptionRepo subscriptionRepo;

    public QuizService(
            QuizSetRepo quizSetRepo,
            QuizQuestionRepo questionRepo,
            QuizQuestionOptionRepo optionRepo,
            QuizAttemptAnswerRepo attemptAnswerRepo,
            UserRepo userRepo,
            SubscriptionRepo subscriptionRepo
    ) {
        this.quizSetRepo = quizSetRepo;
        this.questionRepo = questionRepo;
        this.optionRepo = optionRepo;
        this.attemptAnswerRepo = attemptAnswerRepo;
        this.userRepo = userRepo;
        this.subscriptionRepo = subscriptionRepo;
    }
    @Transactional
    public QuizSet createQuizSet(Long userId, CreateQuizSetRequest req) {
        User user = requireActiveStudent(userId);

        QuizSet set = new QuizSet();
        set.setCreatedBy(user);
        set.setTitle(req.title());
        set.setDescription(req.description());
        set.setStatus("DRAFT");
        set.setVisibility("PRIVATE");
        set.setCreatedAt(LocalDateTime.now());

        return quizSetRepo.save(set);
    }

    @Transactional
    public void addQuestion(Long userId, Long quizSetId, CreateQuestionRequest req) {
        QuizSet quizSet = quizSetRepo.findById(quizSetId)
                .orElseThrow(() -> new NotFoundException("Quiz set not found"));

        // Check ownership
        if (!quizSet.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("You are not the owner of this quiz set");
        }

        validateOptions(req.options());

        // Calculate sort order (next available order in the quiz set)
        List<QuizQuestion> existingQuestions = questionRepo.findByQuizSetIdOrderBySortOrderAsc(quizSetId);
        int nextSortOrder = existingQuestions.isEmpty() 
                ? 0 
                : existingQuestions.get(existingQuestions.size() - 1).getSortOrder() + 1;

        // Create question
        QuizQuestion question = new QuizQuestion();
        question.setQuizSet(quizSet);
        question.setQuestionText(req.questionText());
        question.setExplanation(req.explanation());
        question.setSortOrder(nextSortOrder);
        question = questionRepo.save(question);

        // Create options (batch save)
        List<QuizQuestionOption> options = new ArrayList<>();
        for (var opt : req.options()) {
            QuizQuestionOption option = new QuizQuestionOption();
            option.setQuestion(question);
            option.setOptionKey(opt.optionKey());
            option.setOptionText(opt.optionText());
            option.setCorrect(opt.isCorrect());
            options.add(option);
        }
        optionRepo.saveAll(options);

        // Update quiz set updated_at
        quizSet.setUpdatedAt(LocalDateTime.now());
        quizSetRepo.save(quizSet);
    }

    @Transactional(readOnly = true)
    public List<QuizSet> getQuizSetsForUser(Long userId) {
        // Đảm bảo user còn hiệu lực giống như khi tạo bộ đề
        requireActiveStudent(userId);
        return quizSetRepo.findByCreatedById(userId);
    }

    @Transactional(readOnly = true)
    public QuizSet getOwnedQuizSet(Long userId, Long quizSetId) {
        QuizSet quizSet = quizSetRepo.findById(quizSetId)
                .orElseThrow(() -> new NotFoundException("Quiz set not found"));

        if (!quizSet.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("You are not the owner of this quiz set");
        }

        return quizSet;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsForSet(Long userId, Long quizSetId) {
        // Sẽ ném NotFound/Forbidden nếu không hợp lệ
        getOwnedQuizSet(userId, quizSetId);

        // FIX N+1 QUERY: Sử dụng JOIN FETCH để load questions + options trong 1 query
        // Trước: N+1 queries (1 query cho questions + N queries cho options)
        // Sau: 1 query duy nhất
        List<QuizQuestion> questions = questionRepo.findByQuizSetIdWithOptions(quizSetId);
        
        // Map to response DTOs - options đã được fetch sẵn
        return questions.stream()
                .map(q -> QuestionResponse.from(q, q.getOptions()))
                .toList();
    }

    @Transactional
    public void deleteQuizSet(Long userId, Long quizSetId) {
        QuizSet quizSet = getOwnedQuizSet(userId, quizSetId);
        quizSetRepo.delete(quizSet); // questions & options are ON DELETE CASCADE
    }

    @Transactional
    public void deleteQuestion(Long userId, Long quizSetId, Long questionId) {
        QuizSet quizSet = getOwnedQuizSet(userId, quizSetId);

        QuizQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        if (!question.getQuizSet().getId().equals(quizSet.getId())) {
            throw new BadRequestException("Question does not belong to this quiz set");
        }

        // Xóa các attempt answers liên quan trước (nếu câu hỏi đã được làm)
        attemptAnswerRepo.deleteByQuestionId(questionId);
        
        questionRepo.delete(question); // options ON DELETE CASCADE
    }

    @Transactional
    public void updateQuestion(Long userId, Long quizSetId, Long questionId, CreateQuestionRequest req) {
        QuizSet quizSet = getOwnedQuizSet(userId, quizSetId);

        QuizQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        if (!question.getQuizSet().getId().equals(quizSet.getId())) {
            throw new BadRequestException("Question does not belong to this quiz set");
        }

        validateOptions(req.options());

        // update question fields
        question.setQuestionText(req.questionText());
        question.setExplanation(req.explanation());
        question.setUpdatedAt(LocalDateTime.now());
        questionRepo.save(question);

        // remove old options and recreate (delete by query to avoid unique index conflicts)
        optionRepo.deleteByQuestionId(questionId);

        List<QuizQuestionOption> newOptions = new ArrayList<>();
        for (var opt : req.options()) {
            QuizQuestionOption option = new QuizQuestionOption();
            option.setQuestion(question);
            option.setOptionKey(opt.optionKey());
            option.setOptionText(opt.optionText());
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

    // -------- HELPERS --------
    private User requireActiveStudent(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Lấy subscription ACTIVE mới nhất của user
        Subscription subscription = subscriptionRepo
                .findTopByUserIdAndStatusOrderByStartAtDesc(userId, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("No subscription found"));

        // Check plan
        if (!"STUDENT".equals(subscription.getPlan().getCode())) {
            throw new ForbiddenException("Only STUDENT plan users can create quiz sets");
        }

        // Check subscription expiration
        if (subscription.getEndAt() != null && LocalDateTime.now().isAfter(subscription.getEndAt())) {
            throw new ForbiddenException("Subscription has expired");
        }

        return user;
    }

    private void validateOptions(List<CreateQuestionRequest.OptionDto> options) {
        if (options == null || options.size() != 4) {
            throw new BadRequestException("Must have exactly 4 options");
        }

        Set<String> keys = options.stream()
                .map(CreateQuestionRequest.OptionDto::optionKey)
                .collect(Collectors.toSet());

        if (!keys.equals(Set.of("A", "B", "C", "D"))) {
            throw new BadRequestException("Options must have keys: A, B, C, D");
        }

        long correctCount = options.stream()
                .filter(CreateQuestionRequest.OptionDto::isCorrect)
                .count();

        if (correctCount != 1) {
            throw new BadRequestException("Must have exactly 1 correct option");
        }
    }
}
