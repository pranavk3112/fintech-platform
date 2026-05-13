-- V1: Baseline schema representing current state
-- Users table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Wallets table
CREATE TABLE IF NOT EXISTS wallets (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    balance NUMERIC(19,4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
                                            id BIGSERIAL PRIMARY KEY,
                                            idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    source_wallet_id BIGINT REFERENCES wallets(id),
    destination_wallet_id BIGINT REFERENCES wallets(id),
    amount NUMERIC(19,4) NOT NULL,
    balance_before_source NUMERIC(19,4),
    balance_after_source NUMERIC(19,4),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL
    );