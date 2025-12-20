-- =========================
-- QUIZ SETS
-- =========================
CREATE TABLE dbo.quiz_sets (
                               id BIGINT IDENTITY(1,1) PRIMARY KEY,
                               created_by BIGINT NOT NULL,
                               title NVARCHAR(200) NOT NULL,
                               description NVARCHAR(1000) NULL,
                               visibility NVARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
                               status NVARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                               created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                               updated_at DATETIME2 NULL,
                               CONSTRAINT fk_quiz_sets_user
                                   FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE INDEX ix_quiz_sets_created_by
    ON dbo.quiz_sets(created_by);

-- =========================
-- QUIZ QUESTIONS
-- =========================
CREATE TABLE dbo.quiz_questions (
                                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                    quiz_set_id BIGINT NOT NULL,
                                    question_text NVARCHAR(2000) NOT NULL,
                                    explanation NVARCHAR(2000) NULL,
                                    sort_order INT NOT NULL DEFAULT 0,
                                    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                                    CONSTRAINT fk_questions_set
                                        FOREIGN KEY (quiz_set_id) REFERENCES dbo.quiz_sets(id)
                                            ON DELETE CASCADE
);

CREATE INDEX ix_questions_set
    ON dbo.quiz_questions(quiz_set_id);

-- =========================
-- QUIZ QUESTION OPTIONS
-- =========================
CREATE TABLE dbo.quiz_question_options (
                                           id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                           question_id BIGINT NOT NULL,
                                           option_key CHAR(1) NOT NULL,
                                           option_text NVARCHAR(1000) NOT NULL,
                                           is_correct BIT NOT NULL DEFAULT 0,
                                           CONSTRAINT fk_options_question
                                               FOREIGN KEY (question_id) REFERENCES dbo.quiz_questions(id)
                                                   ON DELETE CASCADE,
                                           CONSTRAINT ck_option_key
                                               CHECK (option_key IN ('A','B','C','D'))
);

CREATE UNIQUE INDEX ux_options_question_key
    ON dbo.quiz_question_options(question_id, option_key);
