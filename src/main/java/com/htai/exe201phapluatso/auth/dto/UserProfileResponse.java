package com.htai.exe201phapluatso.auth.dto;

public record UserProfileResponse(
    Long id,
    String email,
    String fullName,
    String role,
    String avatarUrl
) {}
