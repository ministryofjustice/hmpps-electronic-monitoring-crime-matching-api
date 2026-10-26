ALTER TABLE hub_manager
    ADD COLUMN occupation VARCHAR(255) NOT NULL DEFAULT 'MoJ EM Hub Manager';

ALTER TABLE hub_manager
    ALTER COLUMN occupation DROP DEFAULT;