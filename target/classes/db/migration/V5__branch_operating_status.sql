-- Branch operating status: allow pausing a branch without soft-deleting it.

ALTER TABLE branches
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Existing branches keep operating; only explicitly paused ones flip later.
UPDATE branches SET status = 'ACTIVE' WHERE status = '' OR status IS NULL;
