-- Add checkout_url and qr_code fields to payments table for reuse functionality
ALTER TABLE payments ADD COLUMN IF NOT EXISTS checkout_url VARCHAR(500);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS qr_code TEXT;

COMMENT ON COLUMN payments.checkout_url IS 'PayOS checkout URL for payment';
COMMENT ON COLUMN payments.qr_code IS 'VietQR string or base64 QR image for reuse';
