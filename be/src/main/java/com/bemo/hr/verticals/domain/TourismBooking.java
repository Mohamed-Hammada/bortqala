package com.bemo.hr.verticals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

@Entity
@Table(name = "tourism_package_bookings")
@Getter
@Setter
@NoArgsConstructor
public class TourismBooking {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "booking_code", length = 64, nullable = false)
    private String bookingCode;

    @Column(name = "customer_name", length = 255, nullable = false)
    private String customerName;

    @Column(name = "package_name", length = 255, nullable = false)
    private String packageName;

    @Column(name = "destination", length = 128, nullable = false)
    private String destination;

    @Column(name = "travel_date", nullable = false)
    private long travelDate;

    @Column(name = "return_date", nullable = false)
    private long returnDate;

    @Column(name = "travelers_count", nullable = false)
    private int travelersCount;

    @Column(name = "selling_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal sellingPrice;

    @Column(name = "hotel_cost", precision = 18, scale = 2, nullable = false)
    private BigDecimal hotelCost;

    @Column(name = "flight_cost", precision = 18, scale = 2, nullable = false)
    private BigDecimal flightCost;

    @Column(name = "excursion_cost", precision = 18, scale = 2, nullable = false)
    private BigDecimal excursionCost;

    @Column(name = "gross_margin", precision = 18, scale = 2, nullable = false)
    private BigDecimal grossMargin;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
}
