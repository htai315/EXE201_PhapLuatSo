package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.EmailVerificationToken;
import com.htai.exe201phapluatso.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepo extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUserAndVerifiedAtIsNull(User user);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now AND t.verifiedAt IS NULL")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
