ALTER TABLE dbo.order_detail
ADD meal_contents_json NVARCHAR(MAX) NOT NULL
    CONSTRAINT DF_order_detail_meal_contents DEFAULT (N'[]');
GO

ALTER TABLE dbo.order_detail
ADD CONSTRAINT CK_order_detail_meal_contents CHECK (ISJSON(meal_contents_json) = 1);
