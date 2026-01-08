-- Add sequence for order_code generation
-- This ensures unique order codes without collision risk
-- Starting from 10000000 to have 8-digit order codes

CREATE SEQUENCE order_code_sequence
    START WITH 10000000
    INCREMENT BY 1
    MINVALUE 10000000
    MAXVALUE 99999999
    NO CYCLE;

-- Add comment for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Sequence for generating unique payment order codes. Range: 10000000-99999999', 
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'SEQUENCE', @level1name = N'order_code_sequence';
