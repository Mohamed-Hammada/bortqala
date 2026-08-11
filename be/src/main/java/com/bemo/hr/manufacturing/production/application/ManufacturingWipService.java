package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.MaterialReservationHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialReservationLine;
import com.bemo.hr.manufacturing.production.domain.WipPostingRecord;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialReservationHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialReservationLineRepository;
import com.bemo.hr.manufacturing.production.infrastructure.WipPostingRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
        MaterialReservationHeader header = new MaterialReservationHeader(workOrderId);
        return reservationHeaderRepository.save(header);
    }

    @Transactional
    public MaterialReservationLine addReservationLine(String reservationId, String itemId, BigDecimal reservedQuantity) {
        MaterialReservationLine line = new MaterialReservationLine(reservationId, itemId, reservedQuantity);
        return reservationLineRepository.save(line);
    }

    @Transactional
    public WipPostingRecord postWip(String workOrderId, String workCenterId, BigDecimal laborHours, BigDecimal machineHours, BigDecimal totalWipCost) {
        WipPostingRecord record = new WipPostingRecord(workOrderId, workCenterId, laborHours, machineHours, totalWipCost);
        return wipPostingRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<WipPostingRecord> getWipPostings(String workOrderId) {
        return wipPostingRepository.findByWorkOrderId(workOrderId);
    }
}
