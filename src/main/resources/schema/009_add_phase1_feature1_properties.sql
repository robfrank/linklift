-- Add Phase 1 Feature 1: Automated Content & Metadata Extraction properties
CREATE PROPERTY Content.summary IF NOT EXISTS STRING;
CREATE PROPERTY Content.heroImageUrl IF NOT EXISTS STRING;
CREATE PROPERTY Content.extractedTitle IF NOT EXISTS STRING;
CREATE PROPERTY Content.extractedDescription IF NOT EXISTS STRING;
CREATE PROPERTY Content.author IF NOT EXISTS STRING;
CREATE PROPERTY Content.publishedDate IF NOT EXISTS DATETIME_SECOND;
