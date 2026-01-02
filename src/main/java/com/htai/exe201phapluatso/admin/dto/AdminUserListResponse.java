package com.htai.exe201phapluatso.admin.dto;

import java.time.LocalDateTime;

/**
 * DTO for user list in admin panel
 */
public class AdminUserListResponse {
    
    private Long id;
    private String email;
    private String fullName;
    private String provider;
    private boolean emailVerified;
    private boolean enabled;
    private boolean active;
    private String banReason;
    private LocalDateTime bannedAt;
    private LocalDateTime createdAt;
    
    // Credit info
    private Integer chatCredits;
    private Integer quizGenCredits;
    
    // Statistics
    private Long totalPayments;
    private Long totalQuizSets;
    private Long totalChatSessions;

    // Constructors
    public AdminUserListResponse() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public LocalDateTime getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(LocalDateTime bannedAt) {
        this.bannedAt = bannedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getChatCredits() {
        return chatCredits;
    }

    public void setChatCredits(Integer chatCredits) {
        this.chatCredits = chatCredits;
    }

    public Integer getQuizGenCredits() {
        return quizGenCredits;
    }

    public void setQuizGenCredits(Integer quizGenCredits) {
        this.quizGenCredits = quizGenCredits;
    }

    public Long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(Long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public Long getTotalQuizSets() {
        return totalQuizSets;
    }

    public void setTotalQuizSets(Long totalQuizSets) {
        this.totalQuizSets = totalQuizSets;
    }

    public Long getTotalChatSessions() {
        return totalChatSessions;
    }

    public void setTotalChatSessions(Long totalChatSessions) {
        this.totalChatSessions = totalChatSessions;
    }
}
