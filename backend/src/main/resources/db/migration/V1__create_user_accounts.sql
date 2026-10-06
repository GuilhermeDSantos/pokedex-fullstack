CREATE TABLE user_accounts (
    id            UUID         NOT NULL,
    email         VARCHAR(254) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_email UNIQUE (email)
);
