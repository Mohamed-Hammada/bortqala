package com.bemo.hr.recruitment.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.recruitment.api.RecruitmentApi;
import com.bemo.hr.recruitment.domain.*;
import com.bemo.hr.recruitment.infrastructure.ApplicationStageEventRepository;
import com.bemo.hr.recruitment.infrastructure.JobApplicationRepository;
import com.bemo.hr.recruitment.infrastructure.JobOpeningRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class RecruitmentService {

    private final JobOpeningRepository openingRepository;
    private final JobApplicationRepository applicationRepository;
    private final ApplicationStageEventRepository eventRepository;
    private final EmployeeRepository employeeRepository;

    // Valid stage transitions
    private static final Map<ApplicationStage, Set<ApplicationStage>> TRANSITIONS = Map.of(
            ApplicationStage.NEW, Set.of(ApplicationStage.SCREENING, ApplicationStage.REJECTED),
            ApplicationStage.SCREENING, Set.of(ApplicationStage.INTERVIEW, ApplicationStage.REJECTED),
            ApplicationStage.INTERVIEW, Set.of(ApplicationStage.OFFER, ApplicationStage.REJECTED),
            ApplicationStage.OFFER, Set.of(ApplicationStage.HIRED, ApplicationStage.REJECTED),
            ApplicationStage.HIRED, Set.of(),
            ApplicationStage.REJECTED, Set.of()
    );

    public RecruitmentService(JobOpeningRepository openingRepository,
                              JobApplicationRepository applicationRepository,
                              ApplicationStageEventRepository eventRepository,
                              EmployeeRepository employeeRepository) {
        this.openingRepository = openingRepository;
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
        this.employeeRepository = employeeRepository;
    }

    // ---- Opening CRUD ----

    @Transactional(readOnly = true)
    public List<RecruitmentApi.OpeningResponse> listOpenings() {
        return openingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> toOpeningResponse(o, applicationRepository.findByOpeningIdOrderByCreatedAtDesc(o.getId()).size()))
                .toList();
    }

    @Transactional
    public RecruitmentApi.OpeningResponse createOpening(RecruitmentApi.CreateOpeningRequest request) {
        JobOpening opening = new JobOpening(
                request.titleAr(), request.titleEn(), request.departmentId(),
                request.headcount(), request.description(), false);
        JobOpening saved = openingRepository.save(opening);
        return toOpeningResponse(saved, 0);
    }

    @Transactional
    public RecruitmentApi.OpeningResponse updateOpening(String id, RecruitmentApi.UpdateOpeningRequest request) {
        JobOpening opening = openingRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Opening not found", "RECR_OPENING_NOT_FOUND", HttpStatus.NOT_FOUND));
        opening.update(request.titleAr(), request.titleEn(), request.departmentId(),
                request.headcount(), request.description(), request.published());
        if (request.published() && opening.getStatus() == OpeningStatus.DRAFT) {
            opening.publish();
        }
        JobOpening saved = openingRepository.save(opening);
        int count = applicationRepository.findByOpeningIdOrderByCreatedAtDesc(id).size();
        return toOpeningResponse(saved, count);
    }

    @Transactional
    public void closeOpening(String id) {
        JobOpening opening = openingRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Opening not found", "RECR_OPENING_NOT_FOUND", HttpStatus.NOT_FOUND));
        opening.close();
        openingRepository.save(opening);
    }

    // ---- Application CRUD ----

    @Transactional(readOnly = true)
    public List<RecruitmentApi.ApplicationResponse> listApplications(String openingId) {
        List<JobApplication> list = openingId != null
                ? applicationRepository.findByOpeningIdOrderByCreatedAtDesc(openingId)
                : applicationRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toApplicationResponse).toList();
    }

    @Transactional
    public RecruitmentApi.ApplicationResponse createApplication(RecruitmentApi.CreateApplicationRequest request) {
        JobOpening opening = openingRepository.findById(request.openingId())
                .orElseThrow(() -> new BusinessRuleException("Opening not found", "RECR_OPENING_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (opening.getStatus() == OpeningStatus.CLOSED) {
            throw new BusinessRuleException("Cannot apply to a closed opening", "RECR_OPENING_CLOSED", HttpStatus.CONFLICT);
        }

        // Duplicate warning (non-blocking, logged via warning)
        boolean phoneDup = request.phone() != null && applicationRepository.existsByPhoneOrEmail(request.phone(), request.email());
        boolean emailDup = request.email() != null && applicationRepository.existsByPhoneOrEmail(request.phone(), request.email());

        JobApplication app = new JobApplication(
                request.openingId(), request.fullName(), request.phone(),
                request.email(), request.source(), request.cvAttachmentId());
        JobApplication saved = applicationRepository.save(app);

        eventRepository.save(new ApplicationStageEvent(saved.getId(), ApplicationStage.NEW, ApplicationStage.NEW,
                actor(), "Application created"));

        return toApplicationResponse(saved);
    }

    @Transactional
    public RecruitmentApi.ApplicationResponse moveStage(String applicationId, RecruitmentApi.MoveStageRequest request) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessRuleException("Application not found", "RECR_APP_NOT_FOUND", HttpStatus.NOT_FOUND));

        Set<ApplicationStage> allowed = TRANSITIONS.getOrDefault(app.getStage(), Set.of());
        if (!allowed.contains(request.toStage())) {
            throw new BusinessRuleException(
                    "Invalid stage transition from " + app.getStage() + " to " + request.toStage(),
                    "RECR_INVALID_TRANSITION", HttpStatus.CONFLICT);
        }

        ApplicationStage from = app.getStage();
        app.moveToStage(request.toStage());
        applicationRepository.save(app);

        eventRepository.save(new ApplicationStageEvent(applicationId, from, request.toStage(),
                actor(), request.note()));

        return toApplicationResponse(app);
    }

    @Transactional
    public RecruitmentApi.ApplicationResponse updateRating(String applicationId, Integer rating) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessRuleException("Application not found", "RECR_APP_NOT_FOUND", HttpStatus.NOT_FOUND));
        app.setRating(rating);
        applicationRepository.save(app);
        return toApplicationResponse(app);
    }

    @Transactional
    public RecruitmentApi.ApplicationResponse updateNotes(String applicationId, String notes) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessRuleException("Application not found", "RECR_APP_NOT_FOUND", HttpStatus.NOT_FOUND));
        app.setNotes(notes);
        applicationRepository.save(app);
        return toApplicationResponse(app);
    }

    @Transactional(readOnly = true)
    public List<RecruitmentApi.StageEventResponse> listStageEvents(String applicationId) {
        return eventRepository.findByApplicationIdOrderByEventAtAsc(applicationId).stream()
                .map(e -> new RecruitmentApi.StageEventResponse(
                        e.getId(), e.getFromStage(), e.getToStage(),
                        e.getActor(), e.getNote(), e.getEventAt()))
                .toList();
    }

    // ---- Convert to Employee ----

    @Transactional
    public RecruitmentApi.ConvertResponse convertToEmployee(String applicationId, RecruitmentApi.ConvertToEmployeeRequest request) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessRuleException("Application not found", "RECR_APP_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (app.getConvertedEmployeeId() != null) {
            throw new BusinessRuleException("Application already converted", "RECR_ALREADY_CONVERTED", HttpStatus.CONFLICT);
        }
        if (app.getStage() != ApplicationStage.OFFER && app.getStage() != ApplicationStage.HIRED) {
            throw new BusinessRuleException("Application must be in OFFER or HIRED stage to convert",
                    "RECR_CONVERT_STAGE_INVALID", HttpStatus.CONFLICT);
        }

        String empCode = "REC-" + app.getId().substring(0, 8).toUpperCase();
        Employee employee = new Employee(empCode, app.getFullName(), null,
                request.departmentId() != null ? request.departmentId() : "DEFAULT",
                com.bemo.hr.employee.domain.EmploymentType.FIXED,
                LocalDate.now(), null, true);
        Employee saved = employeeRepository.save(employee);

        app.markHired(saved.getId());
        applicationRepository.save(app);

        eventRepository.save(new ApplicationStageEvent(applicationId, app.getStage(), ApplicationStage.HIRED,
                actor(), "Converted to employee " + saved.getId()));

        return new RecruitmentApi.ConvertResponse(saved.getId(), applicationId);
    }

    // ---- Duplicate check ----

    @Transactional(readOnly = true)
    public List<RecruitmentApi.DuplicateWarning> checkDuplicates(String phone, String email) {
        List<RecruitmentApi.DuplicateWarning> warnings = new ArrayList<>();
        if (phone != null) {
            applicationRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(a -> phone.equals(a.getPhone()))
                    .forEach(a -> warnings.add(new RecruitmentApi.DuplicateWarning(a.getId(), a.getFullName(), "phone")));
        }
        if (email != null) {
            applicationRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(a -> email.equals(a.getEmail()))
                    .forEach(a -> warnings.add(new RecruitmentApi.DuplicateWarning(a.getId(), a.getFullName(), "email")));
        }
        return warnings;
    }

    private String actor() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private RecruitmentApi.OpeningResponse toOpeningResponse(JobOpening o, int applicationCount) {
        return new RecruitmentApi.OpeningResponse(
                o.getId(), o.getTitleAr(), o.getTitleEn(), o.getDepartmentId(),
                o.getHeadcount(), o.getStatus(), o.getDescription(), o.isPublished(),
                applicationCount, o.getCreatedAt(), o.getUpdatedAt(), o.getVersion());
    }

    private RecruitmentApi.ApplicationResponse toApplicationResponse(JobApplication a) {
        return new RecruitmentApi.ApplicationResponse(
                a.getId(), a.getOpeningId(), a.getFullName(), a.getPhone(),
                a.getEmail(), a.getSource(), a.getCvAttachmentId(),
                a.getStage(), a.getRating(), a.getNotes(), a.getConvertedEmployeeId(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getVersion());
    }
}
