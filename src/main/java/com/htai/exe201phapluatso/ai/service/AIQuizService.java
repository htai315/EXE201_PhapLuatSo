package com.htai.exe201phapluatso.ai.service;

import com.htai.exe201phapluatso.ai.dto.AIQuestionDTO;
import com.htai.exe201phapluatso.ai.dto.GenerateQuestionsRequest;
import com.htai.exe201phapluatso.ai.dto.GenerateQuestionsResponse;
import com.htai.exe201phapluatso.auth.entity.User;
import com.htai.exe201phapluatso.auth.repo.UserRepo;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.common.exception.NotFoundException;
import com.htai.exe201phapluatso.credit.service.CreditService;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestion;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestionOption;
import com.htai.exe201phapluatso.quiz.entity.QuizSet;
import com.htai.exe201phapluatso.quiz.repo.QuizQuestionRepo;
import com.htai.exe201phapluatso.quiz.repo.QuizSetRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for AI-powered quiz generation
 * Requires quiz generation credits
 */
@Service
public class AIQuizService {

    private static final Logger log = LoggerFactory.getLogger(AIQuizService.class);
    
    // Constants for validation
    private static final int MIN_QUESTION_COUNT = 1;
    private static final int MAX_QUESTION_COUNT = 30;

    private final DocumentParserService documentParser;
    private final OpenAIService aiService;
    private final QuizSetRepo quizSetRepo;
    private final QuizQuestionRepo questionRepo;
    private final UserRepo userRepo;
    private final CreditService creditService;

    public AIQuizService(
            DocumentParserService documentParser,
            OpenAIService aiService,
            QuizSetRepo quizSetRepo,
            QuizQuestionRepo questionRepo,
            UserRepo userRepo,
            CreditService creditService
    ) {
        this.documentParser = documentParser;
        this.aiService = aiService;
        this.quizSetRepo = quizSetRepo;
        this.questionRepo = questionRepo;
        this.userRepo = userRepo;
        this.creditService = creditService;
    }

    /**
     * Generate quiz questions from uploaded document using AI
     * Requires 1 quiz generation credit per request
     * 
     * @param userEmail User email
     * @param file Uploaded document file
     * @param request Generation request parameters
     * @return Generated questions response
     * @throws com.htai.exe201phapluatso.common.exception.ForbiddenException if insufficient credits
     */
    @Transactional
    public GenerateQuestionsResponse generateQuestionsFromDocument(
            String userEmail,
            MultipartFile file,
            GenerateQuestionsRequest request
    ) {
        // 1. Validate request
        validateRequest(request);
        
        // 2. Get user
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        // 3. Check and deduct credit BEFORE processing
        creditService.checkAndDeductQuizGenCredit(user.getId());
        
        log.info("Generating quiz for user {}: {} questions from document", 
                user.getId(), request.questionCount());

        // 4. Extract text from document
        String documentText = documentParser.extractText(file);

        // 5. Generate questions using AI
        List<AIQuestionDTO> aiQuestions = aiService.generateQuestions(
                documentText,
                request.questionCount()
        );

        // 6. Create quiz set
        QuizSet quizSet = new QuizSet();
        quizSet.setTitle(sanitizeInput(request.quizSetName()));
        quizSet.setDescription(sanitizeInput(request.description()));
        quizSet.setCreatedBy(user);
        quizSet.setCreatedAt(LocalDateTime.now());
        quizSet.setUpdatedAt(LocalDateTime.now());
        quizSet = quizSetRepo.save(quizSet);

        // 7. Batch save questions - FIX N+1 query
        List<QuizQuestion> questionsToSave = new ArrayList<>();
        for (int i = 0; i < aiQuestions.size(); i++) {
            AIQuestionDTO aiQ = aiQuestions.get(i);
            QuizQuestion question = createQuestionFromAI(aiQ, quizSet, i + 1);
            questionsToSave.add(question);
        }
        List<QuizQuestion> savedQuestions = questionRepo.saveAll(questionsToSave);

        log.info("Quiz generation completed for user {}. Created quiz set {} with {} questions", 
                user.getId(), quizSet.getId(), savedQuestions.size());

        // 8. Return response
        return new GenerateQuestionsResponse(
                quizSet.getId(),
                quizSet.getTitle(),
                savedQuestions.size(),
                aiQuestions
        );
    }
    
    /**
     * Validate generation request
     */
    private void validateRequest(GenerateQuestionsRequest request) {
        if (request.quizSetName() == null || request.quizSetName().isBlank()) {
            throw new BadRequestException("Tên bộ câu hỏi không được để trống");
        }
        if (request.quizSetName().length() > 200) {
            throw new BadRequestException("Tên bộ câu hỏi không được vượt quá 200 ký tự");
        }
        if (request.questionCount() < MIN_QUESTION_COUNT || request.questionCount() > MAX_QUESTION_COUNT) {
            throw new BadRequestException(
                String.format("Số câu hỏi phải từ %d đến %d", MIN_QUESTION_COUNT, MAX_QUESTION_COUNT)
            );
        }
    }
    
    /**
     * Sanitize input to prevent XSS
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .trim();
    }

    private QuizQuestion createQuestionFromAI(AIQuestionDTO aiQ, QuizSet quizSet, int orderIndex) {
        QuizQuestion question = new QuizQuestion();
        question.setQuizSet(quizSet);
        // Sanitize AI-generated content
        question.setQuestionText(sanitizeInput(aiQ.question()));
        question.setExplanation(sanitizeInput(aiQ.explanation()));
        question.setSortOrder(orderIndex);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());

        // Create options with sanitized content
        List<QuizQuestionOption> options = new ArrayList<>();
        options.add(createOption(question, "A", sanitizeInput(aiQ.optionA()), aiQ.correctAnswer().equals("A")));
        options.add(createOption(question, "B", sanitizeInput(aiQ.optionB()), aiQ.correctAnswer().equals("B")));
        options.add(createOption(question, "C", sanitizeInput(aiQ.optionC()), aiQ.correctAnswer().equals("C")));
        options.add(createOption(question, "D", sanitizeInput(aiQ.optionD()), aiQ.correctAnswer().equals("D")));

        question.setOptions(options);
        return question;
    }

    private QuizQuestionOption createOption(QuizQuestion question, String optionKey, String optionText, boolean isCorrect) {
        QuizQuestionOption option = new QuizQuestionOption();
        option.setQuestion(question);
        option.setOptionKey(optionKey);
        option.setOptionText(optionText);
        option.setCorrect(isCorrect);
        return option;
    }
}
