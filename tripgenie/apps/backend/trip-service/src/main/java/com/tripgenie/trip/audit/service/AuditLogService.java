package com.tripgenie.trip.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.common.exception.NotFoundException;
import com.tripgenie.common.observability.CorrelationIdFilter;
import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditEntityType;
import com.tripgenie.trip.audit.domain.AuditLog;
import com.tripgenie.trip.audit.domain.AuditStatus;
import com.tripgenie.trip.audit.dto.AuditLogResponse;
import com.tripgenie.trip.audit.repository.AuditLogRepository;
import com.tripgenie.trip.dto.PageResponse;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordSuccess(
            UUID userId,
            AuditAction actionType,
            AuditEntityType entityType,
            UUID entityId,
            Map<String, ?> metadata
    ) {
        return record(userId, actionType, entityType, entityId, AuditStatus.SUCCESS, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordFailure(
            UUID userId,
            AuditAction actionType,
            AuditEntityType entityType,
            UUID entityId,
            Map<String, ?> metadata
    ) {
        return record(userId, actionType, entityType, entityId, AuditStatus.FAILURE, metadata);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findAuditLogs(
            AuditAction action,
            UUID userId,
            AuditEntityType entityType,
            Instant from,
            Instant to,
            AuditStatus status,
            Pageable pageable
    ) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessException("INVALID_AUDIT_DATE_RANGE", "to must be on or after from", HttpStatus.BAD_REQUEST);
        }
        Page<AuditLogResponse> page = auditLogRepository.findAll(
                specification(action, userId, entityType, from, to, status), pageable).map(AuditLogResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLog(UUID id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new NotFoundException("AUDIT_LOG_NOT_FOUND", "Audit log was not found"));
    }

    private AuditLog record(
            UUID userId,
            AuditAction actionType,
            AuditEntityType entityType,
            UUID entityId,
            AuditStatus status,
            Map<String, ?> metadata
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setStatus(status);
        auditLog.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        auditLog.setMetadata(serialize(metadata));
        return auditLogRepository.save(auditLog);
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

    private Specification<AuditLog> specification(
            AuditAction action,
            UUID userId,
            AuditEntityType entityType,
            Instant from,
            Instant to,
            AuditStatus status
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) {
                predicates.add(builder.equal(root.get("actionType"), action));
            }
            if (userId != null) {
                predicates.add(builder.equal(root.get("userId"), userId));
            }
            if (entityType != null) {
                predicates.add(builder.equal(root.get("entityType"), entityType));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
