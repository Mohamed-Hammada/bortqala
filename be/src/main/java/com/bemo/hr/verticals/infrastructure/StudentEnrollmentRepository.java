package com.bemo.hr.verticals.infrastructure;

import com.bemo.hr.verticals.domain.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, String> {
    List<StudentEnrollment> findAllByOrderByCreatedAtDesc();
    Optional<StudentEnrollment> findByStudentCode(String studentCode);
}
