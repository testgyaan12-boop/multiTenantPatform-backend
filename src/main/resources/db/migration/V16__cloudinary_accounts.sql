CREATE TABLE cloudinary_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    label VARCHAR(120) NOT NULL,
    cloud_name VARCHAR(120) NOT NULL,
    api_key VARCHAR(120) NOT NULL,
    api_secret_enc TEXT NOT NULL,
    upload_preset VARCHAR(120) NULL,
    folder_prefix VARCHAR(160) NULL,
    priority INT NOT NULL DEFAULT 0,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    monthly_credit_limit DECIMAL(10,2) NOT NULL DEFAULT 25.00,
    credits_used DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    used_percent DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    local_upload_count BIGINT NOT NULL DEFAULT 0,
    local_bytes_used BIGINT NOT NULL DEFAULT 0,
    usage_checked_at DATETIME(6) NULL,
    last_upload_at DATETIME(6) NULL,
    last_error VARCHAR(500) NULL,
    last_error_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cloudinary_accounts_cloud_name (cloud_name),
    KEY idx_cloudinary_accounts_active_priority (is_active, priority),
    KEY idx_cloudinary_accounts_delete (is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE files ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' AFTER content_type;
ALTER TABLE files ADD COLUMN cloudinary_account_id BIGINT NULL AFTER provider;
