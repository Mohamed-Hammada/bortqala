package com.bemo.hr.serviceops.api;

import com.bemo.hr.serviceops.application.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-ops/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Resources
    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceOpsApi.BookableResourceResponse createResource(@Valid @RequestBody ServiceOpsApi.BookableResourceCreateRequest request) {
        return bookingService.createResource(request);
    }

    @GetMapping("/resources")
    public List<ServiceOpsApi.BookableResourceResponse> listResources() {
        return bookingService.listResources();
    }

    @GetMapping("/resources/{id}")
    public ServiceOpsApi.BookableResourceResponse getResource(@PathVariable String id) {
        return bookingService.getResource(id);
    }

    // Bookings
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceOpsApi.ResourceBookingResponse createBooking(@Valid @RequestBody ServiceOpsApi.ResourceBookingCreateRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping
    public List<ServiceOpsApi.ResourceBookingResponse> listBookings(@RequestParam(required = false) String resourceId) {
        if (resourceId != null && !resourceId.isBlank()) {
            return bookingService.listResourceBookings(resourceId);
        }
        return bookingService.listBookings();
    }

    @PostMapping("/{id}/cancel")
    public ServiceOpsApi.ResourceBookingResponse cancelBooking(@PathVariable String id) {
        return bookingService.cancelBooking(id);
    }
}
