package com.htai.exe201phapluatso.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for banning a user
 */
public class BanUserRequest {
    
    @NotBlank(message = "Ban reason is required")
    @Size(min = 3, max = 500, message = "Ban reason must be between 3 and 500 characters")
    private String reason;

    // Constructors
    public BanUserRequest() {}

    public BanUserRequest(String reason) {
        this.reason = reason;
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
