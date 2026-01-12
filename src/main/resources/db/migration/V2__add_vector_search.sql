-- ============================================================================
-- V2: ADD VECTOR SEARCH CAPABILITY FOR LEGAL ARTICLES
-- Uses pgvector extension for semantic search
-- OpenAI text-embedding-3-small produces 1536-dimensional vectors
-- ============================================================================

-- Add embedding column to legal_articles
ALTER TABLE legal_articles 
ADD COLUMN IF NOT EXISTS embedding vector(1536) NULL;

-- Create index for fast vector similarity search (IVFFlat)
-- This index type is good for approximate nearest neighbor search
CREATE INDEX IF NOT EXISTS ix_legal_articles_embedding 
ON legal_articles USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Add column to track if embedding has been generated
ALTER TABLE legal_articles 
ADD COLUMN IF NOT EXISTS embedding_updated_at TIMESTAMP NULL;

-- Create a function to search by vector similarity
CREATE OR REPLACE FUNCTION search_articles_by_vector(
    query_embedding vector(1536),
    similarity_threshold FLOAT DEFAULT 0.3,
    max_results INT DEFAULT 10
)
RETURNS TABLE (
    article_id BIGINT,
    document_id BIGINT,
    article_number INT,
    article_title VARCHAR(1000),
    content TEXT,
    document_name VARCHAR(500),
    document_status VARCHAR(50),
    similarity FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        a.id AS article_id,
        a.document_id,
        a.article_number,
        a.article_title,
        a.content,
        d.document_name,
        d.status AS document_status,
        1 - (a.embedding <=> query_embedding) AS similarity
    FROM legal_articles a
    JOIN legal_documents d ON a.document_id = d.id
    WHERE a.embedding IS NOT NULL
      AND d.status = 'Còn hiệu lực'
      AND 1 - (a.embedding <=> query_embedding) >= similarity_threshold
    ORDER BY a.embedding <=> query_embedding
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Create hybrid search function (combines vector + keyword)
CREATE OR REPLACE FUNCTION hybrid_search_articles(
    query_embedding vector(1536),
    keywords TEXT[],
    vector_weight FLOAT DEFAULT 0.7,
    keyword_weight FLOAT DEFAULT 0.3,
    max_results INT DEFAULT 10
)
RETURNS TABLE (
    article_id BIGINT,
    document_id BIGINT,
    article_number INT,
    article_title VARCHAR(1000),
    content TEXT,
    document_name VARCHAR(500),
    vector_score FLOAT,
    keyword_score FLOAT,
    combined_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    WITH vector_results AS (
        SELECT 
            a.id,
            a.document_id,
            a.article_number,
            a.article_title,
            a.content,
            d.document_name,
            CASE 
                WHEN a.embedding IS NOT NULL 
                THEN 1 - (a.embedding <=> query_embedding)
                ELSE 0 
            END AS v_score
        FROM legal_articles a
        JOIN legal_documents d ON a.document_id = d.id
        WHERE d.status = 'Còn hiệu lực'
    ),
    keyword_results AS (
        SELECT 
            vr.id,
            COALESCE(
                (SELECT SUM(
                    CASE 
                        WHEN vr.article_title ILIKE '%' || kw || '%' THEN 3
                        ELSE 0 
                    END +
                    CASE 
                        WHEN vr.content ILIKE '%' || kw || '%' THEN 1
                        ELSE 0 
                    END
                ) FROM unnest(keywords) AS kw),
                0
            )::FLOAT / GREATEST(array_length(keywords, 1), 1) AS k_score
        FROM vector_results vr
    )
    SELECT 
        vr.id AS article_id,
        vr.document_id,
        vr.article_number,
        vr.article_title,
        vr.content,
        vr.document_name,
        vr.v_score AS vector_score,
        kr.k_score AS keyword_score,
        (vr.v_score * vector_weight + kr.k_score * keyword_weight) AS combined_score
    FROM vector_results vr
    JOIN keyword_results kr ON vr.id = kr.id
    WHERE vr.v_score > 0.2 OR kr.k_score > 0
    ORDER BY (vr.v_score * vector_weight + kr.k_score * keyword_weight) DESC
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Add comment for documentation
COMMENT ON COLUMN legal_articles.embedding IS 'Vector embedding from OpenAI text-embedding-3-small (1536 dimensions)';
COMMENT ON COLUMN legal_articles.embedding_updated_at IS 'Timestamp when embedding was last generated/updated';

-- ============================================================================
-- END OF MIGRATION V2
-- ============================================================================
