-- Product catalog support.
-- Removes the rejected DB-driven business-type configuration while keeping
-- the already-applied V17 migration checksum valid for Flyway.

ALTER TABLE restaurants
    DROP COLUMN product_tab_label,
    DROP COLUMN product_item_label,
    DROP COLUMN product_item_label_plural,
    DROP COLUMN product_category_label,
    DROP COLUMN product_category_label_plural;

DROP TABLE business_types;

CREATE TABLE menu_item_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    image_label VARCHAR(60) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_menu_item_images_item (tenant_id, menu_item_id, is_delete, sort_order),
    CONSTRAINT fk_menu_item_images_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_menu_item_images_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO permissions (code, name, description) VALUES
    ('product.read', 'Read products', 'View the product catalog'),
    ('product.manage', 'Manage products', 'Create and edit products and product images'),
    ('product_order.read', 'Read product orders', 'View product orders'),
    ('product_order.manage', 'Manage product orders', 'Update product order status');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND r.tenant_id IS NULL
  AND r.is_active = 1
  AND r.is_delete = 0
  AND p.code IN ('product.read', 'product.manage', 'product_order.read', 'product_order.manage');
