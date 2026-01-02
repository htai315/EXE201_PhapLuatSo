package com.htai.exe201phapluatso.admin.dto;

import java.time.LocalDate;

/**
 * DTO for revenue chart data
 */
public class RevenueByDate {
    
    private LocalDate date;
    private Long revenue;
    private Integer paymentCount;

    // Constructors
    public RevenueByDate() {}

    public RevenueByDate(LocalDate date, Long revenue, Integer paymentCount) {
        this.date = date;
        this.revenue = revenue;
        this.paymentCount = paymentCount;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getRevenue() {
        return revenue;
    }

    public void setRevenue(Long revenue) {
        this.revenue = revenue;
    }

    public Integer getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(Integer paymentCount) {
        this.paymentCount = paymentCount;
    }
}
