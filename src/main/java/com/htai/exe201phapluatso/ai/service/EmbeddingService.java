package com.htai.exe201phapluatso.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for generating text embeddings using OpenAI API
 * Uses text-embedding-3-small model (1536 dimensions)
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final String OPENAI_EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int EMBEDDING_DIMENSIONS = 1536;
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRIES = 2;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    public EmbeddingService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generate embedding vector for a single text
     * 
     * @param text Text to embed (max ~8000 tokens)
     * @return float array of 1536 dimensions
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BadRequestException("Text cannot be empty for embedding");
        }

        // Truncate if too long (roughly 8000 tokens ~ 32000 chars for Vietnamese)
        String truncatedText = text.length() > 30000 ? text.substring(0, 30000) : text;

        return callEmbeddingAPIWithRetry(truncatedText);
    }

    /**
     * Generate embedding for article content (combines title + content)
     */
    public float[] generateArticleEmbedding(String title, String content) {
        StringBuilder textBuilder = new StringBuilder();
        
        if (title != null && !title.isEmpty()) {
            textBuilder.append("Tiêu đề: ").append(title).append("\n\n");
        }
        
        textBuilder.append("Nội dung: ").append(content);
        
        return generateEmbedding(textBuilder.toString());
    }

    /**
     * Call OpenAI Embedding API with retry mechanism
     */
    private float[] callEmbeddingAPIWithRetry(String text) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {} for embedding API", attempt);
                    Thread.sleep(1000L * attempt);
                }
                return callEmbeddingAPI(text);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BadRequestException("Embedding request interrupted");
            } catch (Exception e) {
                lastException = e;
                log.warn("Embedding API error on attempt {}: {}", attempt + 1, e.getMessage());
            }
        }

        throw new BadRequestException("Failed to generate embedding after retries: " + 
            (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private float[] callEmbeddingAPI(String text) {
        Map<String, Object> requestBody = Map.of(
            "model", EMBEDDING_MODEL,
            "input", text,
            "dimensions", EMBEDDING_DIMENSIONS
        );

        log.debug("Calling OpenAI Embedding API for text of length: {}", text.length());

        String response = webClient.post()
            .uri(OPENAI_EMBEDDING_URL)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(API_TIMEOUT)
            .block();

        if (response == null) {
            throw new BadRequestException("No response from embedding API");
        }

        return parseEmbeddingResponse(response);
    }

    private float[] parseEmbeddingResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.path("data").get(0).path("embedding");

            if (!embeddingNode.isArray()) {
                throw new BadRequestException("Invalid embedding response format");
            }

            float[] embedding = new float[EMBEDDING_DIMENSIONS];
            for (int i = 0; i < embeddingNode.size() && i < EMBEDDING_DIMENSIONS; i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }

            log.debug("Successfully parsed embedding with {} dimensions", embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("Failed to parse embedding response", e);
            throw new BadRequestException("Failed to parse embedding: " + e.getMessage());
        }
    }

    /**
     * Convert float array to PostgreSQL vector string format
     * Format: [0.1,0.2,0.3,...]
     */
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Get embedding dimensions
     */
    public int getDimensions() {
        return EMBEDDING_DIMENSIONS;
    }
}
