package com.bemo.hr.serviceops;

import com.bemo.hr.serviceops.api.ServiceOpsApi;
import com.bemo.hr.serviceops.application.BookingService;
import com.bemo.hr.serviceops.domain.BookableResource;
import com.bemo.hr.serviceops.domain.ResourceBooking;
import com.bemo.hr.serviceops.infrastructure.BookableResourceRepository;
import com.bemo.hr.serviceops.infrastructure.ResourceBookingRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTests {

    @Mock
    private BookableResourceRepository resourceRepository;

    @Mock
    private ResourceBookingRepository bookingRepository;

    private BookingService bookingService;

    private static final String APP_ID = "test-app";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        bookingService = new BookingService(resourceRepository, bookingRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createResource_createsAndReturnsResponse() {
        ServiceOpsApi.BookableResourceCreateRequest request = new ServiceOpsApi.BookableResourceCreateRequest(
                "CONF-A", "Conference Room A", "Conference Room A",
                BookableResource.Kind.ROOM, 20, "Building 1, Floor 2"
        );

        when(resourceRepository.save(any(BookableResource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOpsApi.BookableResourceResponse response = bookingService.createResource(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("CONF-A");
        assertThat(response.active()).isTrue();
        verify(resourceRepository).save(any(BookableResource.class));
    }

    @Test
    void createBooking_detectsCollisionAndThrows() {
        BookableResource resource = new BookableResource(APP_ID, "ROOM-1", "Room 1", "Room 1", BookableResource.Kind.ROOM, 10, "HQ");
        when(resourceRepository.findByAppIdAndId(APP_ID, resource.getId())).thenReturn(Optional.of(resource));

        long start = 1756550000000L;
        long end = 1756553600000L;

        ResourceBooking existingBooking = new ResourceBooking(APP_ID, resource.getId(), "Meeting 1", "cust-1", "Client A", start, end, "Notes");
        when(bookingRepository.findConflictingBookings(APP_ID, resource.getId(), start, end))
                .thenReturn(List.of(existingBooking));

        ServiceOpsApi.ResourceBookingCreateRequest request = new ServiceOpsApi.ResourceBookingCreateRequest(
                resource.getId(), "Conflicting Meeting", "cust-2", "Client B", start, end, "Notes"
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BOOKING_COLLISION_DETECTED");
    }

    @Test
    void createBooking_succeedsWhenNoCollision() {
        BookableResource resource = new BookableResource(APP_ID, "ROOM-1", "Room 1", "Room 1", BookableResource.Kind.ROOM, 10, "HQ");
        when(resourceRepository.findByAppIdAndId(APP_ID, resource.getId())).thenReturn(Optional.of(resource));

        long start = 1756550000000L;
        long end = 1756553600000L;

        when(bookingRepository.findConflictingBookings(APP_ID, resource.getId(), start, end))
                .thenReturn(List.of());
        when(bookingRepository.save(any(ResourceBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOpsApi.ResourceBookingCreateRequest request = new ServiceOpsApi.ResourceBookingCreateRequest(
                resource.getId(), "Valid Meeting", "cust-2", "Client B", start, end, "Notes"
        );

        ServiceOpsApi.ResourceBookingResponse response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Valid Meeting");
        assertThat(response.status()).isEqualTo(ResourceBooking.Status.CONFIRMED);
    }
}
