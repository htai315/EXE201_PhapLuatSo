package com.htai.exe201phapluatso.admin.entity;

import com.htai.exe201phapluatso.auth.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for tracking admin activities for audit trail
 */
@Entity
@Table(name = "admin_activity_logs")
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // BAN_USER, UNBAN_USER, DELETE_USER, etc.

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // USER, PAYMENT, DOCUMENT, etc.

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public AdminActivityLog() {}

    public AdminActivityLog(User adminUser, String actionType, String targetType, Long targetId, String description) {
        this.adminUser = adminUser;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public User getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(User adminUser) {
        this.adminUser = adminUser;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
