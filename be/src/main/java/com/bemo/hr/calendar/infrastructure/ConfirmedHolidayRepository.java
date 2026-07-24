package com.bemo.hr.calendar.infrastructure;

import com.bemo.hr.calendar.domain.ConfirmedHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConfirmedHolidayRepository extends JpaRepository<ConfirmedHoliday, String> {
    List<ConfirmedHoliday> findByWorkDateBetween(LocalDate from, LocalDate to);
    Optional<ConfirmedHoliday> findByCategoryIdAndWorkDate(String categoryId, LocalDate workDate);
}
