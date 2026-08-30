package com.bemo.hr.trade.retail.laptop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepairTicketRepository extends JpaRepository<DeviceRepairTicket, String> {
    Optional<DeviceRepairTicket> findByTicketNumber(String ticketNumber);
    List<DeviceRepairTicket> findBySerialNumberOrderByCreatedAtDesc(String serialNumber);
    List<DeviceRepairTicket> findByStatusOrderByCreatedAtDesc(String status);
}
