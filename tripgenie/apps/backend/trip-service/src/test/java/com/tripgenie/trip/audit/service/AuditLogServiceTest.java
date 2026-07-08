package com.tripgenie.trip.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditEntityType;
import com.tripgenie.trip.audit.domain.AuditLog;
import com.tripgenie.trip.audit.domain.AuditStatus;
import com.tripgenie.trip.audit.repository.AuditLogRepository;
import com.tripgenie.trip.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {
    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository, new ObjectMapper());
    }

    @Test
    void recordSuccessPersistsAuditLogWithCorrelationIdAndMetadata() {
        UUID userId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        MDC.put("correlationId", "corr-123");
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.recordSuccess(userId, AuditAction.TRIP_CREATED, AuditEntityType.TRIP, tripId,
                Map.of("destination", "Tokyo"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getUserId()).isEqualTo(userId);
        assertThat(auditLog.getActionType()).isEqualTo(AuditAction.TRIP_CREATED);
        assertThat(auditLog.getEntityType()).isEqualTo(AuditEntityType.TRIP);
        assertThat(auditLog.getEntityId()).isEqualTo(tripId);
        assertThat(auditLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(auditLog.getCorrelationId()).isEqualTo("corr-123");
        assertThat(auditLog.getMetadata()).contains("\"destination\":\"Tokyo\"");
        MDC.clear();
    }

    @Test
    void findAuditLogsReturnsPagedResponsesForFilters() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setActionType(AuditAction.TRIP_UPDATED);
        auditLog.setEntityType(AuditEntityType.TRIP);
        auditLog.setStatus(AuditStatus.SUCCESS);
        auditLog.setOccurredAt(Instant.parse("2026-07-07T12:00:00Z"));
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findAll(any(Specification.class), org.mockito.Mockito.eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));

        PageResponse<?> response = auditLogService.findAuditLogs(
                AuditAction.TRIP_UPDATED,
                UUID.randomUUID(),
                AuditEntityType.TRIP,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-08T00:00:00Z"),
                AuditStatus.SUCCESS,
                pageable
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(auditLogRepository).findAll(any(Specification.class), org.mockito.Mockito.eq(pageable));
    }

    @Test
    void findAuditLogsRejectsInvalidDateRange() {
        PageRequest pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> auditLogService.findAuditLogs(null, null, null,
                Instant.parse("2026-07-08T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"),
                null,
                pageable))
                .isInstanceOf(BusinessException.class);
    }
}
