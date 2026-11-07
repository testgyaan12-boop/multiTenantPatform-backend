CREATE TABLE coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(60) NOT NULL,
    description VARCHAR(300) NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    starts_at DATETIME(6) NULL,
    ends_at DATETIME(6) NULL,
    max_redemptions INT NULL,
    redeemed_count INT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_coupons_code (code),
    KEY idx_coupons_tenant (tenant_id)
);

CREATE TABLE platform_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(80) NOT NULL,
    setting_value TEXT NULL,
    description VARCHAR(300) NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_settings_key (setting_key)
);

CREATE TABLE feature_flags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    flag_key VARCHAR(80) NOT NULL,
    description VARCHAR(300) NULL,
    enabled BIT(1) NOT NULL DEFAULT b'0',
    plan_code VARCHAR(40) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_flags_key (flag_key)
);

INSERT INTO platform_settings (setting_key, setting_value, description, category) VALUES
  ('signup.policy', 'APPROVAL', 'How restaurant sign-ups are handled: OPEN or APPROVAL', 'SIGNUP'),
  ('default.plan', 'STARTER', 'Plan assigned to new tenants by default', 'GENERAL'),
  ('billing.provider', 'sandbox', 'Payment/billing provider mode', 'INTEGRATION'),
  ('mail.provider', 'none', 'Mail provider: none disables invitation emails', 'INTEGRATION'),
  ('support.email', 'support@resto-saas.local', 'Support contact shown to restaurants', 'GENERAL');

INSERT INTO feature_flags (flag_key, description, enabled, plan_code) VALUES
  ('new_restaurant_signup', 'Allow self-service restaurant registration', b'1', NULL),
  ('email_invitations', 'Send invitation emails when owners are created', b'0', NULL),
  ('coupon_engine', 'Enable coupon redemption at checkout', b'1', NULL),
  ('reports_beta', 'Expose beta analytics widgets', b'0', NULL);

INSERT INTO coupons (code, description, discount_type, discount_value, starts_at, ends_at, max_redemptions, redeemed_count) VALUES
  ('WELCOME10', '10% off the first month for new restaurants', 'PERCENT', 10.00, '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000', 1000, 0),
  ('LAUNCH25', '25.00 off annual plan upgrades', 'FIXED', 25.00, '2026-01-01 00:00:00.000000', '2026-12-31 23:59:59.000000', 500, 0);
