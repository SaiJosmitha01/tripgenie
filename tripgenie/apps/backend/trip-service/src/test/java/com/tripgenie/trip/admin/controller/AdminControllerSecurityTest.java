package com.tripgenie.trip.admin.controller;

import com.tripgenie.trip.admin.dto.DashboardSummaryResponse;
import com.tripgenie.trip.admin.service.AdminDashboardService;
import com.tripgenie.trip.audit.service.AuditLogService;
import com.tripgenie.trip.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private AdminDashboardService adminDashboardService;
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(roles = "USER")
    void userRoleCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanAccessAdminEndpoint() throws Exception {
        when(adminDashboardService.summary()).thenReturn(new DashboardSummaryResponse(1, 2, 3, 4, List.of()));

        mockMvc.perform(get("/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTrips").value(1))
                .andExpect(jsonPath("$.data.totalAiGenerations").value(2));
    }
}
