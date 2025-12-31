package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetOtpRepo extends JpaRepository<PasswordResetOtp, Long> {
    
    Optional<PasswordResetOtp> findByEmailAndOtpAndUsedFalse(String email, String otp);
    
    @Modifying
    @Query("DELETE FROM PasswordResetOtp p WHERE p.expiresAt < :now")
    void deleteExpiredOtps(LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM PasswordResetOtp p WHERE p.email = :email")
    void deleteByEmail(String email);
}
