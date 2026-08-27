package com.bemo.hr.recruitment.api;

import com.bemo.hr.recruitment.application.RecruitmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recruitment")
public class RecruitmentController {

    private final RecruitmentService service;

    public RecruitmentController(RecruitmentService service) {
        this.service = service;
    }

    // ---- Openings ----

    @GetMapping("/openings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<RecruitmentApi.OpeningResponse>> listOpenings() {
        return ResponseEntity.ok(service.listOpenings());
    }

    @PostMapping("/openings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<RecruitmentApi.OpeningResponse> createOpening(@RequestBody @Valid RecruitmentApi.CreateOpeningRequest request) {
        return ResponseEntity.ok(service.createOpening(request));
    }

    @PutMapping("/openings/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<RecruitmentApi.OpeningResponse> updateOpening(@PathVariable String id,
                                                                       @RequestBody @Valid RecruitmentApi.UpdateOpeningRequest request) {
        return ResponseEntity.ok(service.updateOpening(id, request));
    }

    @PostMapping("/openings/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<Void> closeOpening(@PathVariable String id) {
        service.closeOpening(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Applications ----

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<RecruitmentApi.ApplicationResponse>> listApplications(
            @RequestParam(required = false) String openingId) {
        return ResponseEntity.ok(service.listApplications(openingId));
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<RecruitmentApi.ApplicationResponse> createApplication(
            @RequestBody @Valid RecruitmentApi.CreateApplicationRequest request) {
        return ResponseEntity.ok(service.createApplication(request));
    }

    @PostMapping("/applications/{id}/move-stage")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<RecruitmentApi.ApplicationResponse> moveStage(
            @PathVariable String id, @RequestBody @Valid RecruitmentApi.MoveStageRequest request) {
        return ResponseEntity.ok(service.moveStage(id, request));
    }

    @PutMapping("/applications/{id}/rating")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<RecruitmentApi.ApplicationResponse> updateRating(
            @PathVariable String id, @RequestParam Integer rating) {
        return ResponseEntity.ok(service.updateRating(id, rating));
    }

    @PutMapping("/applications/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<RecruitmentApi.ApplicationResponse> updateNotes(
            @PathVariable String id, @RequestBody String notes) {
        return ResponseEntity.ok(service.updateNotes(id, notes));
    }

    @GetMapping("/applications/{id}/stage-events")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<RecruitmentApi.StageEventResponse>> listStageEvents(@PathVariable String id) {
        return ResponseEntity.ok(service.listStageEvents(id));
    }

    @PostMapping("/applications/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<RecruitmentApi.ConvertResponse> convertToEmployee(
            @PathVariable String id, @RequestBody RecruitmentApi.ConvertToEmployeeRequest request) {
        return ResponseEntity.ok(service.convertToEmployee(id, request));
    }

    @GetMapping("/applications/duplicates")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<RecruitmentApi.DuplicateWarning>> checkDuplicates(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(service.checkDuplicates(phone, email));
    }
}
