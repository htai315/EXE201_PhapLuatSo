package com.htai.exe201phapluatso.admin.service;

import com.htai.exe201phapluatso.admin.entity.AdminActivityLog;
import com.htai.exe201phapluatso.admin.repo.AdminActivityLogRepo;
import com.htai.exe201phapluatso.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for logging admin activities
 * Provides centralized logging for all admin actions
 */
@Service
public class AdminActivityLogService {

    private final AdminActivityLogRepo adminActivityLogRepo;

    public AdminActivityLogService(AdminActivityLogRepo adminActivityLogRepo) {
        this.adminActivityLogRepo = adminActivityLogRepo;
    }

    /**
     * Log an admin action
     */
    @Transactional
    public void logAction(User adminUser, String actionType, String targetType, 
                         Long targetId, String description) {
        AdminActivityLog log = new AdminActivityLog(
            adminUser, 
            actionType, 
            targetType, 
            targetId, 
            description
        );
        adminActivityLogRepo.save(log);
    }

    /**
     * Get all activity logs with pagination
     */
    public Page<AdminActivityLog> getAllLogs(Pageable pageable) {
        return adminActivityLogRepo.findAllWithAdminUser(pageable);
    }

    /**
     * Get activity logs by admin user
     */
    public Page<AdminActivityLog> getLogsByAdmin(Long adminUserId, Pageable pageable) {
        return adminActivityLogRepo.findByAdminUserId(adminUserId, pageable);
    }

    /**
     * Get activity logs by action type
     */
    public Page<AdminActivityLog> getLogsByActionType(String actionType, Pageable pageable) {
        return adminActivityLogRepo.findByActionType(actionType, pageable);
    }

    /**
     * Get activity logs by target
     */
    public Page<AdminActivityLog> getLogsByTarget(String targetType, Long targetId, Pageable pageable) {
        return adminActivityLogRepo.findByTargetTypeAndTargetId(targetType, targetId, pageable);
    }
}
