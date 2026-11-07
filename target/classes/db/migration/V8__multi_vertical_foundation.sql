-- Multi-vertical foundation: tenants can be RESTAURANT | ELECTRONICS | VEHICLE | REAL_ESTATE.
-- Storefronts stay slug-based; vertical-specific product specs live in item_attributes;
-- storefront leads live in inquiries; menu_items gains optional stock tracking.

-- TiDB rejects ADD COLUMN + ADD INDEX referencing the new column in one ALTER,
-- so column and index changes are applied as separate statements.

ALTER TABLE tenants
    ADD COLUMN business_type VARCHAR(30) NOT NULL DEFAULT 'RESTAURANT';

ALTER TABLE tenants
    ADD INDEX idx_tenants_business_type (business_type);

CREATE TABLE item_attributes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    attr_key VARCHAR(60) NOT NULL,
    attr_value VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active_attr VARCHAR(140) GENERATED ALWAYS AS (
        IF(is_delete = 0, CONCAT(tenant_id, ':', menu_item_id, ':', attr_key), NULL)
    ) VIRTUAL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_attributes_active (active_attr),
    KEY idx_item_attributes_tenant_item (tenant_id, menu_item_id),
    CONSTRAINT fk_item_attributes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_item_attributes_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE menu_items
    ADD COLUMN stock_qty INT NULL COMMENT 'NULL = not tracked';

CREATE TABLE inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    menu_item_id BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(255) NULL,
    message VARCHAR(2000) NULL,
    source_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    booking_payment_status VARCHAR(20) NULL,
    booking_amount DECIMAL(10, 2) NULL,
    order_id BIGINT NULL,
    notes VARCHAR(1000) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_inquiries_tenant_status (tenant_id, status),
    KEY idx_inquiries_order (order_id),
    CONSTRAINT fk_inquiries_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO permissions (code, name, description) VALUES
    ('inquiry.read', 'Read enquiries', 'View storefront enquiries and leads'),
    ('inquiry.manage', 'Manage enquiries', 'Update enquiry status and booking details');

INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id
    FROM roles r
    JOIN permissions p ON p.code IN ('inquiry.read', 'inquiry.manage')
    WHERE r.code IN ('TENANT_OWNER', 'TENANT_MANAGER');
