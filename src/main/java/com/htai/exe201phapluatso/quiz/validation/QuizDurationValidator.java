package com.htai.exe201phapluatso.quiz.validation;

import com.htai.exe201phapluatso.common.exception.BadRequestException;

/**
 * Validator cho quiz duration
 * Đảm bảo thời gian làm bài nằm trong khoảng hợp lệ
 */
public final class QuizDurationValidator {

    public static final int MIN_DURATION_MINUTES = 5;
    public static final int MAX_DURATION_MINUTES = 180;
    public static final int DEFAULT_DURATION_MINUTES = 45;

    private QuizDurationValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validate và trả về duration hợp lệ
     * @param duration Duration từ request (có thể null)
     * @return Duration hợp lệ (default nếu null)
     * @throws BadRequestException nếu duration ngoài khoảng [5, 180]
     */
    public static int validateAndGetDuration(Integer duration) {
        if (duration == null) {
            return DEFAULT_DURATION_MINUTES;
        }

        if (duration < MIN_DURATION_MINUTES || duration > MAX_DURATION_MINUTES) {
            throw new BadRequestException(
                    "Thời gian làm bài phải từ " + MIN_DURATION_MINUTES + 
                    " đến " + MAX_DURATION_MINUTES + " phút"
            );
        }

        return duration;
    }

    /**
     * Check if duration is valid without throwing exception
     * @param duration Duration to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidDuration(Integer duration) {
        if (duration == null) {
            return true; // null is valid (will use default)
        }
        return duration >= MIN_DURATION_MINUTES && duration <= MAX_DURATION_MINUTES;
    }
}
