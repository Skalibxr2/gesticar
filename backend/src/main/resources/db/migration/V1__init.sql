CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    rut VARCHAR(30) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(150)
);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(120) NOT NULL,
    year INTEGER,
    customer_id BIGINT REFERENCES customers(id)
);

CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    customer_id BIGINT REFERENCES customers(id),
    vehicle_id BIGINT REFERENCES vehicles(id)
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    details TEXT,
    estimated_hours INTEGER,
    work_order_id BIGINT REFERENCES work_orders(id)
);

CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(12,2) NOT NULL,
    approved BOOLEAN DEFAULT FALSE,
    notes TEXT,
    work_order_id BIGINT REFERENCES work_orders(id)
);

INSERT INTO customers (rut, first_name, last_name, phone, email) VALUES
('12.345.678-9', 'Ana', 'Carvallo', '+56911111111', 'ana@example.com'),
('98.765.432-1', 'Luis', 'Farias', '+56922222222', 'luis@example.com');

INSERT INTO vehicles (license_plate, brand, model, year, customer_id) VALUES
('AA-BB-11', 'Toyota', 'Corolla', 2019, 1),
('CC-DD-22', 'Nissan', 'Versa', 2021, 2);

INSERT INTO work_orders (code, description, status, customer_id, vehicle_id) VALUES
('OT-001', 'Cambio de aceite y filtros', 'BORRADOR', 1, 1),
('OT-002', 'Revisión de frenos', 'INICIADA', 2, 2);

INSERT INTO tasks (title, details, estimated_hours, work_order_id) VALUES
('Reemplazo de filtro de aire', 'Usar filtro OEM', 1, 1),
('Cambio de pastillas', 'Delanteras y traseras', 3, 2);

INSERT INTO budgets (amount, approved, notes, work_order_id) VALUES
(95000, false, 'Pendiente de aprobación', 1),
(180000, true, 'Cliente aprobó por teléfono', 2);
