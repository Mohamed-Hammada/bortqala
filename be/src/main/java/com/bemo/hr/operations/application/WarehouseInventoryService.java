package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.audit.application.AuditService;
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
    private final AuditService auditService;

    public WarehouseInventoryService(WarehouseRepository warehouseRepository,
                                     WarehouseBinRepository binRepository,
                                     StockStatusBalanceRepository balanceRepository,
                                     StockReservationRepository reservationRepository,
                                     AuditService auditService) {
        this.warehouseRepository = warehouseRepository;
        this.binRepository = binRepository;
        this.balanceRepository = balanceRepository;
        this.reservationRepository = reservationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Warehouse createWarehouse(String branchId, String code, String name, String location) {
        Warehouse warehouse = new Warehouse(branchId, code, name, location, true);
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public WarehouseBin createBin(String warehouseId, String binCode, String aisle, String rack, String shelf) {
        requireActiveWarehouse(warehouseId);
        if (binCode == null || binCode.isBlank()) {
            throw new BusinessRuleException("Bin code is required.", "WAREHOUSE_BIN_CODE_REQUIRED", HttpStatus.CONFLICT);
        }
        if (binRepository.findByWarehouseIdAndBinCodeIgnoreCase(warehouseId, binCode.strip()).isPresent()) {
            throw new BusinessRuleException("Bin code already exists in this warehouse.", "WAREHOUSE_BIN_CODE_EXISTS", HttpStatus.CONFLICT);
        }
        WarehouseBin bin = new WarehouseBin(warehouseId, binCode, aisle, rack, shelf);
        return binRepository.save(bin);
    }

    @Transactional(readOnly = true)
    public List<WarehouseBin> bins(String warehouseId) {
        requireActiveWarehouse(warehouseId);
        return binRepository.findByWarehouseId(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<StockStatusBalance> balances(String warehouseId) {
        requireActiveWarehouse(warehouseId);
        return balanceRepository.findByWarehouseId(warehouseId);
    }

    @Transactional
    public void moveStatus(String warehouseId, String binId, String itemId, StockStatusBalance.Status from,
                           StockStatusBalance.Status to, BigDecimal quantity, String actor) {
        requirePositive(quantity);
        requireLocation(warehouseId, binId);
        if (from == null || to == null || from == to) {
            throw new BusinessRuleException("Select two different stock statuses.", "WAREHOUSE_STATUS_CHANGE_INVALID", HttpStatus.CONFLICT);
        }
        List<StockStatusBalance> locked = balanceRepository.findByWarehouseIdAndItemIdForUpdate(warehouseId, itemId);
        String normalizedBin = binId == null ? "" : binId;
        StockStatusBalance source = locked.stream()
                .filter(row -> row.getBinId().equals(normalizedBin) && row.getStatus() == from)
                .findFirst().orElseThrow(() -> new BusinessRuleException("Source stock status balance was not found.",
                        "WAREHOUSE_STOCK_BALANCE_NOT_FOUND", HttpStatus.CONFLICT));
        if (source.getQuantity().compareTo(quantity) < 0) {
            throw new BusinessRuleException("Insufficient quantity in the source stock status.",
                    "WAREHOUSE_STATUS_STOCK_INSUFFICIENT", HttpStatus.CONFLICT);
        }
        StockStatusBalance target = locked.stream()
                .filter(row -> row.getBinId().equals(normalizedBin) && row.getStatus() == to)
                .findFirst().orElseGet(() -> new StockStatusBalance(warehouseId, normalizedBin, itemId, to, BigDecimal.ZERO));
        source.adjustQuantity(quantity.negate());
        target.adjustQuantity(quantity);
        balanceRepository.save(source);
        balanceRepository.save(target);
        auditService.record("STOCK_STATUS_CHANGED", "STOCK_STATUS_BALANCE", source.getId(), actor,
                "{\"warehouseId\":\"" + warehouseId + "\",\"binId\":\"" + normalizedBin
                        + "\",\"itemId\":\"" + itemId + "\",\"from\":\"" + from
                        + "\",\"to\":\"" + to + "\",\"quantity\":" + quantity + "}", null);
    }

    @Transactional
    public StockReservation reserveStock(String reservationNumber, String sourceType, String sourceId, String itemId, String warehouseId, BigDecimal quantity) {
        requirePositive(quantity);
        requireActiveWarehouse(warehouseId);
        StockReservation replay = reservationRepository
                .findBySourceTypeAndSourceIdAndItemIdAndWarehouseId(sourceType, sourceId, itemId, warehouseId)
                .orElse(null);
        if (replay != null) return replay;
        List<StockStatusBalance> balances = balanceRepository.findByWarehouseIdAndItemIdForUpdate(warehouseId, itemId);
        BigDecimal available = availableStock(warehouseId, itemId, balances);
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
        return availableStock(warehouseId, itemId, balances);
    }

    @Transactional
    public StockReservation expireReservation(String reservationId) {
        StockReservation reservation = getReservation(reservationId);
        reservation.expire();
        return reservationRepository.save(reservation);
    }

    @Transactional
    public StockReservation consumeReservation(String reservationId) {
        StockReservation reservation = getReservation(reservationId);
        if (reservation.getStatus() == StockReservation.Status.FULFILLED) return reservation;
        List<StockStatusBalance> balances = balanceRepository.findByWarehouseIdAndItemIdForUpdate(
                reservation.getWarehouseId(), reservation.getItemId());
        StockStatusBalance available = balances.stream()
                .filter(row -> row.getStatus() == StockStatusBalance.Status.AVAILABLE)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Warehouse stock balance was not found.",
                        "WAREHOUSE_STOCK_BALANCE_NOT_FOUND", HttpStatus.CONFLICT));
        if (available.getQuantity().compareTo(reservation.getReservedQuantity()) < 0) {
            throw new BusinessRuleException("Insufficient physical stock for the reserved delivery.",
                    "WAREHOUSE_STOCK_INSUFFICIENT", HttpStatus.CONFLICT);
        }
        reservation.fulfill();
        available.adjustQuantity(reservation.getReservedQuantity().negate());
        balanceRepository.save(available);
        return reservationRepository.save(reservation);
    }

    private BigDecimal availableStock(String warehouseId, String itemId, List<StockStatusBalance> balances) {
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

    @Transactional(readOnly = true)
    public List<StockReservation> reservationsForSource(String sourceType, String sourceId) {
        return reservationRepository.findBySourceTypeAndSourceIdOrderByCreatedAtAsc(sourceType, sourceId);
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

    @Transactional(readOnly = true)
    public BigDecimal getPhysicalStock(String warehouseId, String itemId) {
        requireActiveWarehouse(warehouseId);
        return balanceRepository.findByWarehouseIdAndItemId(warehouseId, itemId).stream()
                .map(StockStatusBalance::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void adjustAvailableStock(String warehouseId, String itemId, BigDecimal delta) {
        if (delta == null || delta.signum() == 0) return;
        if (delta.signum() > 0) receiveAvailableStock(warehouseId, itemId, delta);
        else issueAvailableStock(warehouseId, itemId, delta.abs());
    }

    private void requireLocation(String warehouseId, String binId) {
        requireActiveWarehouse(warehouseId);
        if (binId != null && !binId.isBlank()
                && binRepository.findById(binId).filter(bin -> bin.isActive() && bin.getWarehouseId().equals(warehouseId)).isEmpty()) {
            throw new BusinessRuleException("The bin does not belong to the selected warehouse.",
                    "WAREHOUSE_BIN_MISMATCH", HttpStatus.CONFLICT);
        }
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
