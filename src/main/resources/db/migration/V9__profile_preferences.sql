-- Profile page foundation: restaurant profile fields, user profile fields,
-- and per-user preferences. TiDB requires one ADD COLUMN per statement.

ALTER TABLE restaurants ADD COLUMN cover_image_url VARCHAR(500) NULL AFTER logo_url;
ALTER TABLE restaurants ADD COLUMN gallery LONGTEXT NULL AFTER cover_image_url;
ALTER TABLE restaurants ADD COLUMN brand_colors VARCHAR(500) NULL AFTER gallery;
ALTER TABLE restaurants ADD COLUMN contact_phone VARCHAR(30) NULL AFTER contact_email;
ALTER TABLE restaurants ADD COLUMN website VARCHAR(255) NULL AFTER contact_phone;
ALTER TABLE restaurants ADD COLUMN address_line VARCHAR(255) NULL AFTER website;
ALTER TABLE restaurants ADD COLUMN city VARCHAR(100) NULL AFTER address_line;
ALTER TABLE restaurants ADD COLUMN state VARCHAR(100) NULL AFTER city;
ALTER TABLE restaurants ADD COLUMN country VARCHAR(100) NULL AFTER state;
ALTER TABLE restaurants ADD COLUMN postal_code VARCHAR(20) NULL AFTER country;
ALTER TABLE restaurants ADD COLUMN business_registration_number VARCHAR(100) NULL AFTER postal_code;
ALTER TABLE restaurants ADD COLUMN tax_number VARCHAR(100) NULL AFTER business_registration_number;
ALTER TABLE restaurants ADD COLUMN default_language VARCHAR(10) NOT NULL DEFAULT 'en' AFTER tax_number;

ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL AFTER phone;
ALTER TABLE users ADD COLUMN display_name VARCHAR(80) NULL AFTER avatar_url;
ALTER TABLE users ADD COLUMN bio VARCHAR(500) NULL AFTER display_name;

CREATE TABLE user_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    theme VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
    date_format VARCHAR(20) NOT NULL DEFAULT 'DD/MM/YYYY',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    email_notifications BIT(1) NOT NULL DEFAULT b'1',
    in_app_notifications BIT(1) NOT NULL DEFAULT b'1',
    is_active BIT(1) NOT NULL DEFAULT b'1',
    is_delete BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_preferences_user (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)
);
