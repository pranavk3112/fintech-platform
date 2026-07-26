-- V10: Fixed Deposits and Recurring Deposits tables

CREATE TABLE IF NOT EXISTS fixed_deposits (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL REFERENCES users(id),
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    fd_number VARCHAR(50) NOT NULL UNIQUE,
    principal_amount NUMERIC(19,4) NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    interest_type VARCHAR(30) NOT NULL DEFAULT 'COMPOUND',
    compounding_frequency VARCHAR(30) NOT NULL DEFAULT 'QUARTERLY',
    maturity_amount NUMERIC(19,4) NOT NULL,
    interest_earned NUMERIC(19,4) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    premature_withdrawal_penalty NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS recurring_deposits (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  user_id BIGINT NOT NULL REFERENCES users(id),
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    rd_number VARCHAR(50) NOT NULL UNIQUE,
    monthly_installment NUMERIC(19,4) NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    total_deposited NUMERIC(19,4) NOT NULL DEFAULT 0,
    maturity_amount NUMERIC(19,4) NOT NULL,
    interest_earned NUMERIC(19,4) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    next_installment_date DATE NOT NULL,
    installments_paid INTEGER NOT NULL DEFAULT 0,
    missed_installments INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS rd_installments (
                                               id BIGSERIAL PRIMARY KEY,
                                               rd_id BIGINT NOT NULL REFERENCES recurring_deposits(id),
    installment_number INTEGER NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    due_date DATE NOT NULL,
    paid_date TIMESTAMP,
    penalty_amount NUMERIC(19,4) DEFAULT 0,
    created_at TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_fd_user_id ON fixed_deposits(user_id);
CREATE INDEX IF NOT EXISTS idx_fd_status ON fixed_deposits(status);
CREATE INDEX IF NOT EXISTS idx_fd_maturity_date ON fixed_deposits(maturity_date);
CREATE INDEX IF NOT EXISTS idx_rd_user_id ON recurring_deposits(user_id);
CREATE INDEX IF NOT EXISTS idx_rd_status ON recurring_deposits(status);
CREATE INDEX IF NOT EXISTS idx_rd_next_installment ON recurring_deposits(next_installment_date);
CREATE INDEX IF NOT EXISTS idx_rd_installments_rd_id ON rd_installments(rd_id);