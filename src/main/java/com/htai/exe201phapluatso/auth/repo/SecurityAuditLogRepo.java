package com.htai.exe201phapluatso.auth.repo;

import com.htai.exe201phapluatso.auth.entity.SecurityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityAuditLogRepo extends JpaRepository<SecurityAuditLog, Long> {

    /**
     * Find logs by event type
     */
    Page<SecurityAuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

    /**
     * Find logs by user ID
     */
    Page<SecurityAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find logs by IP address
     */
    Page<SecurityAuditLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress, Pageable pageable);

    /**
     * Find recent logs by user and event type
     */
    List<SecurityAuditLog> findByUserIdAndEventTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId, String eventType, LocalDateTime after);

    /**
     * Find recent logs by IP and event type
     */
    List<SecurityAuditLog> findByIpAddressAndEventTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            String ipAddress, String eventType, LocalDateTime after);

    /**
     * Count events by type within time range
     */
    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.eventType = :eventType AND s.createdAt > :after")
    long countByEventTypeAfter(@Param("eventType") String eventType, @Param("after") LocalDateTime after);

    /**
     * Count failed login attempts by IP within time range
     */
    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.ipAddress = :ip AND s.eventType = 'LOGIN_FAILED' AND s.createdAt > :after")
    long countFailedLoginsByIpAfter(@Param("ip") String ip, @Param("after") LocalDateTime after);

    /**
     * Find all logs within time range (for admin dashboard)
     */
    Page<SecurityAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);
}
