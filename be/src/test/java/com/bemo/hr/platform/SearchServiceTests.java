package com.bemo.hr.platform;

import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.application.HrConfigurationService;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.platform.application.SearchService;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTests {

    @Mock HrConfigurationService hrConfigurationService;
    @Mock BusinessPartyRepository partyRepository;
    @Mock CustomerInvoiceRepository customerInvoiceRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock ProjectRepository projectRepository;
    @Mock JournalEntryRepository journalEntryRepository;

    SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(
                hrConfigurationService,
                partyRepository,
                customerInvoiceRepository,
                purchaseOrderRepository,
                projectRepository,
                journalEntryRepository
        );
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
        assertEquals("e1", result.results().get(0).id());
    }
}
