package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.AttendanceSelfiePunch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceSelfiePunchRepository extends JpaRepository<AttendanceSelfiePunch, String> {

    Optional<AttendanceSelfiePunch> findByOperationId(String operationId);
}
