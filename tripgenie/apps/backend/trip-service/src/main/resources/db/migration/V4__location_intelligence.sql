ALTER TABLE itinerary_items
    ADD COLUMN IF NOT EXISTS formatted_address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS google_place_id VARCHAR(160),
    ADD COLUMN IF NOT EXISTS place_rating NUMERIC(3, 2);

CREATE INDEX IF NOT EXISTS idx_itinerary_items_google_place_id ON itinerary_items(google_place_id);
