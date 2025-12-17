CREATE TABLE dbo.roles (
                           id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           name NVARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE dbo.plans (
                           id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           code NVARCHAR(50) NOT NULL UNIQUE,   -- FREE, STUDENT
                           name NVARCHAR(100) NOT NULL,
                           price INT NOT NULL DEFAULT 0
);

CREATE TABLE dbo.users (
                           id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           email NVARCHAR(255) NOT NULL UNIQUE,
                           password_hash NVARCHAR(255) NULL,
                           full_name NVARCHAR(255) NULL,
                           provider NVARCHAR(20) NOT NULL DEFAULT 'LOCAL', -- LOCAL/GOOGLE
                           provider_id NVARCHAR(255) NULL,
                           email_verified BIT NOT NULL DEFAULT 0,
                           is_enabled BIT NOT NULL DEFAULT 1,
                           created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX ux_users_provider ON dbo.users(provider, provider_id)
    WHERE provider_id IS NOT NULL;

CREATE TABLE dbo.user_roles (
                                user_id BIGINT NOT NULL,
                                role_id BIGINT NOT NULL,
                                CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
                                CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
                                CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
);

CREATE TABLE dbo.subscriptions (
                                   id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                   user_id BIGINT NOT NULL,
                                   plan_id BIGINT NOT NULL,
                                   status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                   start_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                                   end_at DATETIME2 NULL,
                                   CONSTRAINT fk_sub_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
                                   CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES dbo.plans(id)
);

CREATE TABLE dbo.refresh_tokens (
                                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    token_hash NVARCHAR(255) NOT NULL UNIQUE,
                                    expires_at DATETIME2 NOT NULL,
                                    revoked_at DATETIME2 NULL,
                                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                                    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

-- seed role
INSERT INTO dbo.roles(name) VALUES (N'USER'), (N'ADMIN');

-- seed plans (dân thường vs sinh viên khác nhau ở plan)
INSERT INTO dbo.plans(code, name, price) VALUES
                                             (N'FREE', N'Free', 0),
                                             (N'STUDENT', N'Student', 99000);
