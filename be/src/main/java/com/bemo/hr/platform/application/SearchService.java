package com.bemo.hr.platform.application;

import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.application.HrConfigurationService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final HrConfigurationService hrConfigurationService;
    private final BusinessPartyRepository partyRepository;
    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProjectRepository projectRepository;
    private final JournalEntryRepository journalEntryRepository;

    public SearchService(HrConfigurationService hrConfigurationService,
                         BusinessPartyRepository partyRepository,
                         CustomerInvoiceRepository customerInvoiceRepository,
                         PurchaseOrderRepository purchaseOrderRepository,
                         ProjectRepository projectRepository,
                         JournalEntryRepository journalEntryRepository) {
        this.hrConfigurationService = hrConfigurationService;
        this.partyRepository = partyRepository;
        this.customerInvoiceRepository = customerInvoiceRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.projectRepository = projectRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    public PlatformApi.SearchResponse search(String query, String appId) {
        String q = query.toLowerCase().trim();
        List<PlatformApi.SearchResultItem> results = new ArrayList<>();

        if (q.length() < 2) {
            return new PlatformApi.SearchResponse(results);
        }

        // 1. Employees
        try {
            List<EmployeeApi.Response> employees = hrConfigurationService.listEmployees();
            for (EmployeeApi.Response emp : employees) {
                if (emp.fullName().toLowerCase().contains(q) || emp.employeeCode().toLowerCase().contains(q)) {
                    results.add(new PlatformApi.SearchResultItem("employee", emp.id(), emp.fullName(), emp.employeeCode(), "/employees"));
                    if (results.size() >= 5) break;
                }
            }
        } catch (Exception ignored) {}

        // 2. Business Parties (Customers & Suppliers)
        try {
            List<BusinessParty> parties = partyRepository.findAll();
            for (BusinessParty party : parties) {
                String name = party.getName() != null ? party.getName() : "";
                String nameEn = party.getNameEn() != null ? party.getNameEn() : "";
                String code = party.getCode() != null ? party.getCode() : "";
                if (name.toLowerCase().contains(q) || nameEn.toLowerCase().contains(q) || code.toLowerCase().contains(q)) {
                    String type = "SUPPLIER".equalsIgnoreCase(party.getPartyType()) ? "supplier" : "customer";
                    results.add(new PlatformApi.SearchResultItem(type, party.getId(), name, code, "/partners/parties"));
                    if (results.size() >= 10) break;
                }
            }
        } catch (Exception ignored) {}

        // 3. Sales Invoices
        try {
            List<CustomerInvoice> invoices = customerInvoiceRepository.findAll();
            for (CustomerInvoice inv : invoices) {
                String num = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "";
                if (num.toLowerCase().contains(q)) {
                    results.add(new PlatformApi.SearchResultItem("invoice", inv.getId(), "Invoice " + num, "EGP " + inv.getAmount(), "/sales/invoices"));
                    if (results.size() >= 14) break;
                }
            }
        } catch (Exception ignored) {}

        // 4. Purchase Orders
        try {
            List<PurchaseOrder> pos = purchaseOrderRepository.findAll();
            for (PurchaseOrder po : pos) {
                String num = po.getPoNumber() != null ? po.getPoNumber() : "";
                if (num.toLowerCase().contains(q)) {
                    results.add(new PlatformApi.SearchResultItem("purchase_order", po.getId(), "PO " + num, po.getSupplierId(), "/procurement/orders"));
                    if (results.size() >= 17) break;
                }
            }
        } catch (Exception ignored) {}

        // 5. Projects
        try {
            List<Project> projects = projectRepository.findAll();
            for (Project prj : projects) {
                String name = prj.getName() != null ? prj.getName() : "";
                String code = prj.getCode() != null ? prj.getCode() : "";
                if (name.toLowerCase().contains(q) || code.toLowerCase().contains(q)) {
                    results.add(new PlatformApi.SearchResultItem("project", prj.getId(), name, code, "/projects"));
                    if (results.size() >= 20) break;
                }
            }
        } catch (Exception ignored) {}

        // 6. Journal Entries
        try {
            List<JournalEntry> journals = journalEntryRepository.findAll();
            for (JournalEntry j : journals) {
                String num = j.getEntryNumber() != null ? j.getEntryNumber() : "";
                String desc = j.getDescription() != null ? j.getDescription() : "";
                if (num.toLowerCase().contains(q) || desc.toLowerCase().contains(q)) {
                    results.add(new PlatformApi.SearchResultItem("journal", j.getId(), "Journal " + num, desc, "/finance/journal-entries"));
                    if (results.size() >= 23) break;
                }
            }
        } catch (Exception ignored) {}

        return new PlatformApi.SearchResponse(results);
    }
}
