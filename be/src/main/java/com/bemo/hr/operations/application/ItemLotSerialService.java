package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.ItemLotSerial;
import com.bemo.hr.operations.infrastructure.ItemLotSerialRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ItemLotSerialService {

    private final ItemLotSerialRepository repository;

    public ItemLotSerialService(ItemLotSerialRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ItemLotSerial createLotSerial(String itemId, String lotNumber, String serialNumber, LocalDate expirationDate, LocalDate manufactureDate) {
        log.debug("createLotSerial called with itemId={}, lotNumber={}, serialNumber={}", itemId, lotNumber, serialNumber);
        validateIdentity(lotNumber, serialNumber);
        rejectDuplicateSerial(serialNumber);
        ItemLotSerial item = new ItemLotSerial(itemId, lotNumber, serialNumber, expirationDate, manufactureDate);
        ItemLotSerial saved = repository.save(item);
        log.info("ItemLotSerial {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public ItemLotSerial receive(String itemId, String warehouseId, String lotNumber, String serialNumber,
                                 BigDecimal quantity, String receiptReference, LocalDate expirationDate, LocalDate manufactureDate) {
        log.debug("receive called with itemId={}, warehouseId={}, lotNumber={}, quantity={}", itemId, warehouseId, lotNumber, quantity);
        validateIdentity(lotNumber, serialNumber);
        rejectDuplicateSerial(serialNumber);
        if (quantity == null || quantity.signum() <= 0 || (serialNumber != null && !serialNumber.isBlank() && quantity.compareTo(BigDecimal.ONE) != 0)) {
            throw new BusinessRuleException("Lot/serial quantity is invalid.", "LOT_SERIAL_QUANTITY_INVALID", HttpStatus.CONFLICT);
        }
        if (serialNumber == null || serialNumber.isBlank()) {
            ItemLotSerial existing = repository.findByItemIdAndWarehouseIdAndLotNumberIgnoreCase(itemId, warehouseId, lotNumber).orElse(null);
            if (existing != null) {
                existing.receiveReturn(quantity, receiptReference);
                return repository.save(existing);
            }
        }
        return repository.save(new ItemLotSerial(itemId, warehouseId, lotNumber, serialNumber, quantity,
                receiptReference, expirationDate, manufactureDate));
    }

    @Transactional
    public ItemLotSerial issue(String id, BigDecimal quantity, String documentReference) {
        log.debug("issue called with id={}, quantity={}, documentReference={}", id, quantity, documentReference);
        ItemLotSerial item = getItem(id);
        try {
            item.issue(quantity, documentReference);
        } catch (IllegalArgumentException | IllegalStateException error) {
            log.warn("Lot/serial issue failed for id={}: {}", id, error.getMessage());
            throw new BusinessRuleException(error.getMessage(), "LOT_SERIAL_ISSUE_INVALID", HttpStatus.CONFLICT);
        }
        ItemLotSerial saved = repository.save(item);
        log.info("ItemLotSerial {} issued successfully, quantity={}", id, quantity);
        return saved;
    }

    @Transactional
    public ItemLotSerial receiveReturn(String id, BigDecimal quantity, String documentReference) {
        log.debug("receiveReturn called with id={}, quantity={}", id, quantity);
        ItemLotSerial item = getItem(id);
        try {
            item.receiveReturn(quantity, documentReference);
        } catch (IllegalArgumentException error) {
            log.warn("Lot/serial return failed for id={}: {}", id, error.getMessage());
            throw new BusinessRuleException(error.getMessage(), "LOT_SERIAL_RETURN_INVALID", HttpStatus.CONFLICT);
        }
        ItemLotSerial saved = repository.save(item);
        log.info("ItemLotSerial {} return received, quantity={}", id, quantity);
        return saved;
    }

    @Transactional
    public ItemLotSerial quarantine(String id) {
        log.debug("quarantine called with id={}", id);
        ItemLotSerial item = getItem(id);
        item.quarantine();
        ItemLotSerial saved = repository.save(item);
        log.info("ItemLotSerial {} quarantined successfully", id);
        return saved;
    }

    @Transactional
    public ItemLotSerial block(String id) {
        log.debug("block called with id={}", id);
        ItemLotSerial item = getItem(id);
        item.block();
        ItemLotSerial saved = repository.save(item);
        log.info("ItemLotSerial {} blocked successfully", id);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ItemLotSerial> getAvailableLotsByItem(String itemId) {
        return repository.findByItemIdAndStatus(itemId, ItemLotSerial.Status.AVAILABLE);
    }

    /**
     * FEFO (First-Expired, First-Out) picking: auto-issue from the earliest-expiring lots.
     * Blocks expired lots. Returns the list of lots that were partially or fully issued.
     * Throws if total available quantity across all valid lots is insufficient.
     */
    @Transactional
    public List<ItemLotSerial> pickFefo(String itemId, String warehouseId, BigDecimal requiredQty, String documentReference) {
        log.debug("pickFefo called with itemId={}, warehouseId={}, requiredQty={}", itemId, warehouseId, requiredQty);
        if (requiredQty == null || requiredQty.signum() <= 0) {
            throw new BusinessRuleException("Required quantity must be positive.", "LOT_SERIAL_PICK_QTY_INVALID", HttpStatus.CONFLICT);
        }

        // Mark any expired lots first
        List<ItemLotSerial> allAvailable = repository.findFifoLots(itemId, warehouseId);
        LocalDate today = LocalDate.now();
        for (ItemLotSerial lot : allAvailable) {
            lot.checkExpired(today);
            if (lot.getStatus() != ItemLotSerial.Status.AVAILABLE) {
                repository.save(lot);
            }
        }

        // Get FEFO-sorted lots (earliest expiry first)
        List<ItemLotSerial> fefoLots = repository.findFefoLots(itemId, warehouseId);
        BigDecimal remaining = requiredQty;
        List<ItemLotSerial> issuedLots = new ArrayList<>();

        for (ItemLotSerial lot : fefoLots) {
            if (lot.getStatus() != ItemLotSerial.Status.AVAILABLE) {
                continue;
            }
            if (remaining.signum() <= 0) break;
            BigDecimal toIssue = remaining.min(lot.getQuantity());
            lot.issue(toIssue, documentReference);
            repository.save(lot);
            issuedLots.add(lot);
            remaining = remaining.subtract(toIssue);
        }

        if (remaining.signum() > 0) {
            throw new BusinessRuleException(
                "Insufficient stock for FEFO pick. Short by " + remaining + " units.",
                "LOT_SERIAL_INSUFFICIENT_STOCK_FEFO", HttpStatus.CONFLICT);
        }

        log.info("FEFO pick completed: issued {} lots, total={}", issuedLots.size(), requiredQty);
        return issuedLots;
    }

    /**
     * FIFO (First-In, First-Out) picking: auto-issue from the oldest lots.
     */
    @Transactional
    public List<ItemLotSerial> pickFifo(String itemId, String warehouseId, BigDecimal requiredQty, String documentReference) {
        log.debug("pickFifo called with itemId={}, warehouseId={}, requiredQty={}", itemId, warehouseId, requiredQty);
        if (requiredQty == null || requiredQty.signum() <= 0) {
            throw new BusinessRuleException("Required quantity must be positive.", "LOT_SERIAL_PICK_QTY_INVALID", HttpStatus.CONFLICT);
        }

        List<ItemLotSerial> fifoLots = repository.findFifoLots(itemId, warehouseId);
        BigDecimal remaining = requiredQty;
        List<ItemLotSerial> issuedLots = new ArrayList<>();

        for (ItemLotSerial lot : fifoLots) {
            if (remaining.signum() <= 0) break;
            BigDecimal toIssue = remaining.min(lot.getQuantity());
            lot.issue(toIssue, documentReference);
            repository.save(lot);
            issuedLots.add(lot);
            remaining = remaining.subtract(toIssue);
        }

        if (remaining.signum() > 0) {
            throw new BusinessRuleException(
                "Insufficient stock for FIFO pick. Short by " + remaining + " units.",
                "LOT_SERIAL_INSUFFICIENT_STOCK_FIFO", HttpStatus.CONFLICT);
        }

        log.info("FIFO pick completed: issued {} lots, total={}", issuedLots.size(), requiredQty);
        return issuedLots;
    }

    /**
     * Returns lots expiring within the given number of days (for expiry warnings).
     */
    @Transactional(readOnly = true)
    public List<ItemLotSerial> getLotsExpiringWithinDays(String itemId, String warehouseId, int days) {
        return repository.findLotsExpiringWithinDays(itemId, warehouseId, java.time.LocalDate.now().plusDays(days));
    }

    private ItemLotSerial getItem(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Lot/Serial record not found", "LOT_SERIAL_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ItemLotSerial trace(String id) {
        return getItem(id);
    }

    private void rejectDuplicateSerial(String serialNumber) {
        if (serialNumber != null && !serialNumber.isBlank() && repository.findBySerialNumberIgnoreCase(serialNumber.strip()).isPresent()) {
            throw new BusinessRuleException("Serial number already exists.", "LOT_SERIAL_DUPLICATE", HttpStatus.CONFLICT);
        }
    }

    private void validateIdentity(String lotNumber, String serialNumber) {
        if ((lotNumber == null || lotNumber.isBlank()) && (serialNumber == null || serialNumber.isBlank())) {
            throw new BusinessRuleException("A lot or serial number is required.", "LOT_SERIAL_IDENTITY_REQUIRED", HttpStatus.CONFLICT);
        }
    }
}
