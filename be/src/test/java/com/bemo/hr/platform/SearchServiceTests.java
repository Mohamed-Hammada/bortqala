package com.bemo.hr.platform;

import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.application.HrConfigurationService;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.platform.application.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTests {

    @Mock HrConfigurationService hrConfigurationService;
    SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(hrConfigurationService);
    }

    @Test
    void search_emptyQuery_returnsEmpty() {
        var result = service.search("", "app-1");
        assertTrue(result.results().isEmpty());
    }

    @Test
    void search_shortQuery_returnsEmpty() {
        var result = service.search("a", "app-1");
        assertTrue(result.results().isEmpty());
    }

    @Test
    void search_findsByName() {
        when(hrConfigurationService.listEmployees()).thenReturn(List.of(
                new EmployeeApi.Response("e1", "EMP-001", "Ahmed Ali", null, "c1", "Admin", EmploymentType.FIXED, null, null, null, true, 1),
                new EmployeeApi.Response("e2", "EMP-002", "Mohamed Hassan", null, "c1", "Admin", EmploymentType.FIXED, null, null, null, true, 1)
        ));

        var result = service.search("ahmed", "app-1");
        assertEquals(1, result.results().size());
        assertEquals("e1", result.results().get(0).id());
        assertEquals("employee", result.results().get(0).type());
    }

    @Test
    void search_findsByCode() {
        when(hrConfigurationService.listEmployees()).thenReturn(List.of(
                new EmployeeApi.Response("e1", "EMP-001", "Ahmed Ali", null, "c1", "Admin", EmploymentType.FIXED, null, null, null, true, 1)
        ));

        var result = service.search("emp-001", "app-1");
        assertEquals(1, result.results().size());
        assertEquals("EMP-001", result.results().get(0).subtitle());
    }

    @Test
    void search_noMatch_returnsEmpty() {
        when(hrConfigurationService.listEmployees()).thenReturn(List.of(
                new EmployeeApi.Response("e1", "EMP-001", "Ahmed Ali", null, "c1", "Admin", EmploymentType.FIXED, null, null, null, true, 1)
        ));

        var result = service.search("xyz", "app-1");
        assertTrue(result.results().isEmpty());
    }
}
