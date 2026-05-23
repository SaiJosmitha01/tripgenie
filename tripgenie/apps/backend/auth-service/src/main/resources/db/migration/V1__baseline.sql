CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30),
    timezone VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    home_airport VARCHAR(50),
    preferred_currency VARCHAR(50),
    default_trip_length_days INTEGER CHECK (default_trip_length_days IS NULL OR default_trip_length_days BETWEEN 0 AND 365),
    daily_budget NUMERIC(12, 2),
    travel_styles TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    dietary_restrictions TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    accessibility_needs TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'ROLE_USER'),
       ('00000000-0000-0000-0000-000000000002', 'ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;
