CREATE TABLE dbo.category (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_category PRIMARY KEY,
    category_type INT NOT NULL,
    name NVARCHAR(40) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME2(3) NOT NULL,
    updated_at DATETIME2(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    CONSTRAINT CK_category_type CHECK (category_type IN (1, 2)),
    CONSTRAINT CK_category_sort_order CHECK (sort_order >= 0),
    CONSTRAINT UQ_category_type_name UNIQUE (category_type, name)
);

CREATE INDEX IX_category_sort_order ON dbo.category (sort_order, id);
