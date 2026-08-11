package com.bemo.hr.operations.application;

import com.bemo.hr.operations.domain.ItemLotSerial;
import com.bemo.hr.operations.infrastructure.ItemLotSerialRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ItemLotSerialService {

    private final ItemLotSerialRepository repository;

    public ItemLotSerialService(ItemLotSerialRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ItemLotSerial createLotSerial(String itemId, String lotNumber, String serialNumber, LocalDate expirationDate, LocalDate manufactureDate) {
        ItemLotSerial item = new ItemLotSerial(itemId, lotNumber, serialNumber, expirationDate, manufactureDate);
        return repository.save(item);
    }

    @Transactional
    public ItemLotSerial quarantine(String id) {
        ItemLotSerial item = getItem(id);
        item.quarantine();
        return repository.save(item);
    }

    @Transactional
    public ItemLotSerial block(String id) {
        ItemLotSerial item = getItem(id);
        item.block();
        return repository.save(item);
    }

    @Transactional(readOnly = true)
    public List<ItemLotSerial> getAvailableLotsByItem(String itemId) {
        return repository.findByItemIdAndStatus(itemId, ItemLotSerial.Status.AVAILABLE);
    }

    private ItemLotSerial getItem(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Lot/Serial record not found", "LOT_SERIAL_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
