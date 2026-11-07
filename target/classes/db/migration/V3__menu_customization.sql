ALTER TABLE cart_items ADD COLUMN modifier_selections LONGTEXT NULL;
ALTER TABLE menu_items ADD COLUMN is_vegetarian BIT(1) NOT NULL DEFAULT b'0';
