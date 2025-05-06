package com.dilip.audit_service;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.ArgumentMatchers.contains;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@AutoConfigureMockMvc
@SpringBootTest
public class AuditControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("shouldReturnAuditLogsForAdminUser")
    public void shouldReturnAuditLogsForAdminUser() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].changedBy.userId").value(contains("user-001")));
    }

    @Test
    @DisplayName("shouldRestrictAuditLogsForNonAdminUser")
    public void shouldRestrictAuditLogsForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].changedBy.userId").value(contains("user-001")));
    }

    @Test
    @DisplayName("shouldReturnSingleAuditLogById")
    public void shouldReturnSingleAuditLogById() throws Exception {
        mockMvc.perform(get("/api/audit/logs/{eventId}", "some-event-uuid")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("some-event-uuid"));
    }

    @Test
    @DisplayName("shouldSupportAdvancedAuditSearch")
    public void shouldSupportAdvancedAuditSearch() throws Exception {
        String query = """
      {
        "entityType": "Order",
        "eventType": "ORDER_UPDATED",
        "timeRange": {
          "from": "2024-05-01T00:00:00Z",
          "to": "2024-05-15T23:59:59Z"
        },
        "changedBy": {
          "userId": "user-001"
        }
      }
    """;

        mockMvc.perform(post("/api/audit/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
