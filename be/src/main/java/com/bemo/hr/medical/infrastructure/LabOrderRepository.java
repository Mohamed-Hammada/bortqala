package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, String> {

    Optional<LabOrder> findByAppIdAndId(String appId, String id);

    List<LabOrder> findAllByAppIdAndPatientIdOrderByOrderedAtDesc(String appId, String patientId);

    List<LabOrder> findAllByAppIdAndPatientIdAndStatusOrderByOrderedAtDesc(String appId, String patientId, LabOrder.Status status);

    List<LabOrder> findAllByAppIdAndStatusOrderByOrderedAtDesc(String appId, LabOrder.Status status);

    List<LabOrder> findAllByAppIdOrderByOrderedAtDesc(String appId);

    @Query("SELECT o FROM LabOrder o WHERE o.appId = :appId AND o.status = 'SENT_OUT' AND o.sentOutAt <= :thresholdEpoch ORDER BY o.sentOutAt ASC")
    List<LabOrder> findAgingSentOutOrders(@Param("appId") String appId, @Param("thresholdEpoch") long thresholdEpoch);
}
