package com.htai.exe201phapluatso.admin.dto;

import java.util.Map;

/**
 * DTO for payment statistics in admin panel
 */
public class AdminPaymentStatsResponse {
    
    private Long totalPayments;
    private Long successfulPayments;
    private Long failedPayments;
    private Long pendingPayments;
    
    private Long totalRevenue;
    private Long revenueToday;
    private Long revenueThisWeek;
    private Long revenueThisMonth;
    
    private Double averagePaymentAmount;
    private Double successRate;
    
    // Revenue by plan
    private Map<String, Long> revenueByPlan;
    
    // Payment count by plan
    private Map<String, Long> paymentCountByPlan;

    // Constructors
    public AdminPaymentStatsResponse() {}

    // Getters and Setters
    public Long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(Long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public Long getSuccessfulPayments() {
        return successfulPayments;
    }

    public void setSuccessfulPayments(Long successfulPayments) {
        this.successfulPayments = successfulPayments;
    }

    public Long getFailedPayments() {
        return failedPayments;
    }

    public void setFailedPayments(Long failedPayments) {
        this.failedPayments = failedPayments;
    }

    public Long getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(Long pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public Long getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Long totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getRevenueToday() {
        return revenueToday;
    }

    public void setRevenueToday(Long revenueToday) {
        this.revenueToday = revenueToday;
    }

    public Long getRevenueThisWeek() {
        return revenueThisWeek;
    }

    public void setRevenueThisWeek(Long revenueThisWeek) {
        this.revenueThisWeek = revenueThisWeek;
    }

    public Long getRevenueThisMonth() {
        return revenueThisMonth;
    }

    public void setRevenueThisMonth(Long revenueThisMonth) {
        this.revenueThisMonth = revenueThisMonth;
    }

    public Double getAveragePaymentAmount() {
        return averagePaymentAmount;
    }

    public void setAveragePaymentAmount(Double averagePaymentAmount) {
        this.averagePaymentAmount = averagePaymentAmount;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Map<String, Long> getRevenueByPlan() {
        return revenueByPlan;
    }

    public void setRevenueByPlan(Map<String, Long> revenueByPlan) {
        this.revenueByPlan = revenueByPlan;
    }

    public Map<String, Long> getPaymentCountByPlan() {
        return paymentCountByPlan;
    }

    public void setPaymentCountByPlan(Map<String, Long> paymentCountByPlan) {
        this.paymentCountByPlan = paymentCountByPlan;
    }
}
