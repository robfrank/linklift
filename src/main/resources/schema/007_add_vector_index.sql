CREATE PROPERTY Content.embedding IF NOT EXISTS ARRAY_OF_FLOATS;

CREATE INDEX IF NOT EXISTS ON Content(embedding) LSM_VECTOR METADATA {
    "dimensions": 384,
    "maxConnections": 16,
    "beamWidth": 100,
    "similarity": "COSINE"
    };
