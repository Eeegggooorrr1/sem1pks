ALTER TABLE orders DROP CONSTRAINT orders_listing_id_fkey, DROP CONSTRAINT orders_buyer_id_fkey;
ALTER TABLE listings DROP CONSTRAINT listings_user_id_fkey, DROP CONSTRAINT fk_listing_category;
ALTER TABLE listing_notes DROP CONSTRAINT listing_notes_listing_id_fkey, DROP CONSTRAINT listing_notes_author_id_fkey;
ALTER TABLE listing_tags DROP CONSTRAINT listing_tags_listing_id_fkey, DROP CONSTRAINT listing_tags_pkey;
ALTER TABLE refresh_sessions DROP CONSTRAINT refresh_sessions_user_id_fkey;

ALTER TABLE users ALTER COLUMN id DROP IDENTITY;
ALTER TABLE users ALTER COLUMN id TYPE UUID USING md5('users:' || id::text)::uuid;
ALTER TABLE users ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE orders ALTER COLUMN id DROP IDENTITY;
ALTER TABLE orders ALTER COLUMN id TYPE UUID USING md5('orders:' || id::text)::uuid;
ALTER TABLE orders ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE listings DISABLE TRIGGER listings_updated;
ALTER TABLE listings ALTER COLUMN user_id TYPE UUID USING md5('users:' || user_id::text)::uuid;
ALTER TABLE orders ALTER COLUMN buyer_id TYPE UUID USING md5('users:' || buyer_id::text)::uuid;
ALTER TABLE listing_notes ALTER COLUMN author_id TYPE UUID USING md5('users:' || author_id::text)::uuid;
ALTER TABLE refresh_sessions ALTER COLUMN user_id TYPE UUID USING md5('users:' || user_id::text)::uuid;
ALTER TABLE audit_events ALTER COLUMN user_id TYPE UUID USING md5('users:' || user_id::text)::uuid;
ALTER TABLE audit_events ALTER COLUMN entity_id TYPE UUID USING (
    CASE
        WHEN entity_id ~ '^[0-9]+$' AND entity_type IN ('USER', 'ORDER')
            THEN md5(CASE entity_type WHEN 'USER' THEN 'users:' ELSE 'orders:' END || entity_id)::uuid
        ELSE entity_id::uuid
    END
);

ALTER TABLE orders ADD COLUMN listing_uuid UUID;
ALTER TABLE listing_tags ADD COLUMN listing_uuid UUID;
ALTER TABLE listing_notes ADD COLUMN listing_uuid UUID;
ALTER TABLE listings ADD COLUMN category_id UUID;
UPDATE orders o SET listing_uuid=l.uuid FROM listings l WHERE l.id=o.listing_id;
UPDATE listing_tags t SET listing_uuid=l.uuid FROM listings l WHERE l.id=t.listing_id;
UPDATE listing_notes n SET listing_uuid=l.uuid FROM listings l WHERE l.id=n.listing_id;
UPDATE listings l SET category_id=c.id FROM categories c WHERE c.code=l.category;

ALTER TABLE orders DROP COLUMN listing_id;
ALTER TABLE listing_tags DROP COLUMN listing_id;
ALTER TABLE listing_notes DROP COLUMN listing_id;
ALTER TABLE listings DROP COLUMN id, DROP COLUMN category, DROP CONSTRAINT listings_uuid_key;
ALTER TABLE listings RENAME COLUMN uuid TO id;
ALTER TABLE orders RENAME COLUMN listing_uuid TO listing_id;
ALTER TABLE listing_tags RENAME COLUMN listing_uuid TO listing_id;
ALTER TABLE listing_notes RENAME COLUMN listing_uuid TO listing_id;

ALTER TABLE listings ADD PRIMARY KEY (id),
    ADD CONSTRAINT listings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id),
    ALTER COLUMN category_id SET NOT NULL,
    ADD CONSTRAINT fk_listing_category FOREIGN KEY (category_id) REFERENCES categories(id);
ALTER TABLE listings ENABLE TRIGGER listings_updated;
ALTER TABLE orders ALTER COLUMN listing_id SET NOT NULL,
    ADD CONSTRAINT orders_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    ADD CONSTRAINT orders_buyer_id_fkey FOREIGN KEY (buyer_id) REFERENCES users(id);
ALTER TABLE listing_tags ADD PRIMARY KEY (listing_id,tag),
    ADD CONSTRAINT listing_tags_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE;
ALTER TABLE listing_notes ALTER COLUMN listing_id SET NOT NULL,
    ADD CONSTRAINT listing_notes_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    ADD CONSTRAINT listing_notes_author_id_fkey FOREIGN KEY (author_id) REFERENCES users(id);
ALTER TABLE refresh_sessions ADD CONSTRAINT refresh_sessions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_orders_listing ON orders(listing_id);
CREATE INDEX idx_notes_listing ON listing_notes(listing_id,created_at);
