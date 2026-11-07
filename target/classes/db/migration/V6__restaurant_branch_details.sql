-- Restaurant profile details: cuisine, contact email, social links, operating hours
ALTER TABLE restaurants
    ADD COLUMN cuisine VARCHAR(200) NULL;

ALTER TABLE restaurants
    ADD COLUMN contact_email VARCHAR(255) NULL;

ALTER TABLE restaurants
    ADD COLUMN social_links LONGTEXT NULL;

ALTER TABLE restaurants
    ADD COLUMN operating_hours VARCHAR(300) NULL;

-- Branch enhancements: manager assignment + per-branch currency override
ALTER TABLE branches
    ADD COLUMN manager_id BIGINT NULL;

ALTER TABLE branches
    ADD KEY idx_branches_manager (manager_id);

ALTER TABLE branches
    ADD COLUMN currency CHAR(3) NULL;
