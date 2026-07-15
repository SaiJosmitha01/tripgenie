CREATE TABLE IF NOT EXISTS ai_itinerary_generations (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(120) NOT NULL,
    raw_response TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    quality_warnings TEXT NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ai_itinerary_generation_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE INDEX IF NOT EXISTS idx_ai_itinerary_generations_trip_generated
    ON ai_itinerary_generations(trip_id, generated_at DESC);
