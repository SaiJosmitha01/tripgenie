package com.tripgenie.trip.admin.controller;

import com.tripgenie.common.api.ApiResponse;
import com.tripgenie.trip.admin.dto.DashboardSummaryResponse;
import com.tripgenie.trip.admin.service.AdminDashboardService;
import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditEntityType;
import com.tripgenie.trip.audit.domain.AuditStatus;
import com.tripgenie.trip.audit.dto.AuditLogResponse;
import com.tripgenie.trip.audit.service.AuditLogService;
import com.tripgenie.trip.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminController {
    private final AuditLogService auditLogService;
    private final AdminDashboardService adminDashboardService;

    public AdminController(AuditLogService auditLogService, AdminDashboardService adminDashboardService) {
        this.auditLogService = auditLogService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List audit logs")
    ApiResponse<PageResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return ApiResponse.success("Audit logs retrieved",
                auditLogService.findAuditLogs(action, userId, entityType, from, to, status, pageable));
    }

    @GetMapping("/audit-logs/{id}")
    @Operation(summary = "Get an audit log by id")
    ApiResponse<AuditLogResponse> getAuditLog(@PathVariable UUID id) {
        return ApiResponse.success("Audit log retrieved", auditLogService.getAuditLog(id));
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get operational dashboard summary")
    ApiResponse<DashboardSummaryResponse> dashboardSummary() {
        return ApiResponse.success("Dashboard summary retrieved", adminDashboardService.summary());
    }
}
