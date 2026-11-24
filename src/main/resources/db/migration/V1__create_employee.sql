CREATE TABLE dbo.employee (
    id BIGINT IDENTITY(1, 1) CONSTRAINT PK_employee PRIMARY KEY,
    username NVARCHAR(64) NOT NULL CONSTRAINT UQ_employee_username UNIQUE,
    display_name NVARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BIT NOT NULL CONSTRAINT DF_employee_enabled DEFAULT (1)
);
