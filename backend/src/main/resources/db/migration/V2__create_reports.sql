CREATE TABLE reports (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    original_filename   VARCHAR(255) NOT NULL,
    stored_filename     VARCHAR(255) NOT NULL,
    file_path           VARCHAR(500) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    file_size           BIGINT NOT NULL,
    status              VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reports_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_reports_stored_filename ON reports (stored_filename);

-- Nearly every future report query is WHERE user_id = authenticatedUserId,
-- so this index is not optional.
CREATE INDEX idx_reports_user_id ON reports (user_id);