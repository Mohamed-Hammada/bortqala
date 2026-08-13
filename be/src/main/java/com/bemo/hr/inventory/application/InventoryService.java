package com.bemo.hr.inventory.application;

import com.bemo.hr.inventory.domain.InventoryReservation;
import com.bemo.hr.inventory.infrastructure.InventoryReservationRepository;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryService(WarehouseRepository warehouseRepository,
                            InventoryReservationRepository reservationRepository) {
        this.warehouseRepository = warehouseRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Warehouse createWarehouse(String branchId, String code, String name, String location) {
        String effectiveBranch = branchId != null ? branchId : "branch-default";
        Warehouse warehouse = new Warehouse(effectiveBranch, code, name, location, true);
        return warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Transactional
    public InventoryReservation reserveStock(String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {
        InventoryReservation reservation = new InventoryReservation(sourceType, sourceId, itemId, warehouseId, quantity);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void releaseReservation(String reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("Reservation not found", "RESERVATION_NOT_FOUND", HttpStatus.NOT_FOUND));
        reservation.release();
        reservationRepository.save(reservation);
    }
}
