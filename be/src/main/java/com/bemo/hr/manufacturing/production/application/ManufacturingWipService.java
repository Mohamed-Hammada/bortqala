package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.MaterialReservationHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialReservationLine;
import com.bemo.hr.manufacturing.production.domain.WipPostingRecord;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialReservationHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialReservationLineRepository;
import com.bemo.hr.manufacturing.production.infrastructure.WipPostingRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class ManufacturingWipService {

    private final MaterialReservationHeaderRepository reservationHeaderRepository;
    private final MaterialReservationLineRepository reservationLineRepository;
    private final WipPostingRecordRepository wipPostingRepository;

    public ManufacturingWipService(MaterialReservationHeaderRepository reservationHeaderRepository,
                                   MaterialReservationLineRepository reservationLineRepository,
                                   WipPostingRecordRepository wipPostingRepository) {
        this.reservationHeaderRepository = reservationHeaderRepository;
        this.reservationLineRepository = reservationLineRepository;
        this.wipPostingRepository = wipPostingRepository;
    }

    @Transactional
    public MaterialReservationHeader createReservation(String workOrderId) {
        log.debug("createReservation called with workOrderId={}", workOrderId);
        MaterialReservationHeader header = new MaterialReservationHeader(workOrderId);
        MaterialReservationHeader saved = reservationHeaderRepository.save(header);
        log.info("MaterialReservationHeader {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public MaterialReservationLine addReservationLine(String reservationId, String itemId, BigDecimal reservedQuantity) {
        log.debug("addReservationLine called with reservationId={}, itemId={}", reservationId, itemId);
        MaterialReservationLine line = new MaterialReservationLine(reservationId, itemId, reservedQuantity);
        MaterialReservationLine saved = reservationLineRepository.save(line);
        log.info("MaterialReservationLine {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public WipPostingRecord postWip(String workOrderId, String workCenterId, BigDecimal laborHours, BigDecimal machineHours, BigDecimal totalWipCost) {
        log.debug("postWip called with workOrderId={}, workCenterId={}", workOrderId, workCenterId);
        WipPostingRecord record = new WipPostingRecord(workOrderId, workCenterId, laborHours, machineHours, totalWipCost);
        WipPostingRecord saved = wipPostingRepository.save(record);
        log.info("WipPostingRecord {} created successfully", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WipPostingRecord> getWipPostings(String workOrderId) {
        log.debug("getWipPostings called with workOrderId={}", workOrderId);
        return wipPostingRepository.findByWorkOrderId(workOrderId);
    }
}
