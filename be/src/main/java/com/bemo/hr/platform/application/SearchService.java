package com.bemo.hr.platform.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.trade.procurement.domain.PurchaseOrder;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private static final int MAX_RESULTS = 24;

    private final EmployeeRepository employeeRepository;
    private final BusinessPartyRepository partyRepository;
    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProjectRepository projectRepository;
    private final JournalEntryRepository journalEntryRepository;

    public SearchService(EmployeeRepository employeeRepository,
                         BusinessPartyRepository partyRepository,
                         CustomerInvoiceRepository customerInvoiceRepository,
                         PurchaseOrderRepository purchaseOrderRepository,
                         ProjectRepository projectRepository,
                         JournalEntryRepository journalEntryRepository) {
        this.employeeRepository = employeeRepository;
        this.partyRepository = partyRepository;
        this.customerInvoiceRepository = customerInvoiceRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.projectRepository = projectRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    public PlatformApi.SearchResponse search(String query, String appId) {
        String q = query.toLowerCase().trim();
        List<PlatformApi.SearchResultItem> results = new ArrayList<>(MAX_RESULTS);

        if (q.length() < 2) {
            return new PlatformApi.SearchResponse(results);
        }

        addSection(results, "employee", searchEmployees(q), EmployeeSearchAdapter.INSTANCE);
        addSection(results, "party", searchParties(q), PartySearchAdapter.INSTANCE);
        addSection(results, "invoice", searchInvoices(q), InvoiceSearchAdapter.INSTANCE);
        addSection(results, "purchase_order", searchPurchaseOrders(q), PurchaseOrderSearchAdapter.INSTANCE);
        addSection(results, "project", searchProjects(q), ProjectSearchAdapter.INSTANCE);
        addSection(results, "journal", searchJournalEntries(q), JournalSearchAdapter.INSTANCE);

        return new PlatformApi.SearchResponse(results);
    }

    private interface ResultAdapter<T> {
        PlatformApi.SearchResultItem toItem(T entity);
    }

    private enum EmployeeSearchAdapter implements ResultAdapter<Employee> {
        INSTANCE;

        @Override
        public PlatformApi.SearchResultItem toItem(Employee emp) {
            return new PlatformApi.SearchResultItem("employee", emp.getId(), emp.getFullName(), emp.getEmployeeCode(), "/employees");
        }
    }

    private enum PartySearchAdapter implements ResultAdapter<BusinessParty> {
        INSTANCE;

        @Override
        public PlatformApi.SearchResultItem toItem(BusinessParty party) {
            String name = party.getName() != null ? party.getName() : "";
            String code = party.getCode() != null ? party.getCode() : "";
            String type = "SUPPLIER".equalsIgnoreCase(party.getPartyType()) ? "supplier" : "customer";
            return new PlatformApi.SearchResultItem(type, party.getId(), name, code, "/partners/parties");
        }
    }

    private enum InvoiceSearchAdapter implements ResultAdapter<CustomerInvoice> {
        INSTANCE;

        @Override
        public PlatformApi.SearchResultItem toItem(CustomerInvoice inv) {
            String num = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "";
            return new PlatformApi.SearchResultItem("invoice", inv.getId(), "Invoice " + num, "EGP " + inv.getAmount(), "/sales/invoices");
        }
    }

    private enum PurchaseOrderSearchAdapter implements ResultAdapter<PurchaseOrder> {
        INSTANCE;

        @Override
        public PlatformApi.SearchResultItem toItem(PurchaseOrder po) {
            String num = po.getPoNumber() != null ? po.getPoNumber() : "";
            return new PlatformApi.SearchResultItem("purchase_order", po.getId(), "PO " + num, po.getSupplierId(), "/procurement/orders");
        }
    }

    private enum ProjectSearchAdapter implements ResultAdapter<Project> {
        INSTANCE;

        @Override
        public PlatformApi.SearchResultItem toItem(Project prj) {
            String name = prj.getName() != null ? prj.getName() : "";
            String code = prj.getCode() != null ? prj.getCode() : "";
            return new PlatformApi.SearchResultItem("project", prj.getId(), name, code, "/projects");
        }
    }

    private enum JournalSearchAdapter implements ResultAdapter<JournalEntry> {
        INSTANCE;

        @Override
        public PlatformApi.SearchResultItem toItem(JournalEntry j) {
            String num = j.getEntryNumber() != null ? j.getEntryNumber() : "";
            String desc = j.getDescription() != null ? j.getDescription() : "";
            return new PlatformApi.SearchResultItem("journal", j.getId(), "Journal " + num, desc, "/finance/journal-entries");
        }
    }

    private <T> void addSection(List<PlatformApi.SearchResultItem> results, String section, List<T> candidates,
                                ResultAdapter<T> adapter) {
        for (T candidate : candidates) {
            if (results.size() >= MAX_RESULTS) {
                return;
            }
            results.add(adapter.toItem(candidate));
        }
    }

    private List<Employee> searchEmployees(String q) {
        try {
            return employeeRepository.findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc(q, q);
        } catch (RuntimeException e) {
            log.warn("Employee search failed for query '{}'", q, e);
            return List.of();
        }
    }

    private List<BusinessParty> searchParties(String q) {
        try {
            return partyRepository.findTop10ByNameContainingIgnoreCaseOrNameEnContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(q, q, q);
        } catch (RuntimeException e) {
            log.warn("Business party search failed for query '{}'", q, e);
            return List.of();
        }
    }

    private List<CustomerInvoice> searchInvoices(String q) {
        try {
            return customerInvoiceRepository.findTop10ByInvoiceNumberContainingIgnoreCaseOrderByInvoiceDateDesc(q);
        } catch (RuntimeException e) {
            log.warn("Customer invoice search failed for query '{}'", q, e);
            return List.of();
        }
    }

    private List<PurchaseOrder> searchPurchaseOrders(String q) {
        try {
            return purchaseOrderRepository.findTop10ByPoNumberContainingIgnoreCaseOrderByPoDateDesc(q);
        } catch (RuntimeException e) {
            log.warn("Purchase order search failed for query '{}'", q, e);
            return List.of();
        }
    }

    private List<Project> searchProjects(String q) {
        try {
            return projectRepository.findTop10ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(q, q);
        } catch (RuntimeException e) {
            log.warn("Project search failed for query '{}'", q, e);
            return List.of();
        }
    }

    private List<JournalEntry> searchJournalEntries(String q) {
        try {
            return journalEntryRepository.findTop10ByEntryNumberContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByEntryDateDesc(q, q);
        } catch (RuntimeException e) {
            log.warn("Journal entry search failed for query '{}'", q, e);
            return List.of();
        }
    }
}