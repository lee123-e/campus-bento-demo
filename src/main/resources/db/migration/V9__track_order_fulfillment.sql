ALTER TABLE dbo.orders
ADD dispatched_at DATETIME2(3) NULL,
    dispatched_by BIGINT NULL,
    completed_at DATETIME2(3) NULL,
    completed_by BIGINT NULL;

CREATE INDEX IX_orders_created_at ON dbo.orders (created_at DESC, id DESC);
