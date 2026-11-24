CREATE TABLE dbo.shopping_cart (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_shopping_cart PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_type INT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name NVARCHAR(60) NOT NULL,
    flavors_json NVARCHAR(MAX) NOT NULL,
    flavor_key CHAR(64) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT FK_shopping_cart_customer FOREIGN KEY (customer_id) REFERENCES dbo.customer(id),
    CONSTRAINT UQ_cart_product_flavor UNIQUE (customer_id, product_type, product_id, flavor_key),
    CONSTRAINT CK_cart_type CHECK (product_type IN (1, 2)),
    CONSTRAINT CK_cart_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT CK_cart_flavors CHECK (ISJSON(flavors_json) = 1)
);

CREATE TABLE dbo.orders (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_orders PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    order_number VARCHAR(40) NOT NULL CONSTRAINT UQ_orders_number UNIQUE,
    request_id CHAR(36) NOT NULL,
    status INT NOT NULL,
    total DECIMAL(16, 2) NOT NULL,
    contact_name NVARCHAR(32) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    delivery_address NVARCHAR(200) NOT NULL,
    note NVARCHAR(200) NOT NULL,
    created_at DATETIME2(3) NOT NULL,
    paid_at DATETIME2(3) NULL,
    CONSTRAINT FK_orders_customer FOREIGN KEY (customer_id) REFERENCES dbo.customer(id),
    CONSTRAINT UQ_orders_customer_request UNIQUE (customer_id, request_id),
    CONSTRAINT CK_orders_status CHECK (status IN (1, 2, 3, 4, 5)),
    CONSTRAINT CK_orders_total CHECK (total > 0)
);

CREATE INDEX IX_orders_customer_date ON dbo.orders (customer_id, created_at DESC, id DESC);

CREATE TABLE dbo.order_detail (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_order_detail PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_type INT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name NVARCHAR(60) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    flavors_json NVARCHAR(MAX) NOT NULL,
    CONSTRAINT FK_order_detail_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
    CONSTRAINT CK_order_detail_type CHECK (product_type IN (1, 2)),
    CONSTRAINT CK_order_detail_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT CK_order_detail_price CHECK (unit_price > 0),
    CONSTRAINT CK_order_detail_flavors CHECK (ISJSON(flavors_json) = 1)
);

CREATE INDEX IX_order_detail_order ON dbo.order_detail (order_id, id);
