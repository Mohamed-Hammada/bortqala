package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.*;
import com.bemo.hr.operations.infrastructure.*;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.audit.application.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class WarehouseInventoryService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseBinRepository binRepository;
    private final StockStatusBalanceRepository balanceRepository;
    private final StockReservationRepository reservationRepository;
    private final AuditService auditService;
    private final BranchRepository branchRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public WarehouseInventoryService(WarehouseRepository warehouseRepository,
                                     WarehouseBinRepository binRepository,
                                     StockStatusBalanceRepository balanceRepository,
                                     StockReservationRepository reservationRepository,
                                     AuditService auditService,
                                     BranchRepository branchRepository,
                                     InventoryItemRepository inventoryItemRepository) {
        this.warehouseRepository = warehouseRepository;
        this.binRepository = binRepository;
        this.balanceRepository = balanceRepository;
        this.reservationRepository = reservationRepository;
        this.auditService = auditService;
        this.branchRepository = branchRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional
    public Warehouse createWarehouse(String branchId, String code, String name, String location) {
        if (branchId == null || branchId.isBlank()
                || branchRepository.findById(branchId).filter(branch -> branch.isActive()).isEmpty()) {
            throw new BusinessRuleException("Select an active branch.",
                    "WAREHOUSE_BRANCH_REQUIRED", HttpStatus.CONFLICT);
        }
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
        if (itemId == null || itemId.isBlank()
                || inventoryItemRepository.findById(itemId).filter(com.bemo.hr.operations.InventoryItem::isActive).isEmpty()) {
            throw new BusinessRuleException("Select an active inventory item.",
                    "WAREHOUSE_ITEM_ACTIVE_REQUIRED", HttpStatus.CONFLICT);
        }
        List<StockStatusBalance> balances = balanceRepository.findByWarehouseIdAndItemIdForUpdate(warehouseId, itemId);
        StockReservation replay = reservationRepository
                .findBySourceTypeAndSourceIdAndItemIdAndWarehouseId(sourceType, sourceId, itemId, warehouseId)
                .orElse(null);
        if (replay != null) {
            if (replay.getReservedQuantity().compareTo(quantity) != 0) {
                throw new BusinessRuleException("A reservation replay must use the original quantity.",
                        "RESERVATION_REPLAY_CONFLICT", HttpStatus.CONFLICT);
            }
            return replay;
        }
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
        consumeAvailableBalances(balances, reservation.getReservedQuantity());
        reservation.fulfill();
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
        List<StockStatusBalance> balances = balanceRepository.findByWarehouseIdAndItemIdForUpdate(warehouseId, itemId);
        if (availableStock(warehouseId, itemId, balances).compareTo(quantity) < 0) {
            throw new BusinessRuleException("Insufficient available warehouse stock.",
                    "WAREHOUSE_STOCK_INSUFFICIENT", HttpStatus.CONFLICT);
        }
        consumeAvailableBalances(balances, quantity);
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

    private void consumeAvailableBalances(List<StockStatusBalance> balances, BigDecimal quantity) {
        BigDecimal remaining = quantity;
        List<StockStatusBalance> availableBalances = balances.stream()
                .filter(row -> row.getStatus() == StockStatusBalance.Status.AVAILABLE)
                .filter(row -> row.getQuantity().signum() > 0)
                .sorted(Comparator.comparing(StockStatusBalance::getBinId))
                .toList();
        for (StockStatusBalance balance : availableBalances) {
            if (remaining.signum() == 0) break;
            BigDecimal consumed = balance.getQuantity().min(remaining);
            balance.adjustQuantity(consumed.negate());
            balanceRepository.save(balance);
            remaining = remaining.subtract(consumed);
        }
        if (remaining.signum() > 0) {
            throw new BusinessRuleException("Insufficient physical stock across warehouse bins.",
                    "WAREHOUSE_STOCK_INSUFFICIENT", HttpStatus.CONFLICT);
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
