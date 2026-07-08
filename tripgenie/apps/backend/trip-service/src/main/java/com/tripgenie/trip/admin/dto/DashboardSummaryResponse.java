package com.tripgenie.trip.admin.dto;

import com.tripgenie.trip.audit.dto.AuditLogResponse;

import java.util.List;

public record DashboardSummaryResponse(
        long totalTrips,
        long totalAiGenerations,
        long totalLocationEnrichments,
        long totalNotificationsProcessed,
        List<AuditLogResponse> recentFailures
) {
}
