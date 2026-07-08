package com.tripgenie.trip.admin.service;

import com.tripgenie.trip.admin.dto.DashboardSummaryResponse;
import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditStatus;
import com.tripgenie.trip.audit.dto.AuditLogResponse;
import com.tripgenie.trip.audit.repository.AuditLogRepository;
import com.tripgenie.trip.repository.AiItineraryGenerationRepository;
import com.tripgenie.trip.repository.TripRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminDashboardService {
    private final TripRepository tripRepository;
    private final AiItineraryGenerationRepository generationRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardService(
            TripRepository tripRepository,
            AiItineraryGenerationRepository generationRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.tripRepository = tripRepository;
        this.generationRepository = generationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        List<AuditLogResponse> recentFailures = auditLogRepository.findAll(
                        (root, query, builder) -> builder.equal(root.get("status"), AuditStatus.FAILURE),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt")))
                .map(AuditLogResponse::from)
                .toList();
        return new DashboardSummaryResponse(
                tripRepository.count(),
                generationRepository.count(),
                auditLogRepository.countByActionTypeAndStatus(AuditAction.LOCATIONS_ENRICHED, AuditStatus.SUCCESS),
                auditLogRepository.countNotificationsProcessed(),
                recentFailures
        );
    }
}
