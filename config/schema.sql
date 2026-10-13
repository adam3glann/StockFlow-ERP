-- StockFlow ERP PostgreSQL bootstrap schema.
-- Run once against the database configured in config/database.properties.

CREATE TABLE IF NOT EXISTS employees (
    employee_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(512) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'CASHIER', 'STOREKEEPER')),
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(254),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Branch identity belongs to this branch's database so every connected client
-- displays the same branch label. Each branch server uses its own database.
CREATE TABLE IF NOT EXISTS branch_settings (
    singleton_id INTEGER PRIMARY KEY DEFAULT 1 CHECK (singleton_id = 1),
    branch_code VARCHAR(24) NOT NULL DEFAULT 'MAIN',
    branch_name VARCHAR(180) NOT NULL DEFAULT 'Main Branch',
    branch_location VARCHAR(300),
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (tax_rate >= 0 AND tax_rate <= 100),
    updated_by INTEGER REFERENCES employees(employee_id) ON DELETE SET NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO branch_settings (singleton_id, branch_code, branch_name, branch_location)
VALUES (1, 'MAIN', 'Main Branch', '')
ON CONFLICT (singleton_id) DO NOTHING;

-- No account or password is seeded. On a fresh database, the application presents a
-- one-time administrator setup screen and stores the chosen password as a PBKDF2 hash.

CREATE TABLE IF NOT EXISTS categories (
    category_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_active_name
    ON categories (LOWER(name)) WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS products (
    product_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    barcode VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    category_id INTEGER NOT NULL REFERENCES categories(category_id),
    purchase_price NUMERIC(14,2) NOT NULL CHECK (purchase_price > 0),
    selling_price NUMERIC(14,2) NOT NULL CHECK (selling_price >= purchase_price),
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    minimum_stock INTEGER NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(254),
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS customers (
    customer_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(180) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(254),
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS purchases (
    purchase_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    supplier_id INTEGER NOT NULL REFERENCES suppliers(supplier_id),
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    purchase_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS purchase_items (
    purchase_item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_id INTEGER NOT NULL REFERENCES purchases(purchase_id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    purchase_price NUMERIC(14,2) NOT NULL CHECK (purchase_price > 0),
    subtotal NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0)
);

CREATE TABLE IF NOT EXISTS sales (
    sale_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(customer_id),
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    sale_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (subtotal_amount >= 0),
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (tax_rate >= 0 AND tax_rate <= 100),
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total_amount NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
    notes TEXT
);

-- Safe upgrades for databases created by the earlier project versions.
ALTER TABLE sales ADD COLUMN IF NOT EXISTS customer_id INTEGER REFERENCES customers(customer_id);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS subtotal_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS sale_items (
    sale_item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sale_id INTEGER NOT NULL REFERENCES sales(sale_id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    selling_price NUMERIC(14,2) NOT NULL CHECK (selling_price > 0),
    cost_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
    subtotal NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
    refund_limit NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (refund_limit >= 0)
);

-- Safe upgrade for databases created before cost_price was tracked per sale item.
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS cost_price NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS refund_limit NUMERIC(14,2) NOT NULL DEFAULT 0;

-- This historical backfill is necessarily approximate. Record completion so that later
-- product cost edits and legitimate zero-cost sale items are never rewritten on startup.
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Repair earlier purchases whose header total was left at zero despite saved lines.
DO $$
BEGIN
    PERFORM pg_advisory_xact_lock(4815162342);
    IF NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version = 'purchase_zero_totals_repair_v1') THEN
        UPDATE purchases p
        SET total_amount = lines.item_total
        FROM (
            SELECT purchase_id, SUM(subtotal) AS item_total
            FROM purchase_items
            GROUP BY purchase_id
        ) lines
        WHERE p.purchase_id = lines.purchase_id
          AND p.total_amount = 0
          AND lines.item_total > 0;
        INSERT INTO schema_migrations(version) VALUES ('purchase_zero_totals_repair_v1');
    END IF;
END $$;

DO $$
BEGIN
    PERFORM pg_advisory_xact_lock(4815162342);
    IF NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version = 'sale_discount_and_refund_limits_v1') THEN
        UPDATE sales SET subtotal_amount = total_amount WHERE subtotal_amount = 0 AND total_amount > 0;
        UPDATE sale_items SET refund_limit = subtotal WHERE refund_limit = 0 AND subtotal > 0;
        INSERT INTO schema_migrations(version) VALUES ('sale_discount_and_refund_limits_v1');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS sale_payments (
    payment_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sale_id INTEGER NOT NULL REFERENCES sales(sale_id) ON DELETE CASCADE,
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    payment_method VARCHAR(24) NOT NULL CHECK (payment_method IN ('CASH', 'CARD', 'BANK_TRANSFER', 'OTHER', 'LEGACY')),
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    amount_tendered NUMERIC(14,2) NOT NULL CHECK (amount_tendered >= amount),
    change_due NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (change_due >= 0),
    reference VARCHAR(120),
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sale_payments_sale ON sale_payments(sale_id, payment_date);
CREATE INDEX IF NOT EXISTS idx_sale_payments_date ON sale_payments(payment_date DESC);

-- Before payment tracking existed, every saved sale represented a completed sale.
-- Preserve that meaning with a one-time legacy payment for historic records.
DO $$
BEGIN
    PERFORM pg_advisory_xact_lock(4815162342);
    IF NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version = 'sale_payment_ledger_v1') THEN
        INSERT INTO sale_payments (sale_id, employee_id, payment_method, amount, amount_tendered, change_due, payment_date)
        SELECT s.sale_id, s.employee_id, 'LEGACY', s.total_amount, s.total_amount, 0, s.sale_date
        FROM sales s
        WHERE s.total_amount > 0
          AND NOT EXISTS (SELECT 1 FROM sale_payments p WHERE p.sale_id = s.sale_id);
        INSERT INTO schema_migrations(version) VALUES ('sale_payment_ledger_v1');
    END IF;
END $$;
DO $$
BEGIN
    PERFORM pg_advisory_xact_lock(4815162342);
    IF NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version = 'sale_item_cost_price_backfill_v1') THEN
        UPDATE sale_items si
        SET cost_price = p.purchase_price
        FROM products p
        WHERE si.product_id = p.product_id
          AND si.cost_price = 0;
        INSERT INTO schema_migrations(version) VALUES ('sale_item_cost_price_backfill_v1');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS activity_logs (
    log_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    employee_name VARCHAR(220) NOT NULL,
    module VARCHAR(80) NOT NULL,
    action VARCHAR(80) NOT NULL,
    description TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Compatibility migration for the original activity_logs table, which used
-- created_at and did not retain the employee display name.
ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS employee_name VARCHAR(220);
ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS timestamp TIMESTAMP;
ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
UPDATE activity_logs
SET employee_name = COALESCE(employee_name, 'System'),
    timestamp = COALESCE(timestamp, created_at, CURRENT_TIMESTAMP)
WHERE employee_name IS NULL OR timestamp IS NULL;
ALTER TABLE activity_logs ALTER COLUMN employee_name SET NOT NULL;
ALTER TABLE activity_logs ALTER COLUMN timestamp SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE activity_logs ALTER COLUMN timestamp SET NOT NULL;

CREATE TABLE IF NOT EXISTS inventory_movements (
    movement_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    movement_type VARCHAR(20) NOT NULL CHECK (movement_type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'RETURN')),
    quantity_changed INTEGER NOT NULL,
    quantity_before INTEGER NOT NULL CHECK (quantity_before >= 0),
    quantity_after INTEGER NOT NULL CHECK (quantity_after >= 0),
    reference VARCHAR(120),
    notes TEXT,
    movement_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_product ON inventory_movements(product_id, movement_date DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_date ON inventory_movements(movement_date DESC);

-- Widen older installations once. Avoid dropping/recreating this constraint on every startup.
DO $$
DECLARE movement_constraint RECORD;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'inventory_movements'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%movement_type%'
          AND pg_get_constraintdef(oid) ILIKE '%PURCHASE_RETURN%'
    ) THEN
        FOR movement_constraint IN
            SELECT conname FROM pg_constraint
            WHERE conrelid = 'inventory_movements'::regclass
              AND contype = 'c'
              AND pg_get_constraintdef(oid) ILIKE '%movement_type%'
        LOOP
            EXECUTE format('ALTER TABLE inventory_movements DROP CONSTRAINT %I', movement_constraint.conname);
        END LOOP;
        ALTER TABLE inventory_movements ADD CONSTRAINT inventory_movements_movement_type_check
            CHECK (movement_type IN ('PURCHASE', 'SALE', 'ADJUSTMENT', 'RETURN', 'PURCHASE_RETURN'));
    END IF;
END $$;

-- Customer returns: select a sale, return one or more of its items, optionally
-- restock the product, and record the refund. A sale item can be returned across
-- more than one return, but never more than was originally sold (enforced in
-- ReturnDAO with a row-level lock on sale_items at return time).
CREATE TABLE IF NOT EXISTS returns (
    return_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sale_id INTEGER NOT NULL REFERENCES sales(sale_id),
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    return_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_refund NUMERIC(14,2) NOT NULL CHECK (total_refund >= 0),
    reason TEXT
);

CREATE TABLE IF NOT EXISTS return_items (
    return_item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    return_id INTEGER NOT NULL REFERENCES returns(return_id) ON DELETE CASCADE,
    sale_item_id INTEGER NOT NULL REFERENCES sale_items(sale_item_id),
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    refund_amount NUMERIC(14,2) NOT NULL CHECK (refund_amount >= 0),
    -- Whether this quantity was put back into sellable stock. A defective/damaged
    -- item can be refunded without being restocked.
    restocked BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_returns_sale ON returns(sale_id);
CREATE INDEX IF NOT EXISTS idx_returns_date ON returns(return_date DESC);
CREATE INDEX IF NOT EXISTS idx_return_items_return ON return_items(return_id);
CREATE INDEX IF NOT EXISTS idx_return_items_sale_item ON return_items(sale_item_id);

CREATE INDEX IF NOT EXISTS idx_products_category_active ON products(category_id) WHERE active = TRUE;

-- Purchase returns: send purchased stock back to a supplier (defective/wrong/excess
-- goods received) and record the resulting credit. Stock is reduced, mirroring how
-- customer returns increase it, and never allowed to go below zero.
CREATE TABLE IF NOT EXISTS purchase_returns (
    purchase_return_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_id INTEGER NOT NULL REFERENCES purchases(purchase_id),
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    return_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_credit NUMERIC(14,2) NOT NULL CHECK (total_credit >= 0),
    reason TEXT
);

CREATE TABLE IF NOT EXISTS purchase_return_items (
    purchase_return_item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_return_id INTEGER NOT NULL REFERENCES purchase_returns(purchase_return_id) ON DELETE CASCADE,
    purchase_item_id INTEGER NOT NULL REFERENCES purchase_items(purchase_item_id),
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    credit_amount NUMERIC(14,2) NOT NULL CHECK (credit_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_purchase_returns_purchase ON purchase_returns(purchase_id);
CREATE INDEX IF NOT EXISTS idx_purchase_returns_date ON purchase_returns(return_date DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_return_items_return ON purchase_return_items(purchase_return_id);
CREATE INDEX IF NOT EXISTS idx_purchase_return_items_purchase_item ON purchase_return_items(purchase_item_id);
CREATE INDEX IF NOT EXISTS idx_purchases_date ON purchases(purchase_date DESC);
CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date DESC);
CREATE INDEX IF NOT EXISTS idx_sales_customer ON sales(customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_purchase_items_purchase ON purchase_items(purchase_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_activity_logs_employee_time ON activity_logs(employee_id, timestamp DESC);

-- Physical counts capture an immutable on-hand snapshot; variances require an
-- explicit investigation and adjustment before they can change product stock.
CREATE TABLE IF NOT EXISTS inventory_count_sessions (
    count_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    started_by INTEGER NOT NULL REFERENCES employees(employee_id),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_by INTEGER REFERENCES employees(employee_id),
    completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS inventory_count_items (
    count_item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    count_id INTEGER NOT NULL REFERENCES inventory_count_sessions(count_id),
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    recorded_quantity INTEGER NOT NULL CHECK (recorded_quantity >= 0),
    last_movement_id BIGINT,
    counted_quantity INTEGER CHECK (counted_quantity IS NULL OR counted_quantity >= 0),
    variance INTEGER,
    count_reason TEXT,
    resolution_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (resolution_status IN ('PENDING', 'MATCHED', 'OPEN', 'RESOLVED')),
    resolution_notes TEXT,
    resolved_by INTEGER REFERENCES employees(employee_id),
    resolved_at TIMESTAMP,
    applied_quantity_before INTEGER CHECK (applied_quantity_before IS NULL OR applied_quantity_before >= 0),
    applied_quantity_after INTEGER CHECK (applied_quantity_after IS NULL OR applied_quantity_after >= 0),
    UNIQUE (count_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_inventory_count_status ON inventory_count_sessions(status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_count_items_variance ON inventory_count_items(resolution_status, count_id);

-- Existing plaintext employee passwords are upgraded to PBKDF2 automatically after successful login.
