package com.bemo.hr.finance.domain.treasury;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PaymentBatchDisbursementRepository extends JpaRepository<PaymentBatchDisbursement,String>{
    List<PaymentBatchDisbursement> findByBatchId(String batchId);
}
