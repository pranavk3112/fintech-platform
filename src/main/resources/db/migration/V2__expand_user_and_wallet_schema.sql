-- V2: Expand users table with realistic banking fields

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(15) UNIQUE,
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20),
    ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS address_street VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_pincode VARCHAR(10),
    ADD COLUMN IF NOT EXISTS address_country VARCHAR(100) DEFAULT 'India',
    ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- Expand wallets table
ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS wallet_type VARCHAR(30) NOT NULL DEFAULT 'SAVINGS',
    ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100) UNIQUE,
    ADD COLUMN IF NOT EXISTS daily_limit NUMERIC(19,4) NOT NULL DEFAULT 100000.0000,
    ADD COLUMN IF NOT EXISTS monthly_limit NUMERIC(19,4) NOT NULL DEFAULT 1000000.0000,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'INR';

-- Create KYC documents table
CREATE TABLE IF NOT EXISTS kyc_documents (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL REFERENCES users(id),
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    document_url VARCHAR(500),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_mobile ON users(mobile_number);
CREATE INDEX IF NOT EXISTS idx_users_kyc_status ON users(kyc_status);
CREATE INDEX IF NOT EXISTS idx_wallets_upi_id ON wallets(upi_id);
CREATE INDEX IF NOT EXISTS idx_transactions_idempotency ON transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_transactions_source_wallet ON transactions(source_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_destination_wallet ON transactions(destination_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_kyc_user_id ON kyc_documents(user_id);