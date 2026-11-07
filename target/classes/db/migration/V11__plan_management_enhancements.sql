ALTER TABLE subscription_plans MODIFY COLUMN max_branches INT NULL DEFAULT NULL;
ALTER TABLE subscription_plans MODIFY COLUMN max_employees INT NULL DEFAULT NULL;
ALTER TABLE subscription_plans MODIFY COLUMN max_menu_items INT NULL DEFAULT NULL;
ALTER TABLE subscription_plans ADD COLUMN business_type VARCHAR(30) NOT NULL DEFAULT 'RESTAURANT' AFTER description;
ALTER TABLE subscription_plans ADD COLUMN price_quarterly DECIMAL(10, 2) NULL AFTER price_monthly;
ALTER TABLE subscription_plans ADD COLUMN price_yearly DECIMAL(10, 2) NULL AFTER price_quarterly;
ALTER TABLE subscription_plans ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER price_yearly;
ALTER TABLE subscription_plans ADD COLUMN trial_days INT NOT NULL DEFAULT 0 AFTER status;
ALTER TABLE subscription_plans ADD COLUMN setup_fee DECIMAL(10, 2) NULL AFTER trial_days;
ALTER TABLE subscription_plans ADD COLUMN tax_percent DECIMAL(5, 2) NULL AFTER setup_fee;
ALTER TABLE subscription_plans ADD COLUMN yearly_discount_percent DECIMAL(5, 2) NULL AFTER tax_percent;
ALTER TABLE subscription_plans ADD COLUMN max_orders_per_month INT NULL AFTER max_menu_items;
ALTER TABLE subscription_plans ADD COLUMN max_storage_gb INT NULL AFTER max_orders_per_month;
ALTER TABLE subscription_plans ADD COLUMN max_admin_users INT NULL AFTER max_storage_gb;
ALTER TABLE subscription_plans ADD COLUMN max_customers INT NULL AFTER max_admin_users;
ALTER TABLE subscription_plans ADD COLUMN archived_at DATETIME(6) NULL AFTER max_customers;
ALTER TABLE subscription_plans ADD KEY idx_plans_business_type (business_type);
ALTER TABLE subscription_plans ADD KEY idx_plans_status (status);

UPDATE subscription_plans
SET price_quarterly = ROUND(price_monthly * 3 * 0.95, 2),
    price_yearly = ROUND(price_monthly * 12 * 0.80, 2),
    yearly_discount_percent = 20
WHERE price_quarterly IS NULL;

CREATE TABLE plan_price_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    old_price_monthly DECIMAL(10, 2) NULL,
    new_price_monthly DECIMAL(10, 2) NULL,
    old_price_quarterly DECIMAL(10, 2) NULL,
    new_price_quarterly DECIMAL(10, 2) NULL,
    old_price_yearly DECIMAL(10, 2) NULL,
    new_price_yearly DECIMAL(10, 2) NULL,
    currency CHAR(3) NOT NULL,
    changed_by VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_plan_price_history_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
