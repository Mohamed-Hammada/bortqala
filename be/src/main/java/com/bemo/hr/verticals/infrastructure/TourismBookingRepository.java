package com.bemo.hr.verticals.infrastructure;

import com.bemo.hr.verticals.domain.TourismBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourismBookingRepository extends JpaRepository<TourismBooking, String> {
    List<TourismBooking> findAllByOrderByCreatedAtDesc();
    Optional<TourismBooking> findByBookingCode(String bookingCode);
}
