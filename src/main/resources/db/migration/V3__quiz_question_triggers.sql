CREATE OR ALTER TRIGGER dbo.trg_only_one_correct_option
ON dbo.quiz_question_options
AFTER INSERT, UPDATE
                                  AS
BEGIN
  SET NOCOUNT ON;

  IF EXISTS (
    SELECT 1
    FROM inserted i
    JOIN dbo.quiz_question_options o
      ON o.question_id = i.question_id
    GROUP BY o.question_id
    HAVING SUM(CASE WHEN o.is_correct = 1 THEN 1 ELSE 0 END) > 1
  )
BEGIN
    RAISERROR(N'Each question can have only one correct option.', 16, 1);
ROLLBACK TRANSACTION;
END
END;
