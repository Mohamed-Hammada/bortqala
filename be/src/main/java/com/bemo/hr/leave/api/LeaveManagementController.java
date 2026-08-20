package com.bemo.hr.leave.api;

import com.bemo.hr.leave.application.LeaveManagementService;
import com.bemo.hr.leave.domain.LeaveRequestStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
public class LeaveManagementController {

    private final LeaveManagementService leaveService;

    public LeaveManagementController(LeaveManagementService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/types")
    @PreAuthorize("@auth.hasPermission('leaves.read')")
    public List<LeaveManagementApi.LeaveTypeResponse> listTypes() {
        return leaveService.listLeaveTypes();
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('leaves.manage')")
    public LeaveManagementApi.LeaveTypeResponse createType(@Valid @RequestBody LeaveManagementApi.CreateLeaveTypeRequest request) {
        return leaveService.createLeaveType(request);
    }

    @GetMapping("/balances")
    @PreAuthorize("@auth.hasPermission('leaves.read')")
    public List<LeaveManagementApi.LeaveBalanceResponse> listBalances(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) Integer year) {
        return leaveService.listBalances(employeeId, year);
    }

    @PostMapping("/balances/adjust")
    @PreAuthorize("@auth.hasPermission('leaves.manage')")
    public LeaveManagementApi.LeaveBalanceResponse adjustBalance(@Valid @RequestBody LeaveManagementApi.AdjustBalanceRequest request) {
        return leaveService.adjustBalance(request);
    }

    @GetMapping("/requests")
    @PreAuthorize("@auth.hasPermission('leaves.read')")
    public List<LeaveManagementApi.LeaveRequestResponse> listRequests(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) LeaveRequestStatus status) {
        return leaveService.listRequests(employeeId, status);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('leaves.manage')")
    public LeaveManagementApi.LeaveRequestResponse submitRequest(@Valid @RequestBody LeaveManagementApi.SubmitLeaveRequest request) {
        return leaveService.submitLeaveRequest(request);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("@auth.hasPermission('leaves.manage')")
    public LeaveManagementApi.LeaveRequestResponse approveRequest(@PathVariable String id, Authentication auth) {
        String approver = auth != null ? auth.getName() : "ADMIN";
        return leaveService.approveLeaveRequest(id, approver);
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("@auth.hasPermission('leaves.manage')")
    public LeaveManagementApi.LeaveRequestResponse rejectRequest(
            @PathVariable String id,
            @Valid @RequestBody LeaveManagementApi.RejectLeaveRequest request) {
        return leaveService.rejectLeaveRequest(id, request);
    }

    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize("@auth.hasPermission('leaves.manage')")
    public LeaveManagementApi.LeaveRequestResponse cancelRequest(@PathVariable String id) {
        return leaveService.cancelLeaveRequest(id);
    }
}
