package com.bemo.hr.medical.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.*;
import com.bemo.hr.medical.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PharmacyService {

    private final PharmacyItemRepository itemRepository;
    private final PharmacyDispenseRecordRepository recordRepository;
    private final PharmacyDispenseLineRepository lineRepository;
    private final NarcoticsRegisterRepository narcoticsRepository;
    private final ClinicVisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;

    public PharmacyService(PharmacyItemRepository itemRepository,
                           PharmacyDispenseRecordRepository recordRepository,
                           PharmacyDispenseLineRepository lineRepository,
                           NarcoticsRegisterRepository narcoticsRepository,
                           ClinicVisitRepository visitRepository,
                           PatientRepository patientRepository,
                           EmployeeRepository employeeRepository) {
        this.itemRepository = itemRepository;
        this.recordRepository = recordRepository;
        this.lineRepository = lineRepository;
        this.narcoticsRepository = narcoticsRepository;
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
    }

    public PharmacyItemDto savePharmacyItem(SavePharmacyItemRequest request) {
        String appId = TenantContext.require();

        PharmacyItem.DosageForm dosageForm = PharmacyItem.DosageForm.TABLET;
        if (request.dosageForm() != null && !request.dosageForm().trim().isEmpty()) {
            try {
                dosageForm = PharmacyItem.DosageForm.valueOf(request.dosageForm().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                dosageForm = PharmacyItem.DosageForm.TABLET;
            }
        }

        PharmacyItem.ControlSchedule schedule = null;
        if (request.controlSchedule() != null && !request.controlSchedule().trim().isEmpty()) {
            try {
                schedule = PharmacyItem.ControlSchedule.valueOf(request.controlSchedule().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        PharmacyItem item = itemRepository.findByAppIdAndItemId(appId, request.itemId())
                .orElseGet(() -> new PharmacyItem(
                        request.itemId(),
                        request.tradeName(),
                        request.genericName(),
                        PharmacyItem.DosageForm.TABLET,
                        request.strengthText(),
                        request.controlled(),
                        null
                ));

        item.setTradeName(request.tradeName());
        item.setGenericName(request.genericName());
        item.setDosageForm(dosageForm);
        item.setStrengthText(request.strengthText());
        item.setControlled(request.controlled());
        item.setControlSchedule(schedule);

        PharmacyItem saved = itemRepository.save(item);
        log.info("Saved pharmacy item {} in tenant {}", saved.getTradeName(), appId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PharmacyItemDto> getAllPharmacyItems() {
        String appId = TenantContext.require();
        return itemRepository.findAllByAppIdOrderByTradeNameAsc(appId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BatchFefoSuggestionDto> getFefoSuggestions(String pharmacyItemId) {
        String appId = TenantContext.require();
        PharmacyItem item = itemRepository.findByAppIdAndId(appId, pharmacyItemId)
                .orElseThrow(() -> new NotFoundException("Pharmacy item not found", "PHARM_ITEM_NOT_FOUND"));

        LocalDate today = LocalDate.now();
        LocalDate warningHorizon = today.plusDays(90);

        // FEFO sample batches (earliest-expiry first)
        List<BatchFefoSuggestionDto> suggestions = new ArrayList<>();
        suggestions.add(new BatchFefoSuggestionDto(
                "BATCH-" + item.getTradeName().substring(0, Math.min(3, item.getTradeName().length())).toUpperCase() + "-01",
                today.plusMonths(4).toString(),
                BigDecimal.valueOf(100),
                false,
                false
        ));
        suggestions.add(new BatchFefoSuggestionDto(
                "BATCH-" + item.getTradeName().substring(0, Math.min(3, item.getTradeName().length())).toUpperCase() + "-02",
                today.plusMonths(12).toString(),
                BigDecimal.valueOf(250),
                false,
                false
        ));

        return suggestions;
    }

    public PharmacyDispenseRecordDto dispensePrescription(String prescriptionId,
                                                          DispensePrescriptionRequest request,
                                                          String currentUserId) {
        String appId = TenantContext.require();

        ClinicVisit visit = visitRepository.findByAppIdAndId(appId, prescriptionId)
                .orElseThrow(() -> new NotFoundException("Prescription/visit not found", "PHARM_PRESCRIPTION_NOT_FOUND"));

        LocalDate today = LocalDate.now();
        boolean containsControlled = false;

        for (DispenseLineRequest lineReq : request.lines()) {
            PharmacyItem drug = itemRepository.findByAppIdAndId(appId, lineReq.pharmacyItemId())
                    .orElseThrow(() -> new NotFoundException("Pharmacy item not found", "PHARM_ITEM_NOT_FOUND"));

            if (drug.isControlled()) {
                containsControlled = true;
            }

            if (lineReq.expiryDate() != null && !lineReq.expiryDate().trim().isEmpty()) {
                LocalDate expiry = LocalDate.parse(lineReq.expiryDate().trim());
                if (expiry.isBefore(today)) {
                    throw new BusinessRuleException("Cannot dispense an expired medication batch", "PHARM_BATCH_EXPIRED", HttpStatus.BAD_REQUEST);
                }
            }
        }

        PharmacyDispenseRecord.Status status = PharmacyDispenseRecord.Status.DISPENSED;
        if (containsControlled) {
            if (request.secondSignerId() == null || request.secondSignerId().trim().isEmpty()) {
                status = PharmacyDispenseRecord.Status.PENDING_APPROVAL;
            } else if (request.secondSignerId().trim().equals(currentUserId)) {
                throw new BusinessRuleException("Second approver must be different from the dispensing pharmacist", "PHARM_SECOND_SIGNER_CANNOT_BE_DISPENSER", HttpStatus.BAD_REQUEST);
            }
        }

        PharmacyDispenseRecord record = new PharmacyDispenseRecord(
                prescriptionId,
                visit.getPatientId(),
                visit.getDoctorEmployeeId(),
                currentUserId,
                request.secondSignerId(),
                status,
                containsControlled,
                request.notes()
        );

        PharmacyDispenseRecord savedRecord = recordRepository.save(record);
        List<PharmacyDispenseLine> savedLines = new ArrayList<>();

        for (DispenseLineRequest lineReq : request.lines()) {
            PharmacyItem drug = itemRepository.findByAppIdAndId(appId, lineReq.pharmacyItemId()).orElseThrow();
            PharmacyDispenseLine line = new PharmacyDispenseLine(
                    savedRecord.getId(),
                    lineReq.prescriptionLineId(),
                    drug.getId(),
                    drug.getItemId(),
                    lineReq.batchNumber(),
                    lineReq.expiryDate(),
                    lineReq.quantity()
            );
            savedLines.add(lineRepository.save(line));

            if (status == PharmacyDispenseRecord.Status.DISPENSED && drug.isControlled()) {
                createNarcoticsEntry(savedRecord, drug, lineReq.batchNumber(), lineReq.quantity(), "Prescription Dispense");
            }
        }

        log.info("Processed pharmacy dispense {} for prescription {} status {} in tenant {}",
                savedRecord.getId(), prescriptionId, status, appId);

        return toDto(savedRecord, savedLines);
    }

    public PharmacyDispenseRecordDto approveControlledDispense(String dispenseRecordId, String secondSignerId) {
        String appId = TenantContext.require();
        PharmacyDispenseRecord record = recordRepository.findByAppIdAndId(appId, dispenseRecordId)
                .orElseThrow(() -> new NotFoundException("Dispense record not found", "PHARM_DISPENSE_NOT_FOUND"));

        if (record.getStatus() != PharmacyDispenseRecord.Status.PENDING_APPROVAL) {
            throw new BusinessRuleException("This dispense request has already been processed", "PHARM_DISPENSE_ALREADY_PROCESSED", HttpStatus.CONFLICT);
        }

        if (secondSignerId.equals(record.getDispenserUserId())) {
            throw new BusinessRuleException("Second approver must be different from the dispensing pharmacist", "PHARM_SECOND_SIGNER_CANNOT_BE_DISPENSER", HttpStatus.BAD_REQUEST);
        }

        record.approve(secondSignerId);
        PharmacyDispenseRecord saved = recordRepository.save(record);

        List<PharmacyDispenseLine> lines = lineRepository.findAllByAppIdAndDispenseRecordId(appId, dispenseRecordId);
        for (PharmacyDispenseLine line : lines) {
            PharmacyItem drug = itemRepository.findByAppIdAndId(appId, line.getPharmacyItemId()).orElseThrow();
            if (drug.isControlled()) {
                createNarcoticsEntry(saved, drug, line.getBatchNumber(), line.getQuantityDispensed(), "Controlled Drug Dual Approved");
            }
        }

        log.info("Approved controlled dispense {} with second signer {} in tenant {}", dispenseRecordId, secondSignerId, appId);
        return toDto(saved, lines);
    }

    @Transactional(readOnly = true)
    public List<NarcoticsRegisterEntryDto> getNarcoticsRegister(Long fromEpoch, Long toEpoch) {
        String appId = TenantContext.require();
        List<NarcoticsRegisterEntry> entries;
        if (fromEpoch != null && toEpoch != null) {
            entries = narcoticsRepository.findAllInPeriod(appId, fromEpoch, toEpoch);
        } else {
            entries = narcoticsRepository.findAllByAppIdOrderBySignedAtDesc(appId);
        }

        return entries.stream()
                .map(n -> new NarcoticsRegisterEntryDto(
                        n.getId(),
                        n.getDispenseRecordId(),
                        n.getPharmacyItemId(),
                        n.getTradeName(),
                        n.getPatientMrn(),
                        n.getPatientName(),
                        n.getPrescriberDoctorName(),
                        n.getDispenserUserName(),
                        n.getSecondSignerName(),
                        n.getBatchNumber(),
                        n.getQuantity(),
                        n.getReason(),
                        n.getSignedAt()
                ))
                .toList();
    }

    private void createNarcoticsEntry(PharmacyDispenseRecord record,
                                      PharmacyItem drug,
                                      String batchNumber,
                                      BigDecimal quantity,
                                      String reason) {
        String appId = TenantContext.require();
        Patient patient = patientRepository.findByAppIdAndId(appId, record.getPatientId()).orElse(null);
        String patientName = patient != null ? patient.getFullName() : "Unknown";
        String patientMrn = patient != null ? patient.getMrn() : "";

        String prescriberName = employeeRepository.findById(record.getPrescriberDoctorId())
                .map(Employee::getFullName).orElse(record.getPrescriberDoctorId());
        String dispenserName = employeeRepository.findById(record.getDispenserUserId())
                .map(Employee::getFullName).orElse(record.getDispenserUserId());
        String secondSignerName = record.getSecondSignerId() != null
                ? employeeRepository.findById(record.getSecondSignerId()).map(Employee::getFullName).orElse(record.getSecondSignerId())
                : "";

        NarcoticsRegisterEntry entry = new NarcoticsRegisterEntry(
                record.getId(),
                drug.getId(),
                drug.getTradeName(),
                patientMrn,
                patientName,
                prescriberName,
                dispenserName,
                secondSignerName,
                batchNumber,
                quantity,
                reason
        );

        narcoticsRepository.save(entry);
    }

    private PharmacyItemDto toDto(PharmacyItem i) {
        return new PharmacyItemDto(
                i.getId(),
                i.getItemId(),
                i.getTradeName(),
                i.getGenericName(),
                i.getDosageForm().name(),
                i.getStrengthText(),
                i.isControlled(),
                i.getControlSchedule() != null ? i.getControlSchedule().name() : null
        );
    }

    private PharmacyDispenseRecordDto toDto(PharmacyDispenseRecord r, List<PharmacyDispenseLine> lines) {
        String appId = TenantContext.require();
        Patient patient = patientRepository.findByAppIdAndId(appId, r.getPatientId()).orElse(null);
        String patientName = patient != null ? patient.getFullName() : "Unknown";
        String patientMrn = patient != null ? patient.getMrn() : "";

        String prescriberName = employeeRepository.findById(r.getPrescriberDoctorId())
                .map(Employee::getFullName).orElse(r.getPrescriberDoctorId());
        String dispenserName = employeeRepository.findById(r.getDispenserUserId())
                .map(Employee::getFullName).orElse(r.getDispenserUserId());
        String secondSignerName = r.getSecondSignerId() != null
                ? employeeRepository.findById(r.getSecondSignerId()).map(Employee::getFullName).orElse(r.getSecondSignerId())
                : null;

        Map<String, PharmacyItem> drugMap = itemRepository.findAllById(lines.stream().map(PharmacyDispenseLine::getPharmacyItemId).toList())
                .stream().collect(Collectors.toMap(PharmacyItem::getId, i -> i));

        List<PharmacyDispenseLineDto> lineDtos = lines.stream()
                .map(l -> new PharmacyDispenseLineDto(
                        l.getId(),
                        l.getPrescriptionLineId(),
                        l.getPharmacyItemId(),
                        drugMap.containsKey(l.getPharmacyItemId()) ? drugMap.get(l.getPharmacyItemId()).getTradeName() : "",
                        l.getBatchNumber(),
                        l.getExpiryDate(),
                        l.getQuantityDispensed(),
                        l.getCreatedAt()
                ))
                .toList();

        return new PharmacyDispenseRecordDto(
                r.getId(),
                r.getPrescriptionId(),
                r.getPatientId(),
                patientName,
                patientMrn,
                r.getPrescriberDoctorId(),
                prescriberName,
                r.getDispenserUserId(),
                dispenserName,
                r.getSecondSignerId(),
                secondSignerName,
                r.getStatus().name(),
                r.isControlled(),
                r.getNotes(),
                lineDtos,
                r.getCreatedAt()
        );
    }
}
