package com.bemo.hr.trade.export.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.export.api.ExportShipmentApi;
import com.bemo.hr.trade.export.domain.*;
import com.bemo.hr.trade.export.infrastructure.ComplianceRegisterRepository;
import com.bemo.hr.trade.export.infrastructure.ExportShipmentRepository;
import com.bemo.hr.trade.export.infrastructure.PesticideRegisterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExportShipmentService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ExportShipmentRepository shipmentRepository;
    private final ComplianceRegisterRepository complianceRegisterRepository;
    private final PesticideRegisterRepository pesticideRegisterRepository;

    public ExportShipmentService(ExportShipmentRepository shipmentRepository,
                                 ComplianceRegisterRepository complianceRegisterRepository,
                                 PesticideRegisterRepository pesticideRegisterRepository) {
        this.shipmentRepository = shipmentRepository;
        this.complianceRegisterRepository = complianceRegisterRepository;
        this.pesticideRegisterRepository = pesticideRegisterRepository;
    }

    // ─── Shipment CRUD ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExportShipmentApi.ShipmentResponse> listShipments() {
        return shipmentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toShipmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportShipmentApi.ShipmentResponse getShipment(String id) {
        ExportShipment shipment = findShipment(id);
        return toShipmentResponse(shipment);
    }

    @Transactional
    public ExportShipmentApi.ShipmentResponse createShipment(ExportShipmentApi.ShipmentPayload payload) {
        String appId = TenantContext.require();
        String number = generateShipmentNumber();

        ExportShipment shipment = new ExportShipment(number, payload.customerPartyId(), payload.customerPartyName());
        shipment.setContractRef(payload.contractRef());
        shipment.setContainerNo(payload.containerNo());
        shipment.setBookingNo(payload.bookingNo());
        shipment.setAcidNo(payload.acidNo());
        shipment.setPortOfLoading(payload.portOfLoading());
        shipment.setPortOfDischarge(payload.portOfDischarge());
        shipment.setEtbDate(payload.etbDate());
        shipment.setEtaDate(payload.etaDate());
        shipment.setNotes(payload.notes());
        shipment.setExpectedFxAmount(payload.expectedFxAmount());
        shipment.setExpectedFxCurrency(payload.expectedFxCurrency());

        if (payload.lines() != null) {
            for (ExportShipmentApi.ShipmentLinePayload lp : payload.lines()) {
                ExportShipmentLine line = new ExportShipmentLine(lp.lineOrder(), lp.itemName(), lp.quantity());
                line.setItemCode(lp.itemCode());
                line.setLotReference(lp.lotReference());
                line.setUnitOfMeasure(lp.unitOfMeasure());
                line.setNetWeightKg(lp.netWeightKg());
                line.setGrossWeightKg(lp.grossWeightKg());
                line.setPackagesCount(lp.packagesCount());
                shipment.addLine(line);
            }
        }

        shipment = shipmentRepository.save(shipment);
        log.info("Created export shipment {} for party {}", number, payload.customerPartyId());
        return toShipmentResponse(shipment);
    }

    @Transactional
    public ExportShipmentApi.ShipmentResponse updateShipment(String id, ExportShipmentApi.ShipmentPayload payload) {
        ExportShipment shipment = findShipment(id);
        if (shipment.getStatus() != ExportShipmentStatus.PREPARING) {
            throw new BusinessRuleException(
                    "Can only update shipments in PREPARING status",
                    "EXPORT_SHIPMENT_INVALID_STATE", HttpStatus.CONFLICT);
        }

        shipment.setCustomerPartyId(payload.customerPartyId());
        shipment.setCustomerPartyName(payload.customerPartyName());
        shipment.setContractRef(payload.contractRef());
        shipment.setContainerNo(payload.containerNo());
        shipment.setBookingNo(payload.bookingNo());
        shipment.setAcidNo(payload.acidNo());
        shipment.setPortOfLoading(payload.portOfLoading());
        shipment.setPortOfDischarge(payload.portOfDischarge());
        shipment.setEtbDate(payload.etbDate());
        shipment.setEtaDate(payload.etaDate());
        shipment.setNotes(payload.notes());
        shipment.setExpectedFxAmount(payload.expectedFxAmount());
        shipment.setExpectedFxCurrency(payload.expectedFxCurrency());

        shipment.getLines().clear();
        if (payload.lines() != null) {
            for (ExportShipmentApi.ShipmentLinePayload lp : payload.lines()) {
                ExportShipmentLine line = new ExportShipmentLine(lp.lineOrder(), lp.itemName(), lp.quantity());
                line.setItemCode(lp.itemCode());
                line.setLotReference(lp.lotReference());
                line.setUnitOfMeasure(lp.unitOfMeasure());
                line.setNetWeightKg(lp.netWeightKg());
                line.setGrossWeightKg(lp.grossWeightKg());
                line.setPackagesCount(lp.packagesCount());
                shipment.addLine(line);
            }
        }

        shipment = shipmentRepository.save(shipment);
        return toShipmentResponse(shipment);
    }

    @Transactional
    public ExportShipmentApi.ShipmentResponse transitionShipment(String id, String targetStatus) {
        ExportShipment shipment = findShipment(id);
        ExportShipmentStatus target = ExportShipmentStatus.valueOf(targetStatus.strip().toUpperCase());
        shipment.transitionTo(target);
        shipment = shipmentRepository.save(shipment);
        log.info("Shipment {} transitioned to {}", shipment.getShipmentNumber(), target);
        return toShipmentResponse(shipment);
    }

    // ─── Treatment Logs / Compliance ─────────────────────────────────

    @Transactional
    public ExportShipmentApi.TreatmentLogResponse createTreatmentLog(ExportShipmentApi.TreatmentLogPayload payload) {
        ComplianceRegister log = new ComplianceRegister(
                payload.lotReference(), payload.chemical(),
                Instant.ofEpochMilli(payload.treatmentDate()).atZone(ZoneOffset.UTC).toLocalDate(),
                payload.preHarvestIntervalDays());
        log.setDose(payload.dose());
        log.setTreatedBy(payload.treatedBy());
        log.setNotes(payload.notes());
        log = complianceRegisterRepository.save(log);
        return toTreatmentResponse(log);
    }

    @Transactional(readOnly = true)
    public List<ExportShipmentApi.TreatmentLogResponse> listTreatmentLogs(String lotReference) {
        return complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc(lotReference).stream()
                .map(this::toTreatmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportShipmentApi.ComplianceCheckResponse checkCompliance(List<String> lotReferences, LocalDate pickupDate) {
        List<ExportShipmentApi.ViolationResponse> violations = new ArrayList<>();
        int checked = 0;

        for (String lotRef : lotReferences) {
            List<ComplianceRegister> logs = complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc(lotRef);
            if (logs.isEmpty()) continue;
            checked++;

            for (ComplianceRegister log : logs) {
                if (log.isViolation(pickupDate)) {
                    violations.add(new ExportShipmentApi.ViolationResponse(
                            lotRef,
                            log.getChemical(),
                            log.getTreatmentDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                            log.earliestSafePickup().format(ISO_DATE),
                            log.getPreHarvestIntervalDays(),
                            log.daysUntilSafe(pickupDate)));
                }
            }
        }

        return new ExportShipmentApi.ComplianceCheckResponse(
                violations, checked, checked - (violations.isEmpty() ? 0 : 1));
    }

    // ─── Pesticide Register CRUD ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExportShipmentApi.PesticideResponse> listPesticides() {
        return pesticideRegisterRepository.findAllByOrderByChemicalNameAsc().stream()
                .map(this::toPesticideResponse)
                .toList();
    }

    @Transactional
    public ExportShipmentApi.PesticideResponse createPesticide(ExportShipmentApi.PesticidePayload payload) {
        if (pesticideRegisterRepository.existsByChemicalNameIgnoreCase(payload.chemicalName())) {
            throw new BusinessRuleException(
                    "Pesticide already registered: " + payload.chemicalName(),
                    "PESTICIDE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
        PesticideRegister reg = new PesticideRegister(payload.chemicalName());
        reg.setActiveIngredient(payload.activeIngredient());
        reg.setRegistrationNumber(payload.registrationNumber());
        reg.setMrlMgPerKg(payload.mrlMgPerKg());
        reg.setMaxDosePerHa(payload.maxDosePerHa());
        reg.setPreHarvestIntervalDays(payload.preHarvestIntervalDays());
        reg.setCropAuthorized(payload.cropAuthorized());
        reg.setNotes(payload.notes());
        reg = pesticideRegisterRepository.save(reg);
        return toPesticideResponse(reg);
    }

    @Transactional
    public ExportShipmentApi.PesticideResponse updatePesticide(String id, ExportShipmentApi.PesticidePayload payload) {
        PesticideRegister reg = pesticideRegisterRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Pesticide not found", "PESTICIDE_NOT_FOUND", HttpStatus.NOT_FOUND));
        reg.setChemicalName(payload.chemicalName());
        reg.setActiveIngredient(payload.activeIngredient());
        reg.setRegistrationNumber(payload.registrationNumber());
        reg.setMrlMgPerKg(payload.mrlMgPerKg());
        reg.setMaxDosePerHa(payload.maxDosePerHa());
        reg.setPreHarvestIntervalDays(payload.preHarvestIntervalDays());
        reg.setCropAuthorized(payload.cropAuthorized());
        reg.setNotes(payload.notes());
        reg = pesticideRegisterRepository.save(reg);
        return toPesticideResponse(reg);
    }

    // ─── Proceeds & Aging ────────────────────────────────────────────

    @Transactional
    public ExportShipmentApi.ProceedsResponse recordProceeds(String shipmentId, ExportShipmentApi.ProceedsPayload payload) {
        ExportShipment shipment = findShipment(shipmentId);
        shipment.setRealizedFxAmount(payload.realizedFxAmount());
        shipment = shipmentRepository.save(shipment);
        return new ExportShipmentApi.ProceedsResponse(
                shipmentId, shipment.getExpectedFxAmount(),
                shipment.getExpectedFxCurrency(),
                shipment.getRealizedFxAmount(),
                daysOutstanding(shipment));
    }

    @Transactional(readOnly = true)
    public ExportShipmentApi.AgingResponse getAging() {
        List<ExportShipment> openShipments = shipmentRepository.findByStatusIn(
                List.of(ExportShipmentStatus.PREPARING, ExportShipmentStatus.BOOKED, ExportShipmentStatus.SHIPPED));

        List<ExportShipmentApi.AgingEntry> entries = openShipments.stream()
                .map(s -> new ExportShipmentApi.AgingEntry(
                        s.getCustomerPartyId(),
                        s.getCustomerPartyName(),
                        s.getShipmentNumber(),
                        daysOutstanding(s),
                        s.getExpectedFxAmount(),
                        s.getExpectedFxCurrency()))
                .sorted(Comparator.comparingInt(ExportShipmentApi.AgingEntry::daysOutstanding).reversed())
                .toList();

        BigDecimal totalFx = entries.stream()
                .map(e -> e.expectedFxAmount() == null ? BigDecimal.ZERO : e.expectedFxAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = entries.isEmpty() ? null : entries.get(0).expectedFxCurrency();

        return new ExportShipmentApi.AgingResponse(entries, totalFx, currency);
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private ExportShipment findShipment(String id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Export shipment not found", "EXPORT_SHIPMENT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private int daysOutstanding(ExportShipment s) {
        long createdMs = s.getCreatedAt();
        return (int) ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(createdMs).atZone(ZoneOffset.UTC).toLocalDate(),
                LocalDate.now(ZoneOffset.UTC));
    }

    private String generateShipmentNumber() {
        String prefix = "EXP-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        return prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private ExportShipmentApi.ShipmentResponse toShipmentResponse(ExportShipment s) {
        List<ExportShipmentApi.ShipmentLineResponse> lines = s.getLines().stream()
                .map(l -> new ExportShipmentApi.ShipmentLineResponse(
                        l.getId(), l.getLineOrder(), l.getItemName(), l.getItemCode(),
                        l.getLotReference(), l.getQuantity(), l.getUnitOfMeasure(),
                        l.getNetWeightKg(), l.getGrossWeightKg(), l.getPackagesCount()))
                .toList();

        return new ExportShipmentApi.ShipmentResponse(
                s.getId(), s.getShipmentNumber(), s.getCustomerPartyId(), s.getCustomerPartyName(),
                s.getContractRef(), s.getContainerNo(), s.getBookingNo(), s.getAcidNo(),
                s.getPortOfLoading(), s.getPortOfDischarge(), s.getEtbDate(), s.getEtaDate(),
                s.getStatus().name(), s.getNotes(),
                s.getExpectedFxAmount(), s.getExpectedFxCurrency(), s.getRealizedFxAmount(),
                daysOutstanding(s), lines, s.getCreatedAt(), s.getUpdatedAt());
    }

    private ExportShipmentApi.TreatmentLogResponse toTreatmentResponse(ComplianceRegister r) {
        boolean violation = r.isViolation(LocalDate.now(ZoneOffset.UTC));
        long daysSafe = r.daysUntilSafe(LocalDate.now(ZoneOffset.UTC));
        return new ExportShipmentApi.TreatmentLogResponse(
                r.getId(), r.getLotReference(), r.getChemical(), r.getDose(),
                r.getTreatmentDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                r.getPreHarvestIntervalDays(),
                r.earliestSafePickup().format(ISO_DATE),
                violation, daysSafe,
                r.getTreatedBy(), r.getNotes(), r.getCreatedAt());
    }

    private ExportShipmentApi.PesticideResponse toPesticideResponse(PesticideRegister r) {
        return new ExportShipmentApi.PesticideResponse(
                r.getId(), r.getChemicalName(), r.getActiveIngredient(), r.getRegistrationNumber(),
                r.getMrlMgPerKg(), r.getMaxDosePerHa(), r.getPreHarvestIntervalDays(),
                r.getCropAuthorized(), r.getStatus(), r.getNotes(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
