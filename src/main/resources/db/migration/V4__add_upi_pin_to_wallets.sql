-- V4: Add UPI PIN to wallets (stored as BCrypt hash, never plain text)
ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS upi_pin_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS upi_pin_set BOOLEAN NOT NULL DEFAULT FALSE;

-- UPI transactions table
CREATE TABLE IF NOT EXISTS upi_transactions (
                                                id BIGSERIAL PRIMARY KEY,
                                                transaction_id BIGINT REFERENCES transactions(id),
    sender_upi_id VARCHAR(100) NOT NULL,
    receiver_upi_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_upi_sender ON upi_transactions(sender_upi_id);
CREATE INDEX IF NOT EXISTS idx_upi_receiver ON upi_transactions(receiver_upi_id);
CREATE INDEX IF NOT EXISTS idx_upi_transaction_id ON upi_transactions(transaction_id);