package com.bemo.hr.trade.sales.infrastructure;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.*;
public interface CustomerInvoiceRepository extends JpaRepository<CustomerInvoice,String>{
    List<CustomerInvoice> findAllByOrderByInvoiceDateDescCreatedAtDesc();
    List<CustomerInvoice> findByCustomerIdOrderByDueDateAsc(String customerId);
    boolean existsByInvoiceNumberIgnoreCase(String number);
    default BigDecimal outstanding(String customerId){return findByCustomerIdOrderByDueDateAsc(customerId).stream()
            .filter(i->i.getStatus()==CustomerInvoice.Status.OPEN||i.getStatus()==CustomerInvoice.Status.PARTIALLY_PAID)
            .map(CustomerInvoice::getOutstandingAmount).reduce(BigDecimal.ZERO,BigDecimal::add);}
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from CustomerInvoice i where i.id in :ids") List<CustomerInvoice> findAllByIdForUpdate(@Param("ids") Collection<String> ids);
}
