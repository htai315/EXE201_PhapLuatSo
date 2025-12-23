package com.htai.exe201phapluatso.legal.config;

import java.util.Set;

/**
 * Configuration constants for legal search and RAG system
 */
public final class LegalSearchConfig {
    
    private LegalSearchConfig() {
        // Utility class, prevent instantiation
    }
    
    // Search parameters
    public static final int DEFAULT_SEARCH_LIMIT = 10;
    public static final int MIN_KEYWORD_LENGTH = 2;
    public static final int MAX_KEYWORDS = 10;
    public static final int MAX_BIGRAMS = 5;
    
    // Scoring weights
    public static final int TITLE_MATCH_WEIGHT = 5;
    public static final int CONTENT_MATCH_WEIGHT = 1;
    
    // AI prompt parameters
    public static final int MAX_ANSWER_WORDS = 300;
    public static final int CITATION_PREVIEW_LENGTH = 200;
    
    // Vietnamese stop words for keyword extraction
    public static final Set<String> STOP_WORDS = Set.of(
        // Common words
        "là", "của", "và", "có", "được", "trong", "cho", "với", "để",
        "như", "thế", "nào", "gì", "khi", "nếu", "thì", "hay", "hoặc",
        
        // Pronouns
        "tôi", "bạn", "anh", "chị", "em", "mình",
        
        // Particles
        "vậy", "ạ", "à", "ơi", "nhé", "nha",
        
        // Determiners
        "các", "những", "này", "đó", "ấy", "kia",
        
        // Common nouns
        "việc", "cái", "điều", "người",
        
        // Numbers
        "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười",
        
        // Negation and modals
        "không", "chưa", "đã", "sẽ", "phải", "cần", "nên", "bị", "bởi",
        
        // Prepositions
        "ở", "tại", "từ", "đến", "về", "theo", "trên", "dưới", "giữa"
    );
}
