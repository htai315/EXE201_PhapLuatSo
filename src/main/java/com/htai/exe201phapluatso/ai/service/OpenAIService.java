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

/**
 * OpenAI Service - AI provider for the application
 * Uses GPT-4o-mini model for:
 * - Legal chatbot (RAG-based Q&A)
 * - Quiz question generation from documents
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

    public List<AIQuestionDTO> generateQuestions(String documentText, int count) {
        String prompt = buildPrompt(documentText, count);
        String response = callOpenAIWithRetry(prompt, count);
        return parseResponse(response);
    }

    private String buildPrompt(String documentText, int count) {
        return String.format("""
            Bạn là chuyên gia tạo câu hỏi trắc nghiệm về pháp luật Việt Nam.
            
            Từ tài liệu sau, hãy tạo %d câu hỏi trắc nghiệm.
            
            YÊU CẦU:
            - Mỗi câu hỏi có 4 đáp án (A, B, C, D)
            - Chỉ có 1 đáp án đúng
            - Có giải thích chi tiết cho đáp án đúng
            - Câu hỏi phải rõ ràng, không mơ hồ
            - Đáp án sai phải hợp lý, không quá dễ loại trừ
            - Câu hỏi phải dựa trên nội dung tài liệu
            
            QUAN TRỌNG: Trả về ĐÚNG format JSON sau, không thêm text nào khác:
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
            %s
            """, count, documentText);
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
