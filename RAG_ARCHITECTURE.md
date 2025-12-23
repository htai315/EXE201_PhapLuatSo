# RAG Chatbot Architecture - Refactored

## Overview
Retrieval-Augmented Generation (RAG) system for legal Q&A with proper architecture.

## Components

### 1. **LegalSearchService** (Retrieval Layer)
- **Responsibility**: Find relevant legal articles
- **Input**: User question
- **Output**: Ranked list of relevant articles
- **Algorithm**: 
  - Keyword extraction with stop words filtering
  - Bigram extraction for context
  - SQL LIKE search with scoring
  - Title matches weighted 5x higher than content

### 2. **LegalChatService** (Orchestration Layer)
- **Responsibility**: Coordinate RAG pipeline
- **Steps**:
  1. Retrieve relevant articles (via SearchService)
  2. Build context from articles
  3. Create AI prompt with context
  4. Call AI service
  5. Build response with citations
- **Error Handling**: Graceful fallback if no articles found

### 3. **LegalChatController** (API Layer)
- **Responsibility**: HTTP endpoint
- **Endpoint**: POST `/api/legal/chat/ask`
- **Input**: `ChatRequest { question: string }`
- **Output**: `ChatResponse { answer: string, citations: Citation[] }`

## Data Flow

```
User Question
    ↓
Controller (validation)
    ↓
ChatService (orchestration)
    ↓
SearchService (retrieval) → Top 10 articles
    ↓
ChatService (context building)
    ↓
GeminiAI (generation)
    ↓
ChatService (citation building)
    ↓
Controller (response)
    ↓
User Answer + Citations
```

## Improvements Made

### Search Quality
- ✅ Keyword extraction with 40+ stop words
- ✅ Bigram extraction for phrases
- ✅ Scoring system (title: 5pts, content: 1pt)
- ✅ Top 10 articles (was 5)

### Code Quality
- ✅ Clear separation of concerns
- ✅ Proper error handling
- ✅ Logging for debugging
- ✅ Constants for magic numbers
- ✅ Validation

### Performance
- ⚠️ No caching yet (future improvement)
- ⚠️ Synchronous AI calls (future: async)
- ⚠️ No rate limiting (future improvement)

## Future Enhancements

1. **Caching**: Cache search results for common questions
2. **Async**: Non-blocking AI calls
3. **Rate Limiting**: Prevent abuse
4. **Vector Search**: Semantic search with embeddings
5. **Feedback Loop**: Learn from user feedback
