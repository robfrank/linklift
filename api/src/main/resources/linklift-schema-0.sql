-- Creazione del tipo vertice Link
CREATE VERTEX TYPE Link
  IF NOT EXISTS;

-- Proprietà del vertice Link
CREATE PROPERTY Link.url STRING
  IF NOT EXISTS (
    MANDATORY TRUE,
    NOTNULL TRUE,
    INDEX UNIQUE
  );

CREATE PROPERTY Link.title STRING IF NOT EXISTS;
CREATE PROPERTY Link.description STRING IF NOT EXISTS;
CREATE PROPERTY Link.extractedAt LONG IF NOT EXISTS;
CREATE PROPERTY Link.contentType STRING IF NOT EXISTS;

-- Creazione di un edge type per connessioni tra link
CREATE EDGE TYPE LinkConnection
  IF NOT EXISTS;