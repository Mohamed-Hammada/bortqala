package com.bemo.hr.platform;

import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.platform.application.BulkUpdateService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BulkUpdateServiceTests {

    BulkUpdateService service;

    @BeforeEach
    void setUp() {
        service = new BulkUpdateService();
    }

    @Test
    void execute_validEmploymentTypeUpdate_returnsSuccess() {
        var req = new PlatformApi.BulkUpdateRequest("employee", "employmentType", "DAILY", java.util.List.of("e1", "e2"));
        var result = service.execute("app-1", req, "admin");
        assertEquals(2, result.results().size());
        assertTrue(result.results().get(0).success());
        assertTrue(result.results().get(1).success());
    }

    @Test
    void execute_validActiveUpdate_returnsSuccess() {
        var req = new PlatformApi.BulkUpdateRequest("employee", "active", "false", java.util.List.of("e1"));
        var result = service.execute("app-1", req, "admin");
        assertEquals(1, result.results().size());
        assertTrue(result.results().get(0).success());
    }

    @Test
    void execute_emptyIds_throws() {
        var req = new PlatformApi.BulkUpdateRequest("employee", "active", "true", java.util.List.of());
        assertThrows(BusinessRuleException.class, () -> service.execute("app-1", req, "admin"));
    }

    @Test
    void execute_invalidEntityType_throws() {
        var req = new PlatformApi.BulkUpdateRequest("invoice", "status", "PAID", java.util.List.of("i1"));
        assertThrows(BusinessRuleException.class, () -> service.execute("app-1", req, "admin"));
    }

    @Test
    void execute_invalidField_throws() {
        var req = new PlatformApi.BulkUpdateRequest("employee", "salary", "5000", java.util.List.of("e1"));
        assertThrows(BusinessRuleException.class, () -> service.execute("app-1", req, "admin"));
    }

    @Test
    void execute_invalidValue_returnsFailure() {
        var req = new PlatformApi.BulkUpdateRequest("employee", "employmentType", "CONTRACTOR", java.util.List.of("e1"));
        var result = service.execute("app-1", req, "admin");
        assertEquals(1, result.results().size());
        assertFalse(result.results().get(0).success());
        assertNotNull(result.results().get(0).error());
    }
}
