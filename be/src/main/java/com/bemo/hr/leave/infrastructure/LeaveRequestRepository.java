package com.bemo.hr.leave.infrastructure;

import com.bemo.hr.leave.domain.LeaveRequest;
import com.bemo.hr.leave.domain.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveRequestStatus status);

    boolean existsByRequestNumber(String requestNumber);

    @Query("SELECT r FROM LeaveRequest r WHERE r.employeeId = :employeeId AND r.status IN ('PENDING_APPROVAL', 'APPROVED') " +
            "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<LeaveRequest> findOverlappingRequests(@Param("employeeId") String employeeId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
