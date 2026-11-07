-- Resto SaaS baseline schema (MySQL 8.4 / TiDB 8.5 compatible).
-- Conventions:
--   * BIGINT identity primary keys; opaque public tokens for QR/tracking.
--   * Every aggregate carries is_active, is_delete, audit columns.
--   * Soft delete sets is_delete = 1 and is_active = 0.
--   * Uniqueness among live rows is enforced with VIRTUAL generated columns
--     that yield NULL for deleted rows (NULLs do not collide in MySQL/TiDB).

CREATE TABLE tenants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    contact_email VARCHAR(255) NULL,
    contact_phone VARCHAR(30) NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    trial_ends_at DATETIME(6) NULL,
    active_slug VARCHAR(60) GENERATED ALWAYS AS (IF(is_delete = 0, slug, NULL)) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenants_active_slug (active_slug),
    KEY idx_tenants_status (status, is_delete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE subscription_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    price_monthly DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    max_branches INT NOT NULL DEFAULT 1,
    max_employees INT NOT NULL DEFAULT 10,
    max_menu_items INT NOT NULL DEFAULT 100,
    features LONGTEXT NULL,
    is_public BIT(1) NOT NULL DEFAULT b'1',
    sort_order INT NOT NULL DEFAULT 0,
    active_code VARCHAR(40) GENERATED ALWAYS AS (IF(is_delete = 0, code, NULL)) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plans_active_code (active_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TRIALING',
    trial_ends_at DATETIME(6) NULL,
    current_period_start DATETIME(6) NULL,
    current_period_end DATETIME(6) NULL,
    cancel_at_period_end BIT(1) NOT NULL DEFAULT b'0',
    provider VARCHAR(30) NOT NULL DEFAULT 'sandbox',
    provider_subscription_id VARCHAR(120) NULL,
    active_tenant_id BIGINT GENERATED ALWAYS AS (IF(is_delete = 0, tenant_id, NULL)) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subscriptions_active_tenant (active_tenant_id),
    KEY idx_subscriptions_plan (plan_id),
    CONSTRAINT fk_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    system_role BIT(1) NOT NULL DEFAULT b'0',
    active_key VARCHAR(120) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(COALESCE(tenant_id, 0), ':', code), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_active_key (active_key),
    KEY idx_roles_tenant (tenant_id),
    CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    active_code VARCHAR(80) GENERATED ALWAYS AS (IF(is_delete = 0, code, NULL)) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_active_code (active_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified BIT(1) NOT NULL DEFAULT b'0',
    last_login_at DATETIME(6) NULL,
    active_email VARCHAR(255) GENERATED ALWAYS AS (IF(is_delete = 0, email, NULL)) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_active_email (active_email),
    KEY idx_users_tenant (tenant_id),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    replaced_by_id BIGINT NULL,
    user_agent VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_hash (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    actor_id BIGINT NULL,
    actor_email VARCHAR(255) NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(64) NULL,
    before_state LONGTEXT NULL,
    after_state LONGTEXT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    request_id VARCHAR(64) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_audit_tenant_created (tenant_id, created_at),
    KEY idx_audit_entity (entity_type, entity_id),
    KEY idx_audit_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE restaurants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    legal_name VARCHAR(160) NULL,
    description VARCHAR(1000) NULL,
    logo_url VARCHAR(500) NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    tax_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.0000,
    service_charge_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.0000,
    active_code VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', code), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_restaurants_active_code (active_code),
    KEY idx_restaurants_tenant (tenant_id),
    CONSTRAINT fk_restaurants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE branches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200) NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NULL,
    postal_code VARCHAR(20) NULL,
    country VARCHAR(2) NOT NULL DEFAULT 'US',
    phone VARCHAR(30) NULL,
    email VARCHAR(255) NULL,
    latitude DECIMAL(9, 6) NULL,
    longitude DECIMAL(9, 6) NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    opening_hours LONGTEXT NULL,
    is_default BIT(1) NOT NULL DEFAULT b'0',
    active_code VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', code), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_branches_active_code (active_code),
    KEY idx_branches_tenant (tenant_id),
    KEY idx_branches_restaurant (restaurant_id),
    CONSTRAINT fk_branches_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_branches_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    employee_code VARCHAR(40) NOT NULL,
    job_title VARCHAR(80) NULL,
    hired_at DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    active_code VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', employee_code), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_active_code (active_code),
    KEY idx_employees_tenant (tenant_id),
    KEY idx_employees_branch (branch_id),
    CONSTRAINT fk_employees_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_employees_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE menu_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active_name VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', restaurant_id, ':', name), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_categories_active_name (active_name),
    KEY idx_menu_categories_tenant (tenant_id),
    CONSTRAINT fk_menu_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_menu_categories_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE menu_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    sku VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    price DECIMAL(12, 2) NOT NULL,
    cost DECIMAL(12, 2) NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    image_url VARCHAR(500) NULL,
    is_available BIT(1) NOT NULL DEFAULT b'1',
    is_featured BIT(1) NOT NULL DEFAULT b'0',
    prep_time_minutes INT NULL,
    calories INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active_sku VARCHAR(100) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', sku), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_items_active_sku (active_sku),
    KEY idx_menu_items_tenant (tenant_id),
    KEY idx_menu_items_category (category_id),
    CONSTRAINT fk_menu_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_menu_items_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT fk_menu_items_category FOREIGN KEY (category_id) REFERENCES menu_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE menu_item_variants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    price_delta DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_default BIT(1) NOT NULL DEFAULT b'0',
    sort_order INT NOT NULL DEFAULT 0,
    active_name VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(menu_item_id, ':', name), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_variants_active_name (active_name),
    KEY idx_variants_item (menu_item_id),
    CONSTRAINT fk_variants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_variants_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE modifier_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    selection_type VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    min_selections INT NOT NULL DEFAULT 0,
    max_selections INT NOT NULL DEFAULT 1,
    is_required BIT(1) NOT NULL DEFAULT b'0',
    sort_order INT NOT NULL DEFAULT 0,
    active_name VARCHAR(120) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', restaurant_id, ':', name), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_modifier_groups_active_name (active_name),
    CONSTRAINT fk_modifier_groups_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_modifier_groups_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE modifiers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    modifier_group_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price_delta DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_default BIT(1) NOT NULL DEFAULT b'0',
    sort_order INT NOT NULL DEFAULT 0,
    active_name VARCHAR(120) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(modifier_group_id, ':', name), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_modifiers_active_name (active_name),
    CONSTRAINT fk_modifiers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_modifiers_group FOREIGN KEY (modifier_group_id) REFERENCES modifier_groups (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE menu_item_modifier_groups (
    menu_item_id BIGINT NOT NULL,
    modifier_group_id BIGINT NOT NULL,
    PRIMARY KEY (menu_item_id, modifier_group_id),
    CONSTRAINT fk_item_mod_groups_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_mod_groups_group FOREIGN KEY (modifier_group_id) REFERENCES modifier_groups (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE branch_menu_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    is_available BIT(1) NOT NULL DEFAULT b'1',
    price_override DECIMAL(12, 2) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_branch_menu_item (branch_id, menu_item_id),
    CONSTRAINT fk_branch_menu_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_branch_menu_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_branch_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dining_sections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active_name VARCHAR(100) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(branch_id, ':', name), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sections_active_name (active_name),
    CONSTRAINT fk_sections_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_sections_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dining_tables (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    section_id BIGINT NULL,
    table_number VARCHAR(20) NOT NULL,
    name VARCHAR(80) NULL,
    capacity INT NOT NULL DEFAULT 2,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    qr_token CHAR(36) NOT NULL,
    qr_rotated_at DATETIME(6) NULL,
    active_number VARCHAR(40) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(branch_id, ':', table_number), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tables_qr_token (qr_token),
    UNIQUE KEY uk_tables_active_number (active_number),
    KEY idx_tables_branch (branch_id),
    CONSTRAINT fk_tables_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tables_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_tables_section FOREIGN KEY (section_id) REFERENCES dining_sections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    email VARCHAR(255) NULL,
    first_name VARCHAR(80) NULL,
    last_name VARCHAR(80) NULL,
    phone VARCHAR(30) NULL,
    marketing_opt_in BIT(1) NOT NULL DEFAULT b'0',
    active_email VARCHAR(255) GENERATED ALWAYS AS (
        IF(is_delete = 0 AND email IS NOT NULL, CONCAT(tenant_id, ':', email), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customers_active_email (active_email),
    KEY idx_customers_tenant (tenant_id),
    CONSTRAINT fk_customers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guest_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    table_id BIGINT NULL,
    session_token CHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    last_seen_at DATETIME(6) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guest_sessions_token (session_token),
    KEY idx_guest_sessions_table (table_id),
    CONSTRAINT fk_guest_sessions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_guest_sessions_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_guest_sessions_table FOREIGN KEY (table_id) REFERENCES dining_tables (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    table_id BIGINT NULL,
    customer_id BIGINT NULL,
    guest_session_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    service_charge DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tip_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    grand_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_carts_session (guest_session_id, status),
    KEY idx_carts_tenant_branch (tenant_id, branch_id, status),
    CONSTRAINT fk_carts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_carts_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_carts_table FOREIGN KEY (table_id) REFERENCES dining_tables (id),
    CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_carts_guest FOREIGN KEY (guest_session_id) REFERENCES guest_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    cart_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12, 2) NOT NULL,
    modifier_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(12, 2) NOT NULL,
    notes VARCHAR(500) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_cart_items_cart (cart_id),
    CONSTRAINT fk_cart_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES menu_item_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    table_id BIGINT NULL,
    customer_id BIGINT NULL,
    guest_session_id BIGINT NULL,
    order_number VARCHAR(30) NOT NULL,
    public_token CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source VARCHAR(20) NOT NULL DEFAULT 'QR',
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    service_charge DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tip_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    grand_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    customer_name VARCHAR(120) NULL,
    customer_email VARCHAR(255) NULL,
    customer_phone VARCHAR(30) NULL,
    notes VARCHAR(500) NULL,
    placed_at DATETIME(6) NULL,
    accepted_at DATETIME(6) NULL,
    ready_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    cancel_reason VARCHAR(300) NULL,
    active_number VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', branch_id, ':', order_number), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_public_token (public_token),
    UNIQUE KEY uk_orders_active_number (active_number),
    KEY idx_orders_branch_status (branch_id, status, created_at),
    KEY idx_orders_tenant_created (tenant_id, created_at),
    CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES dining_tables (id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_orders_guest FOREIGN KEY (guest_session_id) REFERENCES guest_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NULL,
    variant_id BIGINT NULL,
    name_snapshot VARCHAR(120) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    quantity INT NOT NULL,
    modifier_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(12, 2) NOT NULL,
    notes VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    CONSTRAINT fk_order_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES menu_item_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_item_modifiers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    modifier_id BIGINT NULL,
    name_snapshot VARCHAR(100) NOT NULL,
    price_delta DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_order_item_mods_item (order_item_id),
    CONSTRAINT fk_order_item_mods_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_order_item_mods_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_mods_modifier FOREIGN KEY (modifier_id) REFERENCES modifiers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100) NULL,
    notes VARCHAR(300) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_order_history_order (order_id, created_at),
    CONSTRAINT fk_order_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_payment_id VARCHAR(120) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    payment_method VARCHAR(30) NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    captured_at DATETIME(6) NULL,
    refunded_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    failure_reason VARCHAR(300) NULL,
    raw_response LONGTEXT NULL,
    active_provider_payment VARCHAR(150) GENERATED ALWAYS AS (
        IF(is_delete = 0 AND provider_payment_id IS NOT NULL, CONCAT(provider, ':', provider_payment_id), NULL)
    ) VIRTUAL,
    active_idempotency_key VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, idempotency_key, NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_provider_payment (active_provider_payment),
    UNIQUE KEY uk_payments_idempotency (active_idempotency_key),
    KEY idx_payments_order (order_id),
    CONSTRAINT fk_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    provider_refund_id VARCHAR(120) NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(300) NULL,
    processed_at DATETIME(6) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_refunds_payment (payment_id),
    CONSTRAINT fk_refunds_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payment_webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(30) NOT NULL,
    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload LONGTEXT NOT NULL,
    signature_valid BIT(1) NOT NULL DEFAULT b'0',
    processed_at DATETIME(6) NULL,
    error_message VARCHAR(500) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_webhook_provider_event (provider, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    user_id BIGINT NULL,
    channel VARCHAR(20) NOT NULL,
    template_code VARCHAR(80) NOT NULL,
    subject VARCHAR(200) NULL,
    body LONGTEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at DATETIME(6) NULL,
    read_at DATETIME(6) NULL,
    metadata LONGTEXT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_user (user_id, read_at),
    KEY idx_notifications_tenant (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    storage_key VARCHAR(300) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    public_url VARCHAR(500) NULL,
    uploaded_by BIGINT NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_files_storage_key (storage_key),
    KEY idx_files_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE subscription_invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    due_at DATETIME(6) NULL,
    paid_at DATETIME(6) NULL,
    provider_invoice_id VARCHAR(120) NULL,
    active_number VARCHAR(80) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', invoice_number), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoices_active_number (active_number),
    KEY idx_invoices_subscription (subscription_id),
    CONSTRAINT fk_invoices_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
