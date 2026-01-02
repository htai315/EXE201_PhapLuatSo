package com.htai.exe201phapluatso.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.htai.exe201phapluatso.ai.dto.AIQuestionDTO;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI Service - AI provider for the application
 * Uses GPT-4o-mini model for:
 * - Legal chatbot (RAG-based Q&A)
 * - Quiz question generation from documents
 * 
 * Supports chunking for large question counts (>25 questions)
 */
@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final int MAX_RETRIES = 2;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration API_TIMEOUT = Duration.ofSeconds(180);
    
    // Batch size for chunking - optimal for GPT-4o-mini output limit
    public static final int BATCH_SIZE = 20;

    public OpenAIService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generate text response from OpenAI (for chatbot)
     */
    public String generateText(String prompt) {
        return callOpenAIWithRetry(prompt, 0);
    }

    /**
     * Generate questions - single batch (for small counts)
     */
    public List<AIQuestionDTO> generateQuestions(String documentText, int count) {
        return generateQuestionsWithContext(documentText, count, null);
    }
    
    /**
     * Generate questions with context of existing questions to avoid duplicates
     * Used for chunking strategy when generating large number of questions
     * 
     * @param documentText The document content
     * @param count Number of questions to generate
     * @param existingQuestions List of already generated questions (null for first batch)
     * @return List of generated questions
     */
    public List<AIQuestionDTO> generateQuestionsWithContext(
            String documentText, 
            int count, 
            List<AIQuestionDTO> existingQuestions
    ) {
        String prompt = buildPromptWithContext(documentText, count, existingQuestions);
        String response = callOpenAIWithRetry(prompt, count);
        return parseResponse(response);
    }

    /**
     * Build prompt with context of existing questions to avoid duplicates
     */
    private String buildPromptWithContext(String documentText, int count, List<AIQuestionDTO> existingQuestions) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append(String.format("""
            Bạn là chuyên gia tạo câu hỏi trắc nghiệm về pháp luật Việt Nam.
            
            NHIỆM VỤ: Tạo CHÍNH XÁC %d câu hỏi trắc nghiệm từ tài liệu bên dưới.
            
            YÊU CẦU BẮT BUỘC:
            - PHẢI tạo ĐÚNG %d câu hỏi, không hơn không kém
            - Mỗi câu hỏi có 4 đáp án (A, B, C, D)
            - Chỉ có 1 đáp án đúng
            - Có giải thích chi tiết cho đáp án đúng
            - Câu hỏi phải rõ ràng, không mơ hồ
            - Đáp án sai phải hợp lý, không quá dễ loại trừ
            - Câu hỏi phải dựa trên nội dung tài liệu
            """, count, count));
        
        // Add context about existing questions to avoid duplicates
        if (existingQuestions != null && !existingQuestions.isEmpty()) {
            String existingTopics = existingQuestions.stream()
                    .map(AIQuestionDTO::question)
                    .limit(10) // Only include first 10 to save tokens
                    .collect(Collectors.joining("\n- ", "\n- ", ""));
            
            promptBuilder.append(String.format("""
            
            QUAN TRỌNG - TRÁNH TRÙNG LẶP:
            Đã có %d câu hỏi được tạo trước đó. KHÔNG tạo câu hỏi trùng hoặc tương tự với các chủ đề sau:%s
            
            Hãy tạo câu hỏi về các khía cạnh KHÁC của tài liệu.
            """, existingQuestions.size(), existingTopics));
        }
        
        promptBuilder.append(String.format("""
            
            LƯU Ý QUAN TRỌNG: 
            - Bạn PHẢI trả về ĐÚNG %d câu hỏi trong mảng JSON
            - Đếm lại trước khi trả về để đảm bảo đủ số lượng
            
            FORMAT JSON (trả về ĐÚNG format này, không thêm text nào khác):
            [
              {
                "question": "Câu hỏi ở đây?",
                "optionA": "Đáp án A",
                "optionB": "Đáp án B",
                "optionC": "Đáp án C",
                "optionD": "Đáp án D",
                "correctAnswer": "A",
                "explanation": "Giải thích tại sao A đúng"
              }
            ]
            
            TÀI LIỆU:
            """, count));
        
        promptBuilder.append(documentText);
        
        return promptBuilder.toString();
    }

    /**
     * Call OpenAI API with retry mechanism
     */
    private String callOpenAIWithRetry(String prompt, int questionCount) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {} for OpenAI API call", attempt);
                    Thread.sleep(RETRY_DELAY.toMillis() * attempt);
                }
                return callOpenAI(prompt, questionCount);
            } catch (WebClientResponseException e) {
                lastException = e;
                // Don't retry on 4xx errors (client errors)
                if (e.getStatusCode().is4xxClientError()) {
                    log.error("OpenAI API client error (no retry): {}", e.getStatusCode());
                    break;
                }
                log.warn("OpenAI API error on attempt {}: {}", attempt + 1, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BadRequestException("Bị gián đoạn khi gọi OpenAI API");
            } catch (Exception e) {
                lastException = e;
                log.warn("OpenAI API exception on attempt {}: {}", attempt + 1, e.getMessage());
            }
        }
        
        throw new BadRequestException("Lỗi khi gọi OpenAI API sau " + (MAX_RETRIES + 1) + " lần thử: " + 
            (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private String callOpenAI(String prompt, int questionCount) {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", prompt
                )
            ),
            "temperature", 0.7,
            "max_tokens", 16000
        );

        log.info("Calling OpenAI API with model: {}", model);
        if (questionCount > 0) {
            log.info("Requesting {} questions", questionCount);
        }
        
        String response = webClient.post()
                .uri(OPENAI_API_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(body -> {
                            // Don't log full body - may contain sensitive info
                            log.error("OpenAI API Error: status={}", clientResponse.statusCode());
                            return new BadRequestException("Lỗi OpenAI API: " + clientResponse.statusCode());
                        })
                )
                .bodyToMono(String.class)
                .timeout(API_TIMEOUT)
                .block();

        if (response == null) {
            throw new BadRequestException("Không nhận được phản hồi từ OpenAI");
        }

        log.info("OpenAI response received successfully");
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Remove markdown code blocks if present
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();

            return text;
        } catch (Exception e) {
            throw new BadRequestException("Không thể parse response từ OpenAI");
        }
    }

    private List<AIQuestionDTO> parseResponse(String jsonText) {
        try {
            JsonNode questionsNode = objectMapper.readTree(jsonText);
            List<AIQuestionDTO> questions = new ArrayList<>();

            if (!questionsNode.isArray()) {
                throw new BadRequestException("OpenAI response không đúng format");
            }

            for (JsonNode node : questionsNode) {
                AIQuestionDTO question = new AIQuestionDTO(
                    node.path("question").asText(),
                    node.path("optionA").asText(),
                    node.path("optionB").asText(),
                    node.path("optionC").asText(),
                    node.path("optionD").asText(),
                    node.path("correctAnswer").asText().toUpperCase(),
                    node.path("explanation").asText()
                );

                question.validate();
                questions.add(question);
            }

            if (questions.isEmpty()) {
                throw new BadRequestException("OpenAI không tạo được câu hỏi nào");
            }

            log.info("Parsed {} questions from OpenAI response", questions.size());

            return questions;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Không thể parse câu hỏi từ OpenAI");
        }
    }
}
