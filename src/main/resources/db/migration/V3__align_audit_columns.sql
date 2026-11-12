-- Align audit columns with the service convention:
--   CREATED_BY / CREATED_TIME (+ UPDATED_BY / UPDATED_TIME / VERSION on mutable tables).
-- Renames keep existing data; V1/V2 stay untouched (Flyway checksums).

-- mutable
ALTER TABLE sdui_merchant ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_merchant ALTER COLUMN updated_at RENAME TO updated_time;
ALTER TABLE sdui_merchant ADD COLUMN created_by VARCHAR(128) DEFAULT 'system' NOT NULL;
ALTER TABLE sdui_merchant ADD COLUMN updated_by VARCHAR(128) DEFAULT 'system' NOT NULL;
ALTER TABLE sdui_merchant ADD COLUMN version INT DEFAULT 0 NOT NULL;

ALTER TABLE sdui_page ALTER COLUMN lock_version RENAME TO version;
ALTER TABLE sdui_page ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_page ALTER COLUMN updated_at RENAME TO updated_time;
ALTER TABLE sdui_page ADD COLUMN updated_by VARCHAR(128) DEFAULT 'system' NOT NULL;

ALTER TABLE sdui_component_type ALTER COLUMN updated_at RENAME TO updated_time;
ALTER TABLE sdui_component_type ADD COLUMN created_by VARCHAR(128) DEFAULT 'system' NOT NULL;
ALTER TABLE sdui_component_type ADD COLUMN created_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE sdui_component_type ADD COLUMN updated_by VARCHAR(128) DEFAULT 'system' NOT NULL;
ALTER TABLE sdui_component_type ADD COLUMN version INT DEFAULT 0 NOT NULL;

ALTER TABLE sdui_behavior ADD COLUMN created_by VARCHAR(128) DEFAULT 'system' NOT NULL;
ALTER TABLE sdui_behavior ADD COLUMN created_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE sdui_behavior ADD COLUMN updated_by VARCHAR(128) DEFAULT 'system' NOT NULL;
ALTER TABLE sdui_behavior ADD COLUMN updated_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE sdui_behavior ADD COLUMN version INT DEFAULT 0 NOT NULL;

-- immutable (insert-only): created_* only
ALTER TABLE sdui_bounds ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_bounds ADD COLUMN created_by VARCHAR(128) DEFAULT 'system' NOT NULL;
ALTER TABLE sdui_policy ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_style ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_tree ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_script ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_signing_key ALTER COLUMN created_at RENAME TO created_time;
ALTER TABLE sdui_signing_key ADD COLUMN created_by VARCHAR(128) DEFAULT 'system' NOT NULL;

-- audit log: the actor is the creator of the entry
ALTER TABLE sdui_audit_log ALTER COLUMN actor RENAME TO created_by;
ALTER TABLE sdui_audit_log ALTER COLUMN at RENAME TO created_time;
