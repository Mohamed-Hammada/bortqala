package com.bemo.hr.medical.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.DoctorRosterDto;
import com.bemo.hr.medical.api.MedicalClinicApi.SaveDoctorRosterRequest;
import com.bemo.hr.medical.domain.DoctorRoster;
import com.bemo.hr.medical.infrastructure.DoctorRosterRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class DoctorRosterService {

    private final DoctorRosterRepository rosterRepository;
    private final EmployeeRepository employeeRepository;

    public DoctorRosterService(DoctorRosterRepository rosterRepository, EmployeeRepository employeeRepository) {
        this.rosterRepository = rosterRepository;
        this.employeeRepository = employeeRepository;
    }

    public DoctorRosterDto saveRoster(SaveDoctorRosterRequest request) {
        String appId = TenantContext.require();

        if (request.startTime().compareTo(request.endTime()) >= 0) {
            throw new BusinessRuleException("Start time must be before end time", "ROSTER_INVALID_HOURS", HttpStatus.BAD_REQUEST);
        }

        List<DoctorRoster> existing = rosterRepository.findAllByAppIdAndDoctorEmployeeIdAndWeekdayAndActiveTrue(
                appId, request.doctorEmployeeId(), request.weekday()
        );

        for (DoctorRoster r : existing) {
            boolean overlap = !(request.endTime().compareTo(r.getStartTime()) <= 0 || request.startTime().compareTo(r.getEndTime()) >= 0);
            if (overlap) {
                throw new BusinessRuleException("Roster hours overlap with an existing schedule for this doctor", "ROSTER_OVERLAP", HttpStatus.CONFLICT);
            }
        }

        DoctorRoster roster = new DoctorRoster(
                request.doctorEmployeeId(),
                request.weekday(),
                request.startTime(),
                request.endTime(),
                request.slotMinutes() > 0 ? request.slotMinutes() : 20,
                request.maxPatientsPerSlot() > 0 ? request.maxPatientsPerSlot() : 1,
                request.validFrom(),
                request.validTo()
        );

        DoctorRoster saved = rosterRepository.save(roster);
        log.info("Saved doctor roster for {} on weekday {} in tenant {}", request.doctorEmployeeId(), request.weekday(), appId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DoctorRosterDto> getRostersForDoctor(String doctorEmployeeId) {
        String appId = TenantContext.require();
        return rosterRepository.findAllByAppIdAndDoctorEmployeeIdAndActiveTrue(appId, doctorEmployeeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorRosterDto> getAllRosters() {
        String appId = TenantContext.require();
        return rosterRepository.findAllByAppId(appId)
                .stream()
                .filter(DoctorRoster::isActive)
                .map(this::toDto)
                .toList();
    }

    public void deleteRoster(String rosterId) {
        String appId = TenantContext.require();
        DoctorRoster roster = rosterRepository.findByAppIdAndId(appId, rosterId)
                .orElseThrow(() -> new NotFoundException("Roster record not found", "ROSTER_NOT_FOUND"));
        roster.setActive(false);
        rosterRepository.save(roster);
    }

    public DoctorRosterDto toDto(DoctorRoster r) {
        String docName = employeeRepository.findById(r.getDoctorEmployeeId())
                .map(com.bemo.hr.employee.domain.Employee::getFullName)
                .orElse(r.getDoctorEmployeeId());

        return new DoctorRosterDto(
                r.getId(),
                r.getDoctorEmployeeId(),
                docName,
                r.getWeekday(),
                r.getStartTime(),
                r.getEndTime(),
                r.getSlotMinutes(),
                r.getMaxPatientsPerSlot(),
                r.getValidFrom(),
                r.getValidTo(),
                r.isActive()
        );
    }
}
