package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.EmployeeCodeSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmployeeCodeSequenceRepository extends JpaRepository<EmployeeCodeSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from EmployeeCodeSequence sequence where sequence.categoryId = :categoryId")
    Optional<EmployeeCodeSequence> findForUpdate(String categoryId);
}
