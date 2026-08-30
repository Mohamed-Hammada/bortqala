package com.bemo.hr.serviceops.application;

import com.bemo.hr.serviceops.api.ServiceOpsApi;
import com.bemo.hr.serviceops.domain.BookableResource;
import com.bemo.hr.serviceops.domain.ResourceBooking;
import com.bemo.hr.serviceops.infrastructure.BookableResourceRepository;
import com.bemo.hr.serviceops.infrastructure.ResourceBookingRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class BookingService {

    private final BookableResourceRepository resourceRepository;
    private final ResourceBookingRepository bookingRepository;

    public BookingService(BookableResourceRepository resourceRepository,
                          ResourceBookingRepository bookingRepository) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
    }

    // --- Bookable Resources ---

    @Transactional
    public ServiceOpsApi.BookableResourceResponse createResource(ServiceOpsApi.BookableResourceCreateRequest request) {
        String appId = TenantContext.require();
        BookableResource resource = new BookableResource(
                appId,
                request.code(),
                request.name(),
                request.nameEn(),
                request.kind(),
                request.capacity(),
                request.location()
        );
        BookableResource saved = resourceRepository.save(resource);
        log.info("Created bookable resource {} for app {}", saved.getCode(), appId);
        return toResourceResponse(saved);
    }

    public List<ServiceOpsApi.BookableResourceResponse> listResources() {
        String appId = TenantContext.require();
        return resourceRepository.findByAppIdOrderByCreatedAtDesc(appId).stream()
                .map(this::toResourceResponse)
                .collect(Collectors.toList());
    }

    public ServiceOpsApi.BookableResourceResponse getResource(String id) {
        String appId = TenantContext.require();
        BookableResource resource = resourceRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("BOOKING_RESOURCE_NOT_FOUND", "BOOKING_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toResourceResponse(resource);
    }

    // --- Resource Bookings ---

    @Transactional
    public ServiceOpsApi.ResourceBookingResponse createBooking(ServiceOpsApi.ResourceBookingCreateRequest request) {
        String appId = TenantContext.require();

        BookableResource resource = resourceRepository.findByAppIdAndId(appId, request.resourceId())
                .orElseThrow(() -> new BusinessRuleException("BOOKING_RESOURCE_NOT_FOUND", "BOOKING_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!resource.isActive()) {
            throw new BusinessRuleException("BOOKING_RESOURCE_INACTIVE", "BOOKING_RESOURCE_INACTIVE", HttpStatus.BAD_REQUEST);
        }

        if (request.startTime() >= request.endTime()) {
            throw new BusinessRuleException("BOOKING_INVALID_TIME_RANGE", "BOOKING_INVALID_TIME_RANGE", HttpStatus.BAD_REQUEST);
        }

        List<ResourceBooking> conflicts = bookingRepository.findConflictingBookings(
                appId,
                request.resourceId(),
                request.startTime(),
                request.endTime()
        );

        if (!conflicts.isEmpty()) {
            throw new BusinessRuleException("BOOKING_COLLISION_DETECTED", "BOOKING_COLLISION_DETECTED", HttpStatus.CONFLICT);
        }

        ResourceBooking booking = new ResourceBooking(
                appId,
                request.resourceId(),
                request.title(),
                request.customerPartyId(),
                request.customerName(),
                request.startTime(),
                request.endTime(),
                request.notes()
        );

        ResourceBooking saved = bookingRepository.save(booking);
        log.info("Created booking {} for resource {} from {} to {}", saved.getTitle(), resource.getName(), request.startTime(), request.endTime());
        return toBookingResponse(saved);
    }

    public List<ServiceOpsApi.ResourceBookingResponse> listBookings() {
        String appId = TenantContext.require();
        return bookingRepository.findByAppIdOrderByStartTimeDesc(appId).stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceOpsApi.ResourceBookingResponse> listResourceBookings(String resourceId) {
        String appId = TenantContext.require();
        return bookingRepository.findByAppIdAndResourceIdOrderByStartTimeAsc(appId, resourceId).stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceOpsApi.ResourceBookingResponse cancelBooking(String id) {
        String appId = TenantContext.require();
        ResourceBooking booking = bookingRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("BOOKING_RESOURCE_NOT_FOUND", "BOOKING_RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND));



        booking.setStatus(ResourceBooking.Status.CANCELLED);
        ResourceBooking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
    }

    // --- Mappers ---

    private ServiceOpsApi.BookableResourceResponse toResourceResponse(BookableResource r) {
        return new ServiceOpsApi.BookableResourceResponse(
                r.getId(),
                r.getCode(),
                r.getName(),
                r.getNameEn(),
                r.getKind(),
                r.getCapacity(),
                r.getLocation(),
                r.isActive(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private ServiceOpsApi.ResourceBookingResponse toBookingResponse(ResourceBooking b) {
        return new ServiceOpsApi.ResourceBookingResponse(
                b.getId(),
                b.getResourceId(),
                b.getTitle(),
                b.getCustomerPartyId(),
                b.getCustomerName(),
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus(),
                b.getNotes(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
