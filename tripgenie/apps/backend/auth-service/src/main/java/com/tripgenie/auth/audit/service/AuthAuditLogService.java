package com.tripgenie.auth.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripgenie.auth.audit.domain.AuthAuditLog;
import com.tripgenie.auth.audit.repository.AuthAuditLogRepository;
import com.tripgenie.common.observability.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthAuditLogService {
    private final AuthAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuthAuditLogService(AuthAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId, String actionType, Map<String, ?> metadata) {
        record(userId, actionType, "SUCCESS", metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId, String actionType, Map<String, ?> metadata) {
        record(userId, actionType, "FAILURE", metadata);
    }

    private void record(UUID userId, String actionType, String status, Map<String, ?> metadata) {
        AuthAuditLog auditLog = new AuthAuditLog();
        auditLog.setUserId(userId);
        auditLog.setActionType(actionType);
        auditLog.setEntityType("USER");
        auditLog.setEntityId(userId);
        auditLog.setStatus(status);
        auditLog.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        auditLog.setMetadata(serialize(metadata));
        auditLogRepository.save(auditLog);
    }

    private String serialize(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize audit metadata", exception);
        }
    }
}
