CREATE TABLE trips (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    destination VARCHAR(160) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_trips_date_range CHECK (end_date >= start_date)
);

CREATE INDEX idx_trips_owner_created_at ON trips(owner_id, created_at DESC);
CREATE INDEX idx_trips_owner_status ON trips(owner_id, status);
CREATE INDEX idx_trips_owner_start_date ON trips(owner_id, start_date);

CREATE TABLE itinerary_days (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL,
    date DATE NOT NULL,
    title VARCHAR(160),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_itinerary_days_trip_day UNIQUE (trip_id, day_number),
    CONSTRAINT ck_itinerary_days_day_number CHECK (day_number > 0)
);

CREATE INDEX idx_itinerary_days_trip_id ON itinerary_days(trip_id);

CREATE TABLE itinerary_items (
    id UUID PRIMARY KEY,
    itinerary_day_id UUID NOT NULL REFERENCES itinerary_days(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    location VARCHAR(180),
    start_time TIME,
    end_time TIME,
    estimated_cost NUMERIC(12, 2),
    booking_reference VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_itinerary_items_day_position UNIQUE (itinerary_day_id, position),
    CONSTRAINT ck_itinerary_items_position CHECK (position > 0),
    CONSTRAINT ck_itinerary_items_cost CHECK (estimated_cost IS NULL OR estimated_cost >= 0),
    CONSTRAINT ck_itinerary_items_time_range CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)
);

CREATE INDEX idx_itinerary_items_day_id ON itinerary_items(itinerary_day_id);

CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL UNIQUE REFERENCES trips(id) ON DELETE CASCADE,
    currency VARCHAR(3) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_budgets_total_amount CHECK (total_amount >= 0)
);

CREATE TABLE budget_categories (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_budget_categories_budget_name UNIQUE (budget_id, name),
    CONSTRAINT ck_budget_categories_amount CHECK (amount >= 0)
);

CREATE INDEX idx_budget_categories_budget_id ON budget_categories(budget_id);
