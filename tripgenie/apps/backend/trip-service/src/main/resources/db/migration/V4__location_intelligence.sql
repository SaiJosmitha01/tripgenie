ALTER TABLE itinerary_items
    ADD COLUMN formatted_address VARCHAR(500),
    ADD COLUMN latitude NUMERIC(10, 7),
    ADD COLUMN longitude NUMERIC(10, 7),
    ADD COLUMN google_place_id VARCHAR(160),
    ADD COLUMN place_rating NUMERIC(3, 2);

CREATE INDEX idx_itinerary_items_google_place_id ON itinerary_items(google_place_id);
