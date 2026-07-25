-- Remove full unique constraint on idempotency_key
ALTER TABLE upi_transactions
DROP CONSTRAINT IF EXISTS upi_transactions_idempotency_key_key;

-- Drop old index if exists
DROP INDEX IF EXISTS idx_upi_idempotency_key;

-- Add partial unique index — only block duplicate COMPLETED records
CREATE UNIQUE INDEX idx_upi_completed_idempotency_key
    ON upi_transactions (idempotency_key)
    WHERE status = 'COMPLETED';