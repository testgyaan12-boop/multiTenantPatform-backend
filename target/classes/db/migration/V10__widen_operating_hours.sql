-- Weekly operating-hours JSON no longer fits VARCHAR(300).
ALTER TABLE restaurants MODIFY COLUMN operating_hours LONGTEXT NULL;
