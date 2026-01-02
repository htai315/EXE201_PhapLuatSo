package com.htai.exe201phapluatso.admin.repo;

import com.htai.exe201phapluatso.admin.entity.AdminActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for AdminActivityLog entity
 */
@Repository
public interface AdminActivityLogRepo extends JpaRepository<AdminActivityLog, Long> {
    
    /**
     * Find all activity logs by admin user id with pagination
     */
    Page<AdminActivityLog> findByAdminUserId(Long adminUserId, Pageable pageable);
    
    /**
     * Find all activity logs by action type with pagination
     */
    Page<AdminActivityLog> findByActionType(String actionType, Pageable pageable);
    
    /**
     * Find all activity logs by target type and target id
     */
    Page<AdminActivityLog> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);
    
    /**
     * Find all activity logs with admin user eagerly loaded
     * Note: Removed ORDER BY from query to avoid duplicate with Pageable sort
     */
    @Query(value = "SELECT a FROM AdminActivityLog a LEFT JOIN FETCH a.adminUser",
           countQuery = "SELECT COUNT(a) FROM AdminActivityLog a")
    Page<AdminActivityLog> findAllWithAdminUser(Pageable pageable);
}
