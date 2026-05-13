-- Remove unique constraint on idempotency_key
-- to allow retries while preserving failed transaction history
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_idempotency_key_key;

-- Add a new unique index only on COMPLETED transactions
CREATE UNIQUE INDEX idx_unique_completed_idempotency_key
    ON transactions (idempotency_key)
    WHERE status = 'COMPLETED';