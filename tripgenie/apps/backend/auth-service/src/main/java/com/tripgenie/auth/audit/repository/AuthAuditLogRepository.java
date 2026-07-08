package com.tripgenie.auth.audit.repository;

import com.tripgenie.auth.audit.domain.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {
}
