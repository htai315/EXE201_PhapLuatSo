package com.htai.exe201phapluatso.admin.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for admin dashboard statistics
 */
public class AdminStatsResponse {
    
    // User statistics
    private Long totalUsers;
    private Long activeUsers;
    private Long bannedUsers;
    private Long newUsersLast30Days;
    
    // Payment statistics
    private Long totalSuccessfulPayments;
    private Long totalRevenue;
    private Long revenueLast30Days;
    
    // Quiz statistics
    private Long totalQuizSets;
    private Long totalQuizAttempts;
    
    // Chat statistics
    private Long totalChatSessions;
    private Long totalChatMessages;
    
    // Legal documents
    private Long totalLegalDocuments;
    private Long totalLegalArticles;
    
    // Charts data
    private Map<String, Long> usersByPlan;
    private List<RevenueByDate> revenueChart;
    private List<UserGrowth> userGrowthChart;

    // Constructors
    public AdminStatsResponse() {}

    // Getters and Setters
    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Long getBannedUsers() {
        return bannedUsers;
    }

    public void setBannedUsers(Long bannedUsers) {
        this.bannedUsers = bannedUsers;
    }

    public Long getNewUsersLast30Days() {
        return newUsersLast30Days;
    }

    public void setNewUsersLast30Days(Long newUsersLast30Days) {
        this.newUsersLast30Days = newUsersLast30Days;
    }

    public Long getTotalSuccessfulPayments() {
        return totalSuccessfulPayments;
    }

    public void setTotalSuccessfulPayments(Long totalSuccessfulPayments) {
        this.totalSuccessfulPayments = totalSuccessfulPayments;
    }

    public Long getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Long totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getRevenueLast30Days() {
        return revenueLast30Days;
    }

    public void setRevenueLast30Days(Long revenueLast30Days) {
        this.revenueLast30Days = revenueLast30Days;
    }

    public Long getTotalQuizSets() {
        return totalQuizSets;
    }

    public void setTotalQuizSets(Long totalQuizSets) {
        this.totalQuizSets = totalQuizSets;
    }

    public Long getTotalQuizAttempts() {
        return totalQuizAttempts;
    }

    public void setTotalQuizAttempts(Long totalQuizAttempts) {
        this.totalQuizAttempts = totalQuizAttempts;
    }

    public Long getTotalChatSessions() {
        return totalChatSessions;
    }

    public void setTotalChatSessions(Long totalChatSessions) {
        this.totalChatSessions = totalChatSessions;
    }

    public Long getTotalChatMessages() {
        return totalChatMessages;
    }

    public void setTotalChatMessages(Long totalChatMessages) {
        this.totalChatMessages = totalChatMessages;
    }

    public Long getTotalLegalDocuments() {
        return totalLegalDocuments;
    }

    public void setTotalLegalDocuments(Long totalLegalDocuments) {
        this.totalLegalDocuments = totalLegalDocuments;
    }

    public Long getTotalLegalArticles() {
        return totalLegalArticles;
    }

    public void setTotalLegalArticles(Long totalLegalArticles) {
        this.totalLegalArticles = totalLegalArticles;
    }

    public Map<String, Long> getUsersByPlan() {
        return usersByPlan;
    }

    public void setUsersByPlan(Map<String, Long> usersByPlan) {
        this.usersByPlan = usersByPlan;
    }

    public List<RevenueByDate> getRevenueChart() {
        return revenueChart;
    }

    public void setRevenueChart(List<RevenueByDate> revenueChart) {
        this.revenueChart = revenueChart;
    }

    public List<UserGrowth> getUserGrowthChart() {
        return userGrowthChart;
    }

    public void setUserGrowthChart(List<UserGrowth> userGrowthChart) {
        this.userGrowthChart = userGrowthChart;
    }
}
