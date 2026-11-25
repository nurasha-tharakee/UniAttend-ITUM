package com.university.attendance.service;

import com.university.attendance.entity.AuditLog;
import com.university.attendance.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String username, String action, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog(username != null ? username : "ANONYMOUS", action, details, ipAddress);
        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<AuditLog> getUserLogs(String username) {
        return auditLogRepository.findByUsernameOrderByCreatedAtDesc(username);
    }
}
