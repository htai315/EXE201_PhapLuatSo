package com.htai.exe201phapluatso.quiz.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.htai.exe201phapluatso.quiz.dto.ExamSessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis implementation của ExamSessionStore
 * Primary store khi Redis khả dụng
 */
@Component
@Primary
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisExamSessionStore implements ExamSessionStore {

    private static final Logger log = LoggerFactory.getLogger(RedisExamSessionStore.class);
    private static final String KEY_PREFIX = "exam:session:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisExamSessionStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void save(String sessionKey, ExamSessionData session, Duration ttl) {
        try {
            String key = KEY_PREFIX + sessionKey;
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Saved session to Redis: {} with TTL: {}", key, ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session data for key: {}", sessionKey, e);
            throw new RuntimeException("Failed to save exam session", e);
        }
    }

    @Override
    public Optional<ExamSessionData> get(String sessionKey) {
        try {
            String key = KEY_PREFIX + sessionKey;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            ExamSessionData session = objectMapper.readValue(json, ExamSessionData.class);
            return Optional.of(session);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session data for key: {}", sessionKey, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String sessionKey) {
        String key = KEY_PREFIX + sessionKey;
        redisTemplate.delete(key);
        log.debug("Deleted session from Redis: {}", key);
    }

    @Override
    public boolean isAvailable() {
        try {
            var connection = redisTemplate.getConnectionFactory();
            if (connection == null) {
                return false;
            }
            connection.getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get Redis key prefix (for testing/monitoring)
     */
    public static String getKeyPrefix() {
        return KEY_PREFIX;
    }
}
