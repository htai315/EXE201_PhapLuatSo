CREATE TABLE dbo.quiz_attempts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quiz_set_id BIGINT NOT NULL,
    started_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    finished_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    total_questions INT NOT NULL,
    correct_count INT NOT NULL,
    score_percent INT NOT NULL,
    CONSTRAINT fk_quiz_attempt_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT fk_quiz_attempt_set FOREIGN KEY (quiz_set_id) REFERENCES dbo.quiz_sets(id)
);

CREATE TABLE dbo.quiz_attempt_answers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_key CHAR(1) NOT NULL,
    is_correct BIT NOT NULL,
    CONSTRAINT fk_quiz_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES dbo.quiz_attempts(id),
    CONSTRAINT fk_quiz_attempt_answer_question FOREIGN KEY (question_id) REFERENCES dbo.quiz_questions(id)
);


