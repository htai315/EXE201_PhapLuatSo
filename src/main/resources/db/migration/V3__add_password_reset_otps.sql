-- Migration V3: Add password_reset_otps table
-- Purpose: Store OTP codes for password reset functionality

CREATE TABLE password_reset_otps (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email NVARCHAR(255) NOT NULL,
    otp NVARCHAR(6) NOT NULL,
    expires_at DATETIME2 NOT NULL,
    is_used BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    
    INDEX idx_email (email),
    INDEX idx_otp (otp),
    INDEX idx_expires_at (expires_at)
);
