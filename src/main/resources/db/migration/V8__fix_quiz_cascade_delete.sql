-- Fix cascade delete for quiz attempts and answers
-- SQL Server doesn't allow multiple cascade paths, so we only cascade from attempts
-- and handle question_id deletion in application code

-- Drop existing foreign keys
ALTER TABLE dbo.quiz_attempts
    DROP CONSTRAINT fk_quiz_attempt_set;

ALTER TABLE dbo.quiz_attempt_answers
    DROP CONSTRAINT fk_quiz_attempt_answer_attempt;

ALTER TABLE dbo.quiz_attempt_answers
    DROP CONSTRAINT fk_quiz_attempt_answer_question;

-- Recreate with proper cascade rules
-- quiz_attempts: CASCADE when quiz_set is deleted
ALTER TABLE dbo.quiz_attempts
    ADD CONSTRAINT fk_quiz_attempt_set 
    FOREIGN KEY (quiz_set_id) REFERENCES dbo.quiz_sets(id)
    ON DELETE CASCADE;

-- quiz_attempt_answers: CASCADE when attempt is deleted
ALTER TABLE dbo.quiz_attempt_answers
    ADD CONSTRAINT fk_quiz_attempt_answer_attempt 
    FOREIGN KEY (attempt_id) REFERENCES dbo.quiz_attempts(id)
    ON DELETE CASCADE;

-- quiz_attempt_answers: NO ACTION for question (to avoid multiple cascade paths)
-- Application code will handle deletion
ALTER TABLE dbo.quiz_attempt_answers
    ADD CONSTRAINT fk_quiz_attempt_answer_question 
    FOREIGN KEY (question_id) REFERENCES dbo.quiz_questions(id)
    ON DELETE NO ACTION;
