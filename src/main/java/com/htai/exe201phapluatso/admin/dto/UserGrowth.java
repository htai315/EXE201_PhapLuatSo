package com.htai.exe201phapluatso.admin.dto;

import java.time.LocalDate;

/**
 * DTO for user growth chart data
 */
public class UserGrowth {
    
    private LocalDate date;
    private Long newUsers;
    private Long totalUsers;

    // Constructors
    public UserGrowth() {}

    public UserGrowth(LocalDate date, Long newUsers, Long totalUsers) {
        this.date = date;
        this.newUsers = newUsers;
        this.totalUsers = totalUsers;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(Long newUsers) {
        this.newUsers = newUsers;
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }
}
