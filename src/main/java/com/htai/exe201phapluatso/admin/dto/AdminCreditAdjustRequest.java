package com.htai.exe201phapluatso.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for admin credit adjustment
 */
public class AdminCreditAdjustRequest {

    @Min(value = 0, message = "Chat credits phải >= 0")
    private int chatCredits;

    @Min(value = 0, message = "Quiz gen credits phải >= 0")
    private int quizGenCredits;

    @NotBlank(message = "Lý do không được để trống")
    @Size(max = 500, message = "Lý do không được quá 500 ký tự")
    private String reason;

    // Getters and Setters
    public int getChatCredits() { return chatCredits; }
    public void setChatCredits(int chatCredits) { this.chatCredits = chatCredits; }

    public int getQuizGenCredits() { return quizGenCredits; }
    public void setQuizGenCredits(int quizGenCredits) { this.quizGenCredits = quizGenCredits; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
