-- DB-driven business types (verticals) and catalog terminology.
-- Super-admin can add new verticals and define the labels used by the
-- product/menu module. Restaurant-level columns allow per-tenant overrides.

CREATE TABLE business_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    tab_label VARCHAR(40) NOT NULL,
    item_label VARCHAR(40) NOT NULL,
    item_label_plural VARCHAR(40) NOT NULL,
    category_label VARCHAR(40) NOT NULL,
    category_label_plural VARCHAR(40) NOT NULL,
    icon VARCHAR(40) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_types_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO business_types
    (code, name, description, tab_label, item_label, item_label_plural,
     category_label, category_label_plural, icon, sort_order)
VALUES
    ('RESTAURANT', 'Restaurant / Hotel', 'QR ordering, tables and kitchen',
     'Menu', 'Menu Item', 'Menu Items', 'Category', 'Categories', 'UtensilsCrossed', 10),
    ('ELECTRONICS', 'Electronics store', 'Computers, mobiles and gadgets',
     'Products', 'Product', 'Products', 'Category', 'Categories', 'Smartphone', 20),
    ('VEHICLE', 'Vehicle dealership', 'Two-wheeler and four-wheeler sales',
     'Vehicles', 'Vehicle', 'Vehicles', 'Brand', 'Brands', 'Car', 30),
    ('REAL_ESTATE', 'Builder / Broker', 'Property listings and enquiries',
     'Listings', 'Listing', 'Listings', 'Project', 'Projects', 'Building2', 40);

-- Per-tenant overrides. NULL means "use the business type default".
ALTER TABLE restaurants ADD COLUMN product_tab_label VARCHAR(40) NULL;
ALTER TABLE restaurants ADD COLUMN product_item_label VARCHAR(40) NULL;
ALTER TABLE restaurants ADD COLUMN product_item_label_plural VARCHAR(40) NULL;
ALTER TABLE restaurants ADD COLUMN product_category_label VARCHAR(40) NULL;
ALTER TABLE restaurants ADD COLUMN product_category_label_plural VARCHAR(40) NULL;
