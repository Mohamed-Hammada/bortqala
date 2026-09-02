package com.bemo.hr.platform;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.platform.application.SearchService;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTests {

    @Mock EmployeeRepository employeeRepository;
    @Mock BusinessPartyRepository partyRepository;
    @Mock CustomerInvoiceRepository customerInvoiceRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock ProjectRepository projectRepository;
    @Mock JournalEntryRepository journalEntryRepository;

    SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(
                employeeRepository,
                partyRepository,
                customerInvoiceRepository,
                purchaseOrderRepository,
                projectRepository,
                journalEntryRepository
        );
    }

    @Test
    void search_emptyQuery_returnsEmptyAndQueriesNothing() {
        var result = service.search("", "app-1");
        assertTrue(result.results().isEmpty());
        verify(employeeRepository, never()).findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("", "");
    }

    @Test
    void search_shortQuery_returnsEmpty() {
        var result = service.search("a", "app-1");
        assertTrue(result.results().isEmpty());
        verify(employeeRepository, never()).findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("a", "a");
    }

    @Test
    void search_findsEmployeeByMixedCasePassesLowerCaseToDb() {
        Employee emp = employee("EMP-001", "Ahmed Ali");
        when(employeeRepository.findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("ahmed", "ahmed"))
                .thenReturn(List.of(emp));

        var result = service.search("AHMED", "app-1");
        assertEquals(1, result.results().size());
        assertEquals(emp.getId(), result.results().get(0).id());
        assertEquals("employee", result.results().get(0).type());
        verify(employeeRepository).findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("ahmed", "ahmed");
    }

    @Test
    void search_findsEmployeeByCode() {
        Employee emp = employee("EMP-001", "Ahmed Ali");
        when(employeeRepository.findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("emp-001", "emp-001"))
                .thenReturn(List.of(emp));

        var result = service.search("emp-001", "app-1");
        assertEquals(1, result.results().size());
        assertEquals(emp.getId(), result.results().get(0).id());
        assertEquals("EMP-001", result.results().get(0).subtitle());
    }

    @Test
    void search_noResult_returnsEmpty() {
        when(employeeRepository.findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("zzz", "zzz"))
                .thenReturn(List.of());
        when(partyRepository.findTop10ByNameContainingIgnoreCaseOrNameEnContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc("zzz", "zzz", "zzz"))
                .thenReturn(List.of());
        when(customerInvoiceRepository.findTop10ByInvoiceNumberContainingIgnoreCaseOrderByInvoiceDateDesc("zzz"))
                .thenReturn(List.of());
        when(purchaseOrderRepository.findTop10ByPoNumberContainingIgnoreCaseOrderByPoDateDesc("zzz"))
                .thenReturn(List.of());
        when(projectRepository.findTop10ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc("zzz", "zzz"))
                .thenReturn(List.of());
        when(journalEntryRepository.findTop10ByEntryNumberContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByEntryDateDesc("zzz", "zzz"))
                .thenReturn(List.of());

        var result = service.search("zzz", "app-1");
        assertTrue(result.results().isEmpty());
    }

    @Test
    void search_aggregatesSectionsWithinGlobalCap() {
        List<Employee> employees = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> employee("E-" + i, "Emp " + i))
                .toList();
        List<BusinessParty> parties = List.of(party("p1", "PX1", "SUPPLIER"), party("p2", "PX2", "CUSTOMER"));
        when(employeeRepository.findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("xx", "xx"))
                .thenReturn(employees);
        when(partyRepository.findTop10ByNameContainingIgnoreCaseOrNameEnContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc("xx", "xx", "xx"))
                .thenReturn(parties);

        var result = service.search("XX", "app-1");
        assertEquals(12, result.results().size());
    }

    @Test
    void search_trimsQuery() {
        Employee emp = employee("E1", "Ahmed");
        when(employeeRepository.findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc("ahmed", "ahmed"))
                .thenReturn(List.of(emp));

        var result = service.search("  AHMED  ", "app-1");
        assertEquals(1, result.results().size());
    }

    private Employee employee(String code, String name) {
        return new Employee(code, name, null, "c1", com.bemo.hr.employee.domain.EmploymentType.FIXED,
                new java.math.BigDecimal("1000"), java.time.LocalDate.of(2024, 1, 1), null, true);
    }

    private BusinessParty party(String id, String code, String type) {
        return new BusinessParty(code, "Party " + code, "PT-" + code, type,
                null, null, null, null, null, true,
                "DIRECT", null, null, null, "EGP", "STANDARD", "NET_30", null, null);
    }
}
