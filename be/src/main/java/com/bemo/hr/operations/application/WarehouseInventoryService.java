package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WarehouseInventoryService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseBinRepository binRepository;
    private final StockStatusBalanceRepository balanceRepository;
    private final StockReservationRepository reservationRepository;

    public WarehouseInventoryService(WarehouseRepository warehouseRepository,
                                     WarehouseBinRepository binRepository,
                                     StockStatusBalanceRepository balanceRepository,
                                     StockReservationRepository reservationRepository) {
        this.warehouseRepository = warehouseRepository;
        this.binRepository = binRepository;
        this.balanceRepository = balanceRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Warehouse createWarehouse(String branchId, String code, String name, String location) {
        Warehouse warehouse = new Warehouse(branchId, code, name, location, true);
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public WarehouseBin createBin(String warehouseId, String binCode, String aisle, String rack, String shelf) {
        WarehouseBin bin = new WarehouseBin(warehouseId, binCode, aisle, rack, shelf);
        return binRepository.save(bin);
    }

    @Transactional
    public StockReservation reserveStock(String reservationNumber, String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {
        BigDecimal available = getAvailableStock(warehouseId, itemId);
        if (available.compareTo(quantity) < 0) {
            throw new BusinessRuleException("Insufficient available stock for reservation", "INSUFFICIENT_STOCK_RESERVATION", HttpStatus.CONFLICT);
        }
        StockReservation reservation = new StockReservation(reservationNumber, sourceType, sourceId, itemId, warehouseId, quantity);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public StockReservation fulfillReservation(String reservationId) {
        StockReservation reservation = getReservation(reservationId);
        reservation.fulfill();
        return reservationRepository.save(reservation);
    }

    @Transactional
    public StockReservation cancelReservation(String reservationId) {
        StockReservation reservation = getReservation(reservationId);
        reservation.cancel();
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableStock(String warehouseId, String itemId) {
        List<StockStatusBalance> balances = balanceRepository.findByWarehouseIdAndItemId(warehouseId, itemId);
        BigDecimal totalAvailable = balances.stream()
                .filter(b -> b.getStatus() == StockStatusBalance.Status.AVAILABLE)
                .map(StockStatusBalance::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StockReservation> activeReservations = reservationRepository.findByWarehouseIdAndItemIdAndStatus(warehouseId, itemId, StockReservation.Status.ACTIVE);
        BigDecimal totalReserved = activeReservations.stream()
                .map(StockReservation::getReservedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalAvailable.subtract(totalReserved).max(BigDecimal.ZERO);
    }

    @Transactional
    public void receiveAvailableStock(String warehouseId, String itemId, BigDecimal quantity) {
        requirePositive(quantity);
        requireActiveWarehouse(warehouseId);
        StockStatusBalance balance = balanceRepository
                .findByWarehouseIdAndBinIdAndItemIdAndStatus(
                        warehouseId, "", itemId, StockStatusBalance.Status.AVAILABLE)
                .orElseGet(() -> new StockStatusBalance(warehouseId, "", itemId,
                        StockStatusBalance.Status.AVAILABLE, BigDecimal.ZERO));
        balance.adjustQuantity(quantity);
        balanceRepository.save(balance);
    }

    @Transactional
    public void issueAvailableStock(String warehouseId, String itemId, BigDecimal quantity) {
        requirePositive(quantity);
        requireActiveWarehouse(warehouseId);
        if (getAvailableStock(warehouseId, itemId).compareTo(quantity) < 0) {
            throw new BusinessRuleException("Insufficient available warehouse stock.",
                    "WAREHOUSE_STOCK_INSUFFICIENT", HttpStatus.CONFLICT);
        }
        StockStatusBalance balance = balanceRepository
                .findByWarehouseIdAndBinIdAndItemIdAndStatus(
                        warehouseId, "", itemId, StockStatusBalance.Status.AVAILABLE)
                .orElseThrow(() -> new BusinessRuleException("Warehouse stock balance was not found.",
                        "WAREHOUSE_STOCK_BALANCE_NOT_FOUND", HttpStatus.CONFLICT));
        balance.adjustQuantity(quantity.negate());
        balanceRepository.save(balance);
    }

    private void requireActiveWarehouse(String warehouseId) {
        if (warehouseId == null || warehouseId.isBlank()
                || warehouseRepository.findById(warehouseId).filter(Warehouse::isActive).isEmpty()) {
            throw new BusinessRuleException("Select an active warehouse.",
                    "WAREHOUSE_ACTIVE_REQUIRED", HttpStatus.CONFLICT);
        }
    }

    private void requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Warehouse stock quantity must be positive.",
                    "WAREHOUSE_QUANTITY_POSITIVE", HttpStatus.CONFLICT);
        }
    }

    private StockReservation getReservation(String id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Reservation not found", "RESERVATION_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
