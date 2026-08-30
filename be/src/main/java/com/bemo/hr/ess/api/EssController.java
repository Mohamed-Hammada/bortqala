package com.bemo.hr.ess.api;

import com.bemo.hr.ess.application.EssService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ess")
@PreAuthorize("isAuthenticated()")
public class EssController {

    private final EssService essService;

    public EssController(EssService essService) {
        this.essService = essService;
    }

    @GetMapping("/profile")
    public EssApi.ProfileResponse getProfile(Authentication authentication) {
        return essService.getProfile(authentication.getName());
    }

    @GetMapping("/payslips")
    public List<EssApi.PayslipSummaryResponse> listPayslips(Authentication authentication,
                                                            @RequestParam(name = "year", required = false) Integer year) {
        return essService.listMyPayslips(authentication.getName(), year);
    }

    @GetMapping("/payslips/{id}")
    public EssApi.PayslipDetailResponse getPayslipDetail(Authentication authentication,
                                                         @PathVariable("id") String paymentId) {
        return essService.getPayslipDetail(authentication.getName(), paymentId);
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public EssApi.LeaveResponse submitLeave(Authentication authentication,
                                            @RequestBody EssApi.LeaveSubmitRequest request) {
        return essService.submitLeave(authentication.getName(), request);
    }

    @GetMapping("/leaves")
    public List<EssApi.LeaveResponse> listMyLeaves(Authentication authentication) {
        return essService.listMyLeaves(authentication.getName());
    }

    @PostMapping("/advances")
    @ResponseStatus(HttpStatus.CREATED)
    public EssApi.AdvanceResponse submitAdvance(Authentication authentication,
                                               @RequestBody EssApi.AdvanceSubmitRequest request) {
        return essService.submitAdvance(authentication.getName(), request);
    }

    @GetMapping("/advances")
    public List<EssApi.AdvanceResponse> listMyAdvances(Authentication authentication) {
        return essService.listMyAdvances(authentication.getName());
    }

    @GetMapping("/attendance")
    public List<EssApi.AttendanceRecordResponse> listMyAttendance(Authentication authentication) {
        return essService.listMyAttendance(authentication.getName());
    }
}
