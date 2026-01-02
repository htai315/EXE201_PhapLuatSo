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
 * Supports chunking for large question counts (>BATCH_SIZE)
 * Requires quiz generation credits
 */
@Service
public class AIQuizService {

    private static final Logger log = LoggerFactory.getLogger(AIQuizService.class);
    
    // Constants for validation
    private static final int MIN_QUESTION_COUNT = 15;
    private static final int MAX_QUESTION_COUNT = 40;
    private static final java.util.Set<Integer> ALLOWED_QUESTION_COUNTS = java.util.Set.of(15, 20, 30, 40);

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
     * Supports chunking for large question counts
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

        // 5. Generate questions using AI (with chunking if needed)
        List<AIQuestionDTO> aiQuestions = generateQuestionsWithChunking(
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

        // 7. Batch save questions
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
     * Generate questions with chunking support for large counts
     * Splits into multiple batches if count > BATCH_SIZE
     * Includes retry logic to ensure exact question count
     * 
     * @param documentText The document content
     * @param totalCount Total number of questions to generate
     * @return Combined list of all generated questions
     */
    private List<AIQuestionDTO> generateQuestionsWithChunking(String documentText, int totalCount) {
        int batchSize = OpenAIService.BATCH_SIZE;
        
        // If small count, use single request with retry for missing questions
        if (totalCount <= batchSize) {
            log.info("Single batch generation: {} questions", totalCount);
            List<AIQuestionDTO> questions = aiService.generateQuestions(documentText, totalCount);
            
            // Retry to fill missing questions if needed
            questions = fillMissingQuestions(documentText, questions, totalCount);
            return questions;
        }
        
        // Calculate number of batches needed
        int totalBatches = (totalCount + batchSize - 1) / batchSize;
        log.info("Chunked generation: {} questions in {} batches", totalCount, totalBatches);
        
        List<AIQuestionDTO> allQuestions = new ArrayList<>();
        
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int remaining = totalCount - allQuestions.size();
            int currentBatchSize = Math.min(batchSize, remaining);
            
            log.info("Generating batch {}/{}: {} questions", 
                    batchIndex + 1, totalBatches, currentBatchSize);
            
            try {
                List<AIQuestionDTO> batchQuestions;
                
                if (batchIndex == 0) {
                    // First batch - no context needed
                    batchQuestions = aiService.generateQuestions(documentText, currentBatchSize);
                } else {
                    // Subsequent batches - pass existing questions as context to avoid duplicates
                    batchQuestions = aiService.generateQuestionsWithContext(
                            documentText, 
                            currentBatchSize, 
                            allQuestions
                    );
                }
                
                allQuestions.addAll(batchQuestions);
                log.info("Batch {}/{} completed. Total questions so far: {}", 
                        batchIndex + 1, totalBatches, allQuestions.size());
                
            } catch (Exception e) {
                log.error("Error in batch {}: {}", batchIndex + 1, e.getMessage());
                
                // If we have some questions, continue to fill missing
                if (!allQuestions.isEmpty()) {
                    log.warn("Batch failed, will try to fill missing questions");
                    break;
                }
                
                // If first batch fails, propagate the error
                throw e;
            }
        }
        
        if (allQuestions.isEmpty()) {
            throw new BadRequestException("Không thể tạo câu hỏi từ tài liệu");
        }
        
        // Fill missing questions if we don't have enough
        allQuestions = fillMissingQuestions(documentText, allQuestions, totalCount);
        
        log.info("Chunked generation completed. Total: {} questions", allQuestions.size());
        return allQuestions;
    }
    
    /**
     * Fill missing questions if AI didn't return enough
     * Retries up to MAX_FILL_RETRIES times to get the exact count
     */
    private static final int MAX_FILL_RETRIES = 3;
    
    private List<AIQuestionDTO> fillMissingQuestions(
            String documentText, 
            List<AIQuestionDTO> existingQuestions, 
            int targetCount
    ) {
        List<AIQuestionDTO> allQuestions = new ArrayList<>(existingQuestions);
        int retryCount = 0;
        
        while (allQuestions.size() < targetCount && retryCount < MAX_FILL_RETRIES) {
            int missing = targetCount - allQuestions.size();
            log.info("Missing {} questions, attempting to fill (retry {}/{})", 
                    missing, retryCount + 1, MAX_FILL_RETRIES);
            
            try {
                List<AIQuestionDTO> additionalQuestions = aiService.generateQuestionsWithContext(
                        documentText,
                        missing,
                        allQuestions
                );
                
                allQuestions.addAll(additionalQuestions);
                log.info("Filled {} additional questions. Total now: {}", 
                        additionalQuestions.size(), allQuestions.size());
                
            } catch (Exception e) {
                log.warn("Failed to fill missing questions on retry {}: {}", retryCount + 1, e.getMessage());
            }
            
            retryCount++;
        }
        
        if (allQuestions.size() < targetCount) {
            log.warn("Could not generate all {} questions. Final count: {}", targetCount, allQuestions.size());
        }
        
        return allQuestions;
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
        if (!ALLOWED_QUESTION_COUNTS.contains(request.questionCount())) {
            throw new BadRequestException("Số câu hỏi phải là 15, 20, 30 hoặc 40");
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
