ALTER TABLE dbo.customer
ADD nickname NVARCHAR(24) NOT NULL CONSTRAINT DF_customer_nickname DEFAULT (N'');
