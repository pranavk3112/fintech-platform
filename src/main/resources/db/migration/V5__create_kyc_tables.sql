-- V5: KYC documents and audit table
CREATE TABLE IF NOT EXISTS kyc_documents (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL REFERENCES users(id),
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    document_url VARCHAR(500),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    verified_at TIMESTAMP,
    verified_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS kyc_audit_log (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    performed_by VARCHAR(255),
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_kyc_user_id ON kyc_documents(user_id);
CREATE INDEX IF NOT EXISTS idx_kyc_status ON kyc_documents(verification_status);
CREATE INDEX IF NOT EXISTS idx_kyc_audit_user_id ON kyc_audit_log(user_id);