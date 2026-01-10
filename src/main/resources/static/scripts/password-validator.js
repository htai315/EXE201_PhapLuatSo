/**
 * Password Validation Utility
 * Validates password strength according to security policy
 */

const PasswordValidator = {
    /**
     * Minimum password requirements
     */
    requirements: {
        minLength: 8,
        requireUppercase: true,
        requireLowercase: true,
        requireNumber: true,
        requireSpecialChar: false // Optional for now
    },

    /**
     * Validate password against all requirements
     * @param {string} password - Password to validate
     * @returns {Object} - { valid: boolean, errors: string[] }
     */
    validate(password) {
        const errors = [];

        if (!password) {
            return { valid: false, errors: ['Mật khẩu không được để trống'] };
        }

        // Check minimum length
        if (password.length < this.requirements.minLength) {
            errors.push(`Mật khẩu phải có ít nhất ${this.requirements.minLength} ký tự`);
        }

        // Check uppercase letter
        if (this.requirements.requireUppercase && !/[A-Z]/.test(password)) {
            errors.push('Mật khẩu phải có ít nhất 1 chữ cái viết hoa');
        }

        // Check lowercase letter
        if (this.requirements.requireLowercase && !/[a-z]/.test(password)) {
            errors.push('Mật khẩu phải có ít nhất 1 chữ cái viết thường');
        }

        // Check number
        if (this.requirements.requireNumber && !/\d/.test(password)) {
            errors.push('Mật khẩu phải có ít nhất 1 chữ số');
        }

        // Check special character (optional)
        if (this.requirements.requireSpecialChar && !/[@$!%*?&]/.test(password)) {
            errors.push('Mật khẩu phải có ít nhất 1 ký tự đặc biệt (@$!%*?&)');
        }

        return {
            valid: errors.length === 0,
            errors: errors
        };
    },

    /**
     * Get password strength (weak, medium, strong)
     * @param {string} password
     * @returns {string} - 'weak', 'medium', 'strong'
     */
    getStrength(password) {
        if (!password) return 'weak';

        let score = 0;

        // Length score
        if (password.length >= 8) score++;
        if (password.length >= 12) score++;
        if (password.length >= 16) score++;

        // Character variety score
        if (/[a-z]/.test(password)) score++;
        if (/[A-Z]/.test(password)) score++;
        if (/\d/.test(password)) score++;
        if (/[@$!%*?&]/.test(password)) score++;

        // Determine strength
        if (score <= 3) return 'weak';
        if (score <= 5) return 'medium';
        return 'strong';
    },

    /**
     * Get strength color for UI
     * @param {string} strength
     * @returns {string} - CSS color
     */
    getStrengthColor(strength) {
        switch (strength) {
            case 'weak': return '#dc3545'; // red
            case 'medium': return '#ffc107'; // yellow
            case 'strong': return '#28a745'; // green
            default: return '#6c757d'; // gray
        }
    },

    /**
     * Get strength text in Vietnamese
     * @param {string} strength
     * @returns {string}
     */
    getStrengthText(strength) {
        switch (strength) {
            case 'weak': return 'Yếu';
            case 'medium': return 'Trung bình';
            case 'strong': return 'Mạnh';
            default: return '';
        }
    }
};

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PasswordValidator;
}
