package com.htai.exe201phapluatso.admin.dto;

import java.time.LocalDateTime;

/**
 * DTO for admin activity log response
 */
public class AdminActivityLogResponse {
    private Long id;
    private Long adminUserId;
    private String adminUserName;
    private String adminUserEmail;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAdminUserId() { return adminUserId; }
    public void setAdminUserId(Long adminUserId) { this.adminUserId = adminUserId; }

    public String getAdminUserName() { return adminUserName; }
    public void setAdminUserName(String adminUserName) { this.adminUserName = adminUserName; }

    public String getAdminUserEmail() { return adminUserEmail; }
    public void setAdminUserEmail(String adminUserEmail) { this.adminUserEmail = adminUserEmail; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
