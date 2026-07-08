package com.tripgenie.trip.admin.service;

import com.tripgenie.trip.admin.dto.DashboardSummaryResponse;
import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditLog;
import com.tripgenie.trip.audit.domain.AuditStatus;
import com.tripgenie.trip.audit.repository.AuditLogRepository;
import com.tripgenie.trip.repository.AiItineraryGenerationRepository;
import com.tripgenie.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {
    @Mock
    private TripRepository tripRepository;
    @Mock
    private AiItineraryGenerationRepository generationRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardService(tripRepository, generationRepository, auditLogRepository);
    }

    @Test
    void summaryReturnsOperationalCountsAndRecentFailures() {
        AuditLog failure = new AuditLog();
        failure.setId(UUID.randomUUID());
        failure.setActionType(AuditAction.ITINERARY_GENERATED);
        failure.setStatus(AuditStatus.FAILURE);
        failure.setOccurredAt(Instant.now());
        when(tripRepository.count()).thenReturn(4L);
        when(generationRepository.count()).thenReturn(3L);
        when(auditLogRepository.countByActionTypeAndStatus(AuditAction.LOCATIONS_ENRICHED, AuditStatus.SUCCESS))
                .thenReturn(2L);
        when(auditLogRepository.countNotificationsProcessed()).thenReturn(5L);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(failure)));

        DashboardSummaryResponse response = service.summary();

        assertThat(response.totalTrips()).isEqualTo(4);
        assertThat(response.totalAiGenerations()).isEqualTo(3);
        assertThat(response.totalLocationEnrichments()).isEqualTo(2);
        assertThat(response.totalNotificationsProcessed()).isEqualTo(5);
        assertThat(response.recentFailures()).hasSize(1);
    }
}
