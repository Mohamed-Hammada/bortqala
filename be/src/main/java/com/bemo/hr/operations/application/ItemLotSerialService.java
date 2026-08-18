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
