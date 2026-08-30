package com.bemo.hr.verticals.api;

import com.bemo.hr.verticals.api.SpecializedVerticalsApi.*;
import com.bemo.hr.verticals.application.SpecializedVerticalsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/verticals")
public class SpecializedVerticalsController {

    private final SpecializedVerticalsService verticalsService;

    public SpecializedVerticalsController(SpecializedVerticalsService verticalsService) {
        this.verticalsService = verticalsService;
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public VerticalsSummaryResponse getSummary() {
        return verticalsService.getVerticalsSummary();
    }

    // --- School & Education Endpoints ---

    @GetMapping("/school/students")
    @PreAuthorize("isAuthenticated()")
    public List<StudentEnrollmentResponse> listStudents() {
        return verticalsService.listStudents();
    }

    @PostMapping("/school/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER')")
    public StudentEnrollmentResponse registerStudent(@Valid @RequestBody RegisterStudentPayload payload) {
        return verticalsService.registerStudent(payload);
    }

    // --- Tourism & Travel Endpoints ---

    @GetMapping("/tourism/bookings")
    @PreAuthorize("isAuthenticated()")
    public List<TourismBookingResponse> listTourismBookings() {
        return verticalsService.listTourismBookings();
    }

    @PostMapping("/tourism/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALES_MANAGER', 'FINANCE_MANAGER')")
    public TourismBookingResponse createTourismBooking(@Valid @RequestBody CreateBookingPayload payload) {
        return verticalsService.createTourismBooking(payload);
    }

    // --- Customs Clearance Endpoints ---

    @GetMapping("/customs/declarations")
    @PreAuthorize("isAuthenticated()")
    public List<CustomsDeclarationResponse> listCustomsDeclarations() {
        return verticalsService.listCustomsDeclarations();
    }

    @PostMapping("/customs/declarations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'FINANCE_MANAGER')")
    public CustomsDeclarationResponse openCustomsDeclaration(@Valid @RequestBody OpenDeclarationPayload payload) {
        return verticalsService.openCustomsDeclaration(payload);
    }

    // --- 3PL Logistics Endpoints ---

    @GetMapping("/3pl/contracts")
    @PreAuthorize("isAuthenticated()")
    public List<ThreePlContractResponse> list3plContracts() {
        return verticalsService.list3plContracts();
    }

    @PostMapping("/3pl/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'FINANCE_MANAGER')")
    public ThreePlContractResponse create3plContract(@Valid @RequestBody Create3plContractPayload payload) {
        return verticalsService.create3plContract(payload);
    }
}
