CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO categories(code,name)
SELECT code,code FROM unnest(ARRAY['ELECTRONICS','REAL_ESTATE','TRANSPORT','FURNITURE','CLOTHING',
    'SPORTS','BOOKS','BEAUTY','SERVICES','JOBS','PETS','GARDEN','CONSTRUCTION','CHILDREN','OTHER']) AS code;
INSERT INTO categories(code,name) SELECT DISTINCT category,category FROM listings ON CONFLICT DO NOTHING;
ALTER TABLE listings ADD CONSTRAINT fk_listing_category FOREIGN KEY (category) REFERENCES categories(code);
CREATE SEQUENCE listing_code_seq;
ALTER TABLE orders DROP CONSTRAINT orders_listing_id_fkey;
ALTER TABLE orders ADD CONSTRAINT orders_listing_id_fkey FOREIGN KEY(listing_id) REFERENCES listings(id) ON DELETE CASCADE;
ALTER TABLE listings ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE listings ADD COLUMN code VARCHAR(64) NOT NULL DEFAULT ('L-' || nextval('listing_code_seq')) UNIQUE;
ALTER TABLE listings ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE listings SET updated_at=created_at;
CREATE FUNCTION update_listing_timestamp() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at=CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;
CREATE TRIGGER listings_updated BEFORE UPDATE ON listings FOR EACH ROW EXECUTE FUNCTION update_listing_timestamp();
CREATE TABLE listing_tags (
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    tag VARCHAR(40) NOT NULL,
    PRIMARY KEY (listing_id,tag)
);
CREATE TABLE listing_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id),
    body VARCHAR(2000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notes_listing ON listing_notes(listing_id,created_at);
CREATE INDEX idx_tags_tag ON listing_tags(tag);
CREATE INDEX idx_listings_archive_created ON listings(archived,created_at DESC);
CREATE TABLE import_jobs (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('NEW_ONLY','UPSERT')),
    content TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    applied BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE import_job_rows (
    job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL,
    existing_uuid UUID,
    existing_version BIGINT,
    PRIMARY KEY(job_id,code)
);
