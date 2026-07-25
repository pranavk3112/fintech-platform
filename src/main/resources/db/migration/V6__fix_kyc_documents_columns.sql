-- V6: Add missing columns to existing kyc_documents table
ALTER TABLE kyc_documents
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS verified_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Set default updated_at for existing rows
UPDATE kyc_documents SET updated_at = created_at WHERE updated_at IS NULL;

-- Add kyc_audit_log if not exists
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

CREATE INDEX IF NOT EXISTS idx_kyc_audit_user_id ON kyc_audit_log(user_id);