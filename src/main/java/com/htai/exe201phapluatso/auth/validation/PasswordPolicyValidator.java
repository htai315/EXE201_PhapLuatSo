package com.htai.exe201phapluatso.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for password policy.
 * Validates that password meets security requirements.
 */
public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*]");

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            // Let @NotBlank handle null/empty validation
            return true;
        }

        List<String> violations = validate(password);
        
        if (violations.isEmpty()) {
            return true;
        }

        // Disable default message and add custom messages
        context.disableDefaultConstraintViolation();
        
        for (String violation : violations) {
            context.buildConstraintViolationWithTemplate(violation)
                    .addConstraintViolation();
        }
        
        return false;
    }

    /**
     * Validate password and return list of violations.
     * Can be used directly for more detailed error handling.
     */
    public static List<String> validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            violations.add("Mật khẩu không được để trống");
            return violations;
        }

        if (password.length() < MIN_LENGTH) {
            violations.add("Mật khẩu phải có ít nhất " + MIN_LENGTH + " ký tự");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            violations.add("Mật khẩu phải có ít nhất 1 chữ hoa (A-Z)");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            violations.add("Mật khẩu phải có ít nhất 1 chữ thường (a-z)");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            violations.add("Mật khẩu phải có ít nhất 1 chữ số (0-9)");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            violations.add("Mật khẩu phải có ít nhất 1 ký tự đặc biệt (!@#$%^&*)");
        }

        return violations;
    }

    /**
     * Check if password is valid without returning violations.
     */
    public static boolean isValidPassword(String password) {
        return validate(password).isEmpty();
    }
}
