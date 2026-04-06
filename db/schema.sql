-- Hotel Management (H2). If you had the old billing schema, delete folder ./data/ once.

CREATE TABLE IF NOT EXISTS guest (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS room (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_number VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    nightly_rate DECIMAL(12, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT uq_room_number UNIQUE (room_number)
);

CREATE TABLE IF NOT EXISTS hotel_service (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT uq_service_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS stay (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    guest_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_out TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'CHECKED_IN',
    CONSTRAINT fk_stay_guest FOREIGN KEY (guest_id) REFERENCES guest (id),
    CONSTRAINT fk_stay_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE TABLE IF NOT EXISTS bill (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stay_id BIGINT NOT NULL,
    bill_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(14, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(32) NOT NULL DEFAULT 'CASH',
    CONSTRAINT fk_bill_stay FOREIGN KEY (stay_id) REFERENCES stay (id)
);

CREATE TABLE IF NOT EXISTS bill_line (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    hotel_service_id BIGINT NOT NULL,
    qty INT NOT NULL,
    line_total DECIMAL(14, 2) NOT NULL,
    CONSTRAINT fk_line_bill FOREIGN KEY (bill_id) REFERENCES bill (id) ON DELETE CASCADE,
    CONSTRAINT fk_line_service FOREIGN KEY (hotel_service_id) REFERENCES hotel_service (id)
);

CREATE INDEX IF NOT EXISTS idx_stay_status ON stay (status);
CREATE INDEX IF NOT EXISTS idx_room_status ON room (status);
CREATE INDEX IF NOT EXISTS idx_bill_stay ON bill (stay_id);

-- Default extras: ROOM = auto nights x nightly_rate (catalog price ignored on that line)
INSERT INTO hotel_service (code, name, unit_price)
SELECT 'ROOM', 'Room stay (per night)', 0 FROM (VALUES (1)) AS d(x)
WHERE NOT EXISTS (SELECT 1 FROM hotel_service s WHERE s.code = 'ROOM');
INSERT INTO hotel_service (code, name, unit_price)
SELECT 'LAUNDRY', 'Laundry', 150.00 FROM (VALUES (1)) AS d(x)
WHERE NOT EXISTS (SELECT 1 FROM hotel_service s WHERE s.code = 'LAUNDRY');
INSERT INTO hotel_service (code, name, unit_price)
SELECT 'MINIBAR', 'Minibar package', 350.00 FROM (VALUES (1)) AS d(x)
WHERE NOT EXISTS (SELECT 1 FROM hotel_service s WHERE s.code = 'MINIBAR');
INSERT INTO hotel_service (code, name, unit_price)
SELECT 'BREAKFAST', 'Breakfast buffet', 200.00 FROM (VALUES (1)) AS d(x)
WHERE NOT EXISTS (SELECT 1 FROM hotel_service s WHERE s.code = 'BREAKFAST');
