CREATE TABLE dbo.customer (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_customer PRIMARY KEY,
    phone VARCHAR(11) NOT NULL CONSTRAINT UQ_customer_phone UNIQUE,
    created_at DATETIME2(3) NOT NULL CONSTRAINT DF_customer_created_at DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.address_book (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_address_book PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    contact_name NVARCHAR(32) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    delivery_address NVARCHAR(200) NOT NULL,
    label NVARCHAR(12) NOT NULL,
    is_default BIT NOT NULL,
    CONSTRAINT FK_address_book_customer FOREIGN KEY (customer_id) REFERENCES dbo.customer(id)
);

CREATE INDEX IX_address_book_customer ON dbo.address_book (customer_id, id);
CREATE UNIQUE INDEX UQ_address_book_default ON dbo.address_book (customer_id) WHERE is_default = 1;
