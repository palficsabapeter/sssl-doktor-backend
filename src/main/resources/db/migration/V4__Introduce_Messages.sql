CREATE TABLE messages(
    id  BIGSERIAL PRIMARY KEY NOT NULL,
    message_id UUID UNIQUE NOT NULL,
    ticket_id UUID NOT NULL,
    uid VARCHAR(60) NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    text TEXT NOT NULL,
    reviewed_by_uid VARCHAR(60),
    reviewed_by VARCHAR(200),
    reviewed_at BIGINT,
    FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id)
);