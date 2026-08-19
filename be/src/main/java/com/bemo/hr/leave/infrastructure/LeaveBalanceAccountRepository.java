package com.bemo.hr.leave.infrastructure;

import com.bemo.hr.leave.domain.LeaveBalanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceAccountRepository extends JpaRepository<LeaveBalanceAccount, String> {

    List<LeaveBalanceAccount> findByEmployeeIdAndYear(String employeeId, int year);

    List<LeaveBalanceAccount> findByYear(int year);

    Optional<LeaveBalanceAccount> findByEmployeeIdAndLeaveTypeIdAndYear(String employeeId, String leaveTypeId, int year);
}
