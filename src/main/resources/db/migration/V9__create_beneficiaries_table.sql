-- V9: Beneficiaries table
CREATE TABLE IF NOT EXISTS beneficiaries (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL REFERENCES users(id),
    nickname VARCHAR(100) NOT NULL,
    beneficiary_type VARCHAR(30) NOT NULL,
    upi_id VARCHAR(100),
    wallet_id BIGINT REFERENCES wallets(id),
    beneficiary_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, nickname)
    );

CREATE INDEX IF NOT EXISTS idx_beneficiary_user_id ON beneficiaries(user_id);
CREATE INDEX IF NOT EXISTS idx_beneficiary_upi_id ON beneficiaries(upi_id);