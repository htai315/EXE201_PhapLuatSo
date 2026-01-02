package com.htai.exe201phapluatso.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for detailed user information in admin panel
 */
public class AdminUserDetailResponse {
    
    // Basic info
    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String provider;
    private String providerId;
    private boolean emailVerified;
    private boolean enabled;
    private boolean active;
    private String banReason;
    private LocalDateTime bannedAt;
    private Long bannedByUserId;
    private String bannedByUserName;
    private LocalDateTime createdAt;
    
    // Credit info
    private Integer chatCredits;
    private Integer quizGenCredits;
    private LocalDateTime creditsExpiresAt;
    
    // Statistics
    private Long totalPayments;
    private Long totalRevenue;
    private Long totalQuizSets;
    private Long totalQuizAttempts;
    private Long totalChatSessions;
    private Long totalChatMessages;
    
    // Recent activities
    private List<RecentPayment> recentPayments;
    private List<RecentQuiz> recentQuizzes;
    private List<RecentChat> recentChats;

    // Constructors
    public AdminUserDetailResponse() {}

    // Inner classes for recent activities
    public static class RecentPayment {
        private Long id;
        private String orderId;
        private Long amount;
        private String status;
        private LocalDateTime createdAt;

        public RecentPayment() {}

        public RecentPayment(Long id, String orderId, Long amount, String status, LocalDateTime createdAt) {
            this.id = id;
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class RecentQuiz {
        private Long id;
        private String title;
        private Integer questionCount;
        private LocalDateTime createdAt;

        public RecentQuiz() {}

        public RecentQuiz(Long id, String title, Integer questionCount, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.questionCount = questionCount;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getQuestionCount() { return questionCount; }
        public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class RecentChat {
        private Long id;
        private String title;
        private Integer messageCount;
        private LocalDateTime createdAt;

        public RecentChat() {}

        public RecentChat(Long id, String title, Integer messageCount, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.messageCount = messageCount;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getMessageCount() { return messageCount; }
        public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // Getters and Setters for main class
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }
    
    public LocalDateTime getBannedAt() { return bannedAt; }
    public void setBannedAt(LocalDateTime bannedAt) { this.bannedAt = bannedAt; }
    
    public Long getBannedByUserId() { return bannedByUserId; }
    public void setBannedByUserId(Long bannedByUserId) { this.bannedByUserId = bannedByUserId; }
    
    public String getBannedByUserName() { return bannedByUserName; }
    public void setBannedByUserName(String bannedByUserName) { this.bannedByUserName = bannedByUserName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Integer getChatCredits() { return chatCredits; }
    public void setChatCredits(Integer chatCredits) { this.chatCredits = chatCredits; }
    
    public Integer getQuizGenCredits() { return quizGenCredits; }
    public void setQuizGenCredits(Integer quizGenCredits) { this.quizGenCredits = quizGenCredits; }
    
    public LocalDateTime getCreditsExpiresAt() { return creditsExpiresAt; }
    public void setCreditsExpiresAt(LocalDateTime creditsExpiresAt) { this.creditsExpiresAt = creditsExpiresAt; }
    
    public Long getTotalPayments() { return totalPayments; }
    public void setTotalPayments(Long totalPayments) { this.totalPayments = totalPayments; }
    
    public Long getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Long totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public Long getTotalQuizSets() { return totalQuizSets; }
    public void setTotalQuizSets(Long totalQuizSets) { this.totalQuizSets = totalQuizSets; }
    
    public Long getTotalQuizAttempts() { return totalQuizAttempts; }
    public void setTotalQuizAttempts(Long totalQuizAttempts) { this.totalQuizAttempts = totalQuizAttempts; }
    
    public Long getTotalChatSessions() { return totalChatSessions; }
    public void setTotalChatSessions(Long totalChatSessions) { this.totalChatSessions = totalChatSessions; }
    
    public Long getTotalChatMessages() { return totalChatMessages; }
    public void setTotalChatMessages(Long totalChatMessages) { this.totalChatMessages = totalChatMessages; }
    
    public List<RecentPayment> getRecentPayments() { return recentPayments; }
    public void setRecentPayments(List<RecentPayment> recentPayments) { this.recentPayments = recentPayments; }
    
    public List<RecentQuiz> getRecentQuizzes() { return recentQuizzes; }
    public void setRecentQuizzes(List<RecentQuiz> recentQuizzes) { this.recentQuizzes = recentQuizzes; }
    
    public List<RecentChat> getRecentChats() { return recentChats; }
    public void setRecentChats(List<RecentChat> recentChats) { this.recentChats = recentChats; }
}
