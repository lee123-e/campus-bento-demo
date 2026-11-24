CREATE TABLE dbo.dish (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_dish PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name NVARCHAR(60) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    description NVARCHAR(500) NOT NULL,
    on_sale BIT NOT NULL,
    created_at DATETIME2(3) NOT NULL,
    updated_at DATETIME2(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    CONSTRAINT UQ_dish_name UNIQUE (name),
    CONSTRAINT CK_dish_price CHECK (price > 0),
    CONSTRAINT FK_dish_category FOREIGN KEY (category_id) REFERENCES dbo.category(id)
);

CREATE INDEX IX_dish_category ON dbo.dish (category_id, id);

CREATE TABLE dbo.dish_flavor (
    dish_id BIGINT NOT NULL,
    group_position INT NOT NULL,
    option_position INT NOT NULL,
    group_name NVARCHAR(40) NOT NULL,
    option_value NVARCHAR(40) NOT NULL,
    CONSTRAINT PK_dish_flavor PRIMARY KEY (dish_id, group_position, option_position),
    CONSTRAINT FK_dish_flavor_dish FOREIGN KEY (dish_id) REFERENCES dbo.dish(id) ON DELETE CASCADE
);

CREATE TABLE dbo.dish_image (
    dish_id BIGINT CONSTRAINT PK_dish_image PRIMARY KEY,
    media_type VARCHAR(10) NOT NULL,
    contents VARBINARY(MAX) NOT NULL,
    CONSTRAINT CK_dish_image_type CHECK (media_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT CK_dish_image_size CHECK (DATALENGTH(contents) BETWEEN 1 AND 5242880),
    CONSTRAINT FK_dish_image_dish FOREIGN KEY (dish_id) REFERENCES dbo.dish(id) ON DELETE CASCADE
);
