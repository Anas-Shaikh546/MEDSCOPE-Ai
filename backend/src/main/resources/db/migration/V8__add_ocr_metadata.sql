-- Add OCR metadata columns to reports table for transparency
ALTER TABLE reports ADD COLUMN ocr_used BOOLEAN;
ALTER TABLE reports ADD COLUMN ocr_confidence DOUBLE PRECISION;
ALTER TABLE reports ADD COLUMN ocr_pages VARCHAR(255);
