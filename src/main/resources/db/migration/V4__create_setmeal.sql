CREATE TABLE dbo.setmeal (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_setmeal PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name NVARCHAR(60) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    description NVARCHAR(500) NOT NULL,
    on_sale BIT NOT NULL,
    image_media_type VARCHAR(10) NOT NULL,
    image_contents VARBINARY(MAX) NOT NULL,
    created_at DATETIME2(3) NOT NULL,
    updated_at DATETIME2(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    CONSTRAINT UQ_setmeal_name UNIQUE (name),
    CONSTRAINT CK_setmeal_price CHECK (price > 0),
    CONSTRAINT CK_setmeal_image_type CHECK (image_media_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT CK_setmeal_image_size CHECK (DATALENGTH(image_contents) BETWEEN 1 AND 5242880),
    CONSTRAINT FK_setmeal_category FOREIGN KEY (category_id) REFERENCES dbo.category(id)
);

CREATE INDEX IX_setmeal_category ON dbo.setmeal (category_id, id);

CREATE TABLE dbo.setmeal_dish (
    setmeal_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    position INT NOT NULL,
    CONSTRAINT PK_setmeal_dish PRIMARY KEY (setmeal_id, dish_id),
    CONSTRAINT CK_setmeal_dish_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT FK_setmeal_dish_setmeal FOREIGN KEY (setmeal_id) REFERENCES dbo.setmeal(id) ON DELETE CASCADE,
    CONSTRAINT FK_setmeal_dish_dish FOREIGN KEY (dish_id) REFERENCES dbo.dish(id)
);

CREATE INDEX IX_setmeal_dish_dish ON dbo.setmeal_dish (dish_id, setmeal_id);
