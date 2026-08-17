package com.bemo.hr.audit.api;

import com.bemo.hr.audit.domain.AuditLog;
import com.bemo.hr.audit.infrastructure.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTests {

    private MockMvc mockMvc;

    @Mock
    private AuditLogRepository repository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(repository)).build();
    }

    @Test
    void listWithNoFiltersDelegatesToSearchWithNulls() throws Exception {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(repository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "N/A");

        mockMvc.perform(get("/api/v1/audit-logs").principal(auth))
                .andExpect(status().isOk());

        verify(repository).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class));
    }

    @Test
    void listWithAllFiltersDelegatesWithStrippedValues() throws Exception {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(repository.search(eq("EMPLOYEE"), eq("CREATE"), eq("qa"), eq("QA-EMP-RETEST-0808"),
                eq(1700000000000L), eq(1800000000000L), any(PageRequest.class)))
                .thenReturn(page);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "N/A");

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("entityType", " EMPLOYEE ")
                        .param("action", "CREATE")
                        .param("username", " qa ")
                        .param("search", "QA-EMP-RETEST-0808")
                        .param("from", "1700000000000")
                        .param("to", "1800000000000")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(repository).search(eq("EMPLOYEE"), eq("CREATE"), eq("qa"), eq("QA-EMP-RETEST-0808"),
                eq(1700000000000L), eq(1800000000000L), any(PageRequest.class));
    }

    @Test
    void blankFiltersAreTreatedAsAbsent() throws Exception {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(repository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "N/A");

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("entityType", "   ")
                        .param("search", "")
                        .principal(auth))
                .andExpect(status().isOk());

        verify(repository).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class));
    }

    @Test
    void responseMapsAuditLogFields() throws Exception {
        AuditLog log = new AuditLog("CREATE", "EMPLOYEE", "emp-1", "qa", "{\"code\":\"QA-EMP-RETEST-0808\"}", "10.0.0.1");
        when(repository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "N/A");

        mockMvc.perform(get("/api/v1/audit-logs").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.content[0].entityType").value("EMPLOYEE"))
                .andExpect(jsonPath("$.content[0].entityId").value("emp-1"))
                .andExpect(jsonPath("$.content[0].username").value("qa"))
                .andExpect(jsonPath("$.content[0].detailsJson").value("{\"code\":\"QA-EMP-RETEST-0808\"}"))
                .andExpect(jsonPath("$.content[0].ipAddress").value("10.0.0.1"));
    }
}
