package com.dilip.audit_service;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("shouldRestrictAuditLogsForNonAdminUser")
    public void shouldRestrictAuditLogsForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/audit/logs/event/67890")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("67890"))
                .andExpect(jsonPath("$.changedBy.userId").value("user-001"));
    }

    @Test
    @DisplayName("shouldReturnSingleAuditLogById")
    public void shouldReturnSingleAuditLogById() throws Exception {
        mockMvc.perform(get("/api/audit/logs/event/{eventId}", "12345")
                .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("12345"))
                .andExpect(jsonPath("$.changedBy.userId").value("admin123"));
    }

    @Test
    @DisplayName("shouldSupportAdvancedAuditSearch")
    public void shouldSupportAdvancedAuditSearch() throws Exception {
        String query = """
      {
        "entityType": "User",
        "eventType": "UserCreated",
        "startTime": "2024-05-05T00:00:00Z",
        "endTime": "2024-05-05T23:59:59Z",
        "changedByUserId": "user-001"
      }
    """;

        mockMvc.perform(post("/api/audit/query")
                        .with(httpBasic("admin", "adminpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("shouldSupportAdvancedAuditSearch")
    public void shouldSupportAdvancedAuditSearch_WithNonAdminUser() throws Exception {
        String query = """
      {
        "entityType": "User",
        "eventType": "UserCreated",
        "startTime": "2024-05-05T00:00:00Z",
        "endTime": "2024-05-05T23:59:59Z",
        "changedByUserId": "user-001"
      }
    """;

        mockMvc.perform(post("/api/audit/query")
                        .with(httpBasic("user-001", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("shouldSupportAdvancedAuditSearch")
    public void shouldFailAdvancedAuditSearch_WithNonAdminUserForAdminLogs() throws Exception {
        String query = """
      {
        "entityType": "User",
        "eventType": "UserCreated",
        "startTime": "2024-05-05T00:00:00Z",
        "endTime": "2024-05-05T23:59:59Z",
        "changedByUserId": "admin123"
      }
    """;

        mockMvc.perform(post("/api/audit/query")
                        .with(httpBasic("user-001", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isForbidden());
    }
}

/* old format tests
package com.example.audit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testWriteAuditLog() throws Exception {
        AuditLog log = new AuditLog();
        log.setEventId("e1");
        log.setTimestamp(Instant.now());

        mockMvc.perform(post("/api/audit/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(log)))
                .andExpect(status().isCreated());

        Mockito.verify(auditService).saveAuditLog(Mockito.any(AuditLog.class));
    }

    @Test
    void testGetAuditLogById_found() throws Exception {
        AuditLog log = new AuditLog();
        log.setEventId("e1");
        Mockito.when(auditService.findById("e1")).thenReturn(Optional.of(log));

        mockMvc.perform(get("/api/audit/logs/e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("e1"));
    }

    @Test
    void testGetAuditLogById_notFound() throws Exception {
        Mockito.when(auditService.findById("e1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/audit/logs/e1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAdvancedSearch() throws Exception {
        AuditSearchRequest request = new AuditSearchRequest();
        request.setEntityType("user");

        AuditLog result = new AuditLog();
        result.setEventId("e1");

        Mockito.when(auditService.advancedSearch(Mockito.any()))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/audit/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("e1"));
    }
}
 */