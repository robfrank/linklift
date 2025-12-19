-- Add embedding property (List of Floats)
-- nomic-embed-text uses 768 dimensions
CREATE PROPERTY Content.embedding IF NOT EXISTS LIST<FLOAT>;

-- Create Vector Index using HNSW
-- DISTANCE COSINE is optimal for text embeddings
-- M=16, EF=100 are typical defaults for HNSW
CREATE INDEX Content_embedding ON Content (embedding)
  VECTOR
  KEY M 16
  EF 100
  DISTANCE COSINE;
