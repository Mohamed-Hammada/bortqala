package com.bemo.hr.trade.retail.laptop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SerializedDeviceRepository extends JpaRepository<SerializedDevice, String> {
    Optional<SerializedDevice> findBySerialNumber(String serialNumber);
    boolean existsBySerialNumber(String serialNumber);
    List<SerializedDevice> findByStatusOrderByCreatedAtDesc(String status);
    List<SerializedDevice> findByBrandIgnoreCaseOrderByCreatedAtDesc(String brand);
    List<SerializedDevice> findByCustomerIdOrderBySaleDateDesc(String customerId);
}
