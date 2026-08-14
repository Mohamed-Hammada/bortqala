package com.bemo.hr.trade.sales.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.*;
import com.bemo.hr.party.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.api.SalesApi;
import com.bemo.hr.trade.sales.domain.*;
import com.bemo.hr.trade.sales.infrastructure.*;
import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesReceivablesServiceTests {
    @Mock CustomerCreditProfileRepository creditRepository; @Mock CustomerInvoiceRepository invoiceRepository;
    @Mock CustomerReceiptRepository receiptRepository; @Mock CustomerReceiptAllocationRepository allocationRepository;
    @Mock CustomerCreditNoteRepository creditNoteRepository;
    @Mock CollectionTaskRepository taskRepository; @Mock BusinessPartyRepository partyRepository;
    @Mock PartnerLedgerEntryRepository ledgerRepository; @Mock AuditService auditService;
    @Mock SubledgerPostingService subledgerPostingService;
    SalesReceivablesService service; BusinessParty customer;

    @BeforeEach void setup(){service=new SalesReceivablesService(creditRepository,invoiceRepository,receiptRepository,allocationRepository,creditNoteRepository,taskRepository,partyRepository,ledgerRepository,auditService,subledgerPostingService);
        customer=new BusinessParty("C-1","Customer",null,"PROCESSING_CUSTOMER",null,null,null,null,null,true,"DIRECT",null,null,null,"EGP","PER_DELIVERY","NET_30",null,null);
        lenient().when(partyRepository.findById(customer.getId())).thenReturn(Optional.of(customer));lenient().when(invoiceRepository.save(any())).thenAnswer(i->i.getArgument(0));
        lenient().when(receiptRepository.save(any())).thenAnswer(i->i.getArgument(0));lenient().when(allocationRepository.save(any())).thenAnswer(i->i.getArgument(0));
        lenient().when(ledgerRepository.save(any())).thenAnswer(i->i.getArgument(0));lenient().when(allocationRepository.findByReceiptId(anyString())).thenReturn(List.of());}

    @Test void creditLimitBlocksAdditionalExposure(){CustomerCreditProfile profile=new CustomerCreditProfile(customer.getId());profile.update(new BigDecimal("1000"),30,false);
        when(creditRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(profile));when(invoiceRepository.outstanding(customer.getId())).thenReturn(new BigDecimal("900"));
        assertThatThrownBy(()->service.assertCreditAvailable(customer.getId(),new BigDecimal("200"))).isInstanceOfSatisfying(BusinessRuleException.class,e->assertThat(e.getCode()).isEqualTo("AR_CREDIT_LIMIT_EXCEEDED"));}

    @Test void receiptAllocatesPartiallyAndPostsCustomerCredit(){CustomerInvoice invoice=openInvoice("INV-1",new BigDecimal("500"),LocalDate.now().minusDays(10));
        when(receiptRepository.findByOperationId("op-1")).thenReturn(Optional.empty());when(receiptRepository.existsByReceiptNumberIgnoreCase("RC-1")).thenReturn(false);
        when(invoiceRepository.findAllByIdForUpdate(List.of(invoice.getId()))).thenReturn(List.of(invoice));
        SalesApi.ReceiptResponse response=service.recordReceipt(new SalesApi.ReceiptRequest("RC-1",customer.getId(),ms(LocalDate.now()),"EGP",new BigDecimal("300"),"op-1",List.of(new SalesApi.AllocationRequest(invoice.getId(),new BigDecimal("200")))),"user");
        assertThat(invoice.getOutstandingAmount()).isEqualByComparingTo("300");assertThat(response.unallocatedAmount()).isEqualByComparingTo("100");
        ArgumentCaptor<PartnerLedgerEntry> ledger=ArgumentCaptor.forClass(PartnerLedgerEntry.class);verify(ledgerRepository).save(ledger.capture());assertThat(ledger.getValue().getAmountDelta()).isEqualByComparingTo("-300");}

    @Test void receiptOperationReplayDoesNotWriteAgain(){CustomerReceipt receipt=new CustomerReceipt("RC-1",customer.getId(),LocalDate.now(),"EGP",new BigDecimal("100"),"same-op","user");
        when(receiptRepository.findByOperationId("same-op")).thenReturn(Optional.of(receipt));service.recordReceipt(new SalesApi.ReceiptRequest("RC-OTHER",customer.getId(),ms(LocalDate.now()),"EGP",new BigDecimal("100"),"same-op",List.of()),"user");
        verify(receiptRepository,never()).save(any());verify(ledgerRepository,never()).save(any());}

    @Test void receiptAllocatesAcrossTwoCustomerInvoices(){CustomerInvoice first=openInvoice("I-1",new BigDecimal("100"),LocalDate.now());CustomerInvoice second=openInvoice("I-2",new BigDecimal("80"),LocalDate.now());
        when(receiptRepository.findByOperationId("multi-op")).thenReturn(Optional.empty());when(invoiceRepository.findAllByIdForUpdate(List.of(first.getId(),second.getId()))).thenReturn(List.of(first,second));
        service.recordReceipt(new SalesApi.ReceiptRequest("RC-M",customer.getId(),ms(LocalDate.now()),"EGP",new BigDecimal("150"),"multi-op",List.of(new SalesApi.AllocationRequest(first.getId(),new BigDecimal("100")),new SalesApi.AllocationRequest(second.getId(),new BigDecimal("50")))),"user");
        assertThat(first.getOutstandingAmount()).isZero();assertThat(second.getOutstandingAmount()).isEqualByComparingTo("30");verify(allocationRepository).saveAll(argThat(rows->{int count=0;for(CustomerReceiptAllocation ignored:rows)count++;return count==2;}));}

    @Test void agingUsesOutstandingOnlyAcrossDeterministicBuckets(){LocalDate asOf=LocalDate.of(2026,8,31);CustomerInvoice current=openInvoice("I-0",new BigDecimal("10"),asOf.plusDays(1));CustomerInvoice d20=openInvoice("I-20",new BigDecimal("20"),asOf.minusDays(20));CustomerInvoice d70=openInvoice("I-70",new BigDecimal("70"),asOf.minusDays(70));
        when(invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc()).thenReturn(List.of(current,d20,d70));SalesApi.AgingResponse result=service.aging(ms(asOf));
        assertThat(result.current()).isEqualByComparingTo("10");assertThat(result.days1To30()).isEqualByComparingTo("20");assertThat(result.days61To90()).isEqualByComparingTo("70");assertThat(result.total()).isEqualByComparingTo("100");}

    @Test void agingRequiresExplicitBusinessDate(){assertThatThrownBy(()->service.aging(0)).isInstanceOfSatisfying(BusinessRuleException.class,e->assertThat(e.getCode()).isEqualTo("AR_AS_OF_DATE_REQUIRED"));}

    @Test void overdueInvoiceCreatesCollectionTask(){LocalDate asOf=LocalDate.of(2026,8,31);CustomerInvoice invoice=openInvoice("OVERDUE",new BigDecimal("150"),asOf.minusDays(12));CollectionTask task=new CollectionTask(invoice.getId());
        when(invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc()).thenReturn(List.of(invoice));when(taskRepository.findByInvoiceId(invoice.getId())).thenReturn(Optional.empty());when(taskRepository.save(any())).thenReturn(task);when(taskRepository.findAllByOrderByNextActionDateAscCreatedAtAsc()).thenReturn(List.of(task));
        List<SalesApi.CollectionTaskResponse> rows=service.collections(asOf);assertThat(rows).singleElement().satisfies(row->{assertThat(row.daysOverdue()).isEqualTo(12);assertThat(row.outstandingAmount()).isEqualByComparingTo("150");});}

    private CustomerInvoice openInvoice(String number,BigDecimal amount,LocalDate due){CustomerInvoice invoice=new CustomerInvoice(number,customer.getId(),null,due.minusDays(30),due,"EGP",amount);invoice.issue("user");return invoice;}
    private static long ms(LocalDate date){return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();}
}
