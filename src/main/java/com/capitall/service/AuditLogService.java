package com.capitall.service;

import com.capitall.model.AuditLog;
import java.util.List;
import java.util.UUID;

public interface AuditLogService {
    void log(UUID userId, String username, String action, String ipAddress);
    List<AuditLog> getLogsForUser(UUID userId);
    List<AuditLog> getAllLogs();
}
