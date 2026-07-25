ALTER TABLE upi_transactions
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);

CREATE UNIQUE INDEX IF NOT EXISTS idx_upi_idempotency_key
    ON upi_transactions(idempotency_key)
    WHERE idempotency_key IS NOT NULL;