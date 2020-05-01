CREATE TABLE tickets (
    id  BIGSERIAL PRIMARY KEY NOT NULL,
    ticket_id UUID UNIQUE NOT NULL,
    uid VARCHAR(60) NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL,
    ticket_type VARCHAR(60) NOT NULL,
    is_anonym BOOLEAN NOT NULL,
    description TEXT,
    assigned_to VARCHAR(50)
);