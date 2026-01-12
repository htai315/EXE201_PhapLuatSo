package com.htai.exe201phapluatso.admin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for credit analytics
 */
public class CreditAnalyticsResponse {

    private long totalChatUsed;
    private long totalQuizGenUsed;
    private long totalChatPurchased;
    private long totalQuizGenPurchased;
    private long totalChatRefunded;
    private long totalQuizGenRefunded;
    private List<DailyUsage> usageByDay;

    // Getters and Setters
    public long getTotalChatUsed() { return totalChatUsed; }
    public void setTotalChatUsed(long totalChatUsed) { this.totalChatUsed = totalChatUsed; }

    public long getTotalQuizGenUsed() { return totalQuizGenUsed; }
    public void setTotalQuizGenUsed(long totalQuizGenUsed) { this.totalQuizGenUsed = totalQuizGenUsed; }

    public long getTotalChatPurchased() { return totalChatPurchased; }
    public void setTotalChatPurchased(long totalChatPurchased) { this.totalChatPurchased = totalChatPurchased; }

    public long getTotalQuizGenPurchased() { return totalQuizGenPurchased; }
    public void setTotalQuizGenPurchased(long totalQuizGenPurchased) { this.totalQuizGenPurchased = totalQuizGenPurchased; }

    public long getTotalChatRefunded() { return totalChatRefunded; }
    public void setTotalChatRefunded(long totalChatRefunded) { this.totalChatRefunded = totalChatRefunded; }

    public long getTotalQuizGenRefunded() { return totalQuizGenRefunded; }
    public void setTotalQuizGenRefunded(long totalQuizGenRefunded) { this.totalQuizGenRefunded = totalQuizGenRefunded; }

    public List<DailyUsage> getUsageByDay() { return usageByDay; }
    public void setUsageByDay(List<DailyUsage> usageByDay) { this.usageByDay = usageByDay; }

    /**
     * Daily usage statistics
     */
    public static class DailyUsage {
        private LocalDate date;
        private long chatUsed;
        private long quizGenUsed;

        public DailyUsage() {}

        public DailyUsage(LocalDate date, long chatUsed, long quizGenUsed) {
            this.date = date;
            this.chatUsed = chatUsed;
            this.quizGenUsed = quizGenUsed;
        }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public long getChatUsed() { return chatUsed; }
        public void setChatUsed(long chatUsed) { this.chatUsed = chatUsed; }

        public long getQuizGenUsed() { return quizGenUsed; }
        public void setQuizGenUsed(long quizGenUsed) { this.quizGenUsed = quizGenUsed; }
    }
}
