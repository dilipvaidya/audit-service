package com.dilip.audit_service;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@AutoConfigureMockMvc
@ActiveProfiles("dev")
@SpringBootTest
public class AuditControllerTests {

    @Autowired
    private MockMvc mockMvc;

    // logs
    @Test
    @DisplayName("itShouldReturnAllAuditLogsForAllUsers_whenAdminUserQueryWithoutFilter")
    public void itShouldReturnAllAuditLogsForAllUsers_whenAdminUserQueryWithoutFilter() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value("12345"));
    }

    @Test
    @DisplayName("itShouldReturnAllAuditLogsForNonAdminUsers_whenNonAdminUserQueryWithoutFilter")
    public void itShouldReturnAllAuditLogsForNonAdminUsers_whenNonAdminUserQueryWithoutFilter() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value("67890"));
    }

    // eventId
    @Test
    @DisplayName("itShouldReturnAuditLogsForNonAdminUser_whenNonAdminUserQueryWithEventId")
    public void itShouldReturnAuditLogsForNonAdminUser_whenNonAdminUserQueryWithEventIt() throws Exception {
        mockMvc.perform(get("/api/audit/logs/event/67890")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("67890"))
                .andExpect(jsonPath("$.changedBy.userId").value("user-001"));
    }

    @Test
    @DisplayName("itShouldReturnNotFound_whenNonAdminUserQueryWithEventId")
    public void itShouldReturnNotFound_whenNonAdminUserQueryWithEventId() throws Exception {
        mockMvc.perform(get("/api/audit/logs/event/12345")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("itShouldReturnSingleAuditLog_whenAdminUserQueryByEventId")
    public void ItShouldReturnSingleAuditLog_WhenAdminUserQueryByEventId() throws Exception {
        mockMvc.perform(get("/api/audit/logs/event/{eventId}", "12345")
                .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("12345"))
                .andExpect(jsonPath("$.changedBy.userId").value("admin123"));
    }

    @Test
    @DisplayName("itShouldReturnSingleAuditLog_whenAdminUserQueryByEventId")
    public void ItShouldReturnSingleAuditLog_WhenAdminUserQueryByNonAdminEventId() throws Exception {
        mockMvc.perform(get("/api/audit/logs/event/{eventId}", "67890")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("67890"))
                .andExpect(jsonPath("$.changedBy.userId").value("user-001"));
    }

    // userId
    @Test
    @DisplayName("itShouldReturnAuditLogsForNonAdminUser_whenNonAdminUserQueryWithNonAdminUserId")
    public void itShouldReturnAuditLogsForNonAdminUser_whenNonAdminUserQueryWithNonAdminUserId() throws Exception {
        mockMvc.perform(get("/api/audit/logs/user/user-001")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("67890"))
                .andExpect(jsonPath("$[0].changedBy.userId").value("user-001"));
    }

    @Test
    @DisplayName("itShouldReturnNotFound_whenNonAdminUserQueryWithAdminUserId")
    public void itShouldReturnNotFound_whenNonAdminUserQueryWithAdminUserId() throws Exception {
        mockMvc.perform(get("/api/audit/logs/user/admin123")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ItShouldReturnAllAuditLog_WhenAdminUserQueryByAdminUserId")
    public void ItShouldReturnAllAuditLog_WhenAdminUserQueryByUserId() throws Exception {
        mockMvc.perform(get("/api/audit/logs/user/{userId}", "admin123")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("12345"))
                .andExpect(jsonPath("$[0].changedBy.userId").value("admin123"));
    }

    @Test
    @DisplayName("ItShouldReturnSingleAuditLog_WhenAdminUserQueryByNonAdminUserId")
    public void ItShouldReturnSingleAuditLog_WhenAdminUserQueryByNonAdminUserId() throws Exception {
        mockMvc.perform(get("/api/audit/logs/user/{userId}", "user-001")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("67890"))
                .andExpect(jsonPath("$[0].changedBy.userId").value("user-001"));
    }

    // query
    @Test
    @DisplayName("itShouldReturnAllAuditLogs_whenAdminQueriesWithoutAdvancedFilters")
    public void itShouldReturnAllAuditLogs_whenAdminQueriesWithAdvancedFilters() throws Exception {
        String query = "{}";

        mockMvc.perform(get("/api/audit/query")
                        .with(httpBasic("admin", "adminpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("itShouldReturnAllNonAdminUserAuditLogs_whenNonAdminQueriesWithoutAdvancedFilters")
    public void itShouldReturnAllNonAdminUserAuditLogs_whenNonAdminQueriesWithoutAdvancedFilters() throws Exception {
        String query = "{}";

        mockMvc.perform(get("/api/audit/query")
                        .with(httpBasic("user-001", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("itShouldReturnAllAuditLogs_whenAdminQueriesWithAdvancedFiltersOnAdminUser")
    public void itShouldReturnAllAuditLogs_whenAdminQueriesWithAdvancedFiltersOnAdminUser() throws Exception {
        String query = """
                { "changedByUserId": "admin123" }
                """;

        mockMvc.perform(get("/api/audit/query")
                        .with(httpBasic("admin", "adminpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value("12345"));
    }

    @Test
    @DisplayName("itShouldReturnAllAuditLogs_whenAdminQueriesWithAdvancedFiltersOnNonAdminUser")
    public void itShouldReturnAllAuditLogs_whenAdminQueriesWithAdvancedFiltersOnNonAdminUser() throws Exception {
        String query = """
                { "changedByUserId": "user-001" }
                """;

        mockMvc.perform(get("/api/audit/query")
                        .with(httpBasic("admin", "adminpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value("67890"));
    }

    @Test
    @DisplayName("itShouldReturnAllAuditLogs_whenAdminQueriesWithAdvancedFilters_EntityTypeEventTypeDateRangeAndChangedBy")
    public void itShouldReturnAllAuditLogs_whenAdminQueriesWithAdvancedFilters_EntityTypeEventTypeDateRangeAndChangedBy()
            throws Exception {
        String query = """
      {
        "entityType": "User",
        "eventType": "UserCreated",
        "startTime": "2024-05-05T00:00:00Z",
        "endTime": "2024-05-05T23:59:59Z",
        "changedByUserId": "user-001"
      }
    """;

        mockMvc.perform(get("/api/audit/query")
                        .with(httpBasic("admin", "adminpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("itShouldReturnUsersAuditLogs_whenNonAdminUserQueriesWithAdvancedFilters_EntityTypeEventTypeDateRangeAndChangedBy")
    public void itShouldReturnUsersAuditLogs_whenNonAdminUserQueriesWithAdvancedFilters_EntityTypeEventTypeDateRangeAndChangedBy() throws Exception {
        String query = """
      {
        "entityType": "User",
        "eventType": "UserCreated",
        "startTime": "2024-05-05T00:00:00Z",
        "endTime": "2024-05-05T23:59:59Z",
        "changedByUserId": "user-001"
      }
    """;

        mockMvc.perform(get("/api/audit/query")
                        .with(httpBasic("user-001", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("itShouldReturnForbidden_whenNonAdminUserQueriesAuditLogsChangedByAdminWithAdvancedFilter" +
            "_EntityTypeEventTypeDateRangeAndChangedBy")
    public void itShouldReturnForbidden_whenNonAdminUserQueriesAuditLogsChangedByAdminWithAdvancedFilter_EntityTypeEventTypeDateRangeAndChangedBy() throws Exception {
        String query = """
      {
        "entityType": "User",
        "eventType": "UserCreated",
        "startTime": "2024-05-05T00:00:00Z",
        "endTime": "2024-05-05T23:59:59Z",
        "changedByUserId": "admin123"
      }
    """;

        mockMvc.perform(get("/api/audit/query")
                        .with(httpBasic("user-001", "userpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isForbidden());
    }


    // Delete not found
    @Test
    @DisplayName("itShouldReturnNotFoundError_whenAdminUserDeletesUnavailableAuditLog")
    public void itShouldReturnNotFoundError_whenAdminUserDeletesUnavailableAuditLog() throws Exception {
        mockMvc.perform(delete("/api/audit/event/{eventId}", "5647")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("itShouldReturnNotFoundError_whenNonAdminUserDeletesUnavailableAuditLog")
    public void itShouldReturnNotFoundError_whenNonAdminUserDeletesUnavailableAuditLog() throws Exception {
        mockMvc.perform(delete("/api/audit/logs/event/{eventId}", "5647")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isNotFound());
    }

    // Delete non admin unauthorised
    @Test
    @DisplayName("itShouldReturnNotFoundStatus_whenNonAdminUserDeletesAnotherUsersAuditLog")
    public void itShouldReturnUnauthorizedError_whenNonAdminUserDeletesAnotherUsersAuditLog() throws Exception {
        mockMvc.perform(delete("/api/audit/logs/event/{eventId}", "12345")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isForbidden());
    }

    // Delete admin success
    @Test
    @DisplayName("itShouldDeleteAuditLog_whenAdminUserDeletesSelfAuditLog")
    public void itShouldDeleteAuditLog_whenAdminUserDeletesSelfAuditLog() throws Exception {
        mockMvc.perform(delete("/api/audit/logs/event/{eventId}", "12345")
                        .with(httpBasic("admin", "adminpass")))
                .andDo(print())
                .andExpect(status().isOk());
                //.andExpect(jsonPath("$.message").value("Deleted successfully"));;
    }

//    @Test
//    @WithMockUser(username = "admin", roles = {"ROLE_ADMIN"})
//    public void itShouldDeleteAuditLog_withMockAdminUser() throws Exception {
//        mockMvc.perform(delete("/api/audit/logs/event/{eventId}", "12345"))
//                .andExpect(status().isOk());
//                //.andExpect(jsonPath("$.message").value("Deleted successfully"));
//    }

    @Test
    @DisplayName("itShouldDeleteAuditLog_whenAdminUserDeletesAnotherUsersAuditLog")
    public void itShouldDeleteAuditLog_whenAdminUserDeletesAnotherUsersAuditLog() throws Exception {
        mockMvc.perform(delete("/api/audit/logs/event/{eventId}", "67890")
                        .with(httpBasic("admin", "adminpass")))
                .andExpect(status().isOk());
                //.andExpect(jsonPath("$.message").value("Deleted successfully"));
    }

    // Delete non admin success
    @Test
    @DisplayName("itShouldDeleteAuditLog_whenNonAdminUserDeletesSelfAuditLog")
    public void itShouldDeleteAuditLog_whenNonAdminUserDeletesSelfAuditLog() throws Exception {
        mockMvc.perform(delete("/api/audit/logs/event/{eventId}", "67890")
                        .with(httpBasic("user-001", "userpass")))
                .andExpect(status().isOk());
                //.andExpect(jsonPath("$.message").value("Deleted successfully"));
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