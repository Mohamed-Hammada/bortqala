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
@Table(name = "school_student_enrollments")
@Getter
@Setter
@NoArgsConstructor
public class StudentEnrollment {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @TenantId
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "student_code", length = 64, nullable = false)
    private String studentCode;

    @Column(name = "student_name", length = 255, nullable = false)
    private String studentName;

    @Column(name = "grade_level", length = 64, nullable = false)
    private String gradeLevel;

    @Column(name = "academic_year", length = 32, nullable = false)
    private String academicYear;

    @Column(name = "guardian_name", length = 255, nullable = false)
    private String guardianName;

    @Column(name = "guardian_phone", length = 64)
    private String guardianPhone;

    @Column(name = "total_tuition_fee", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalTuitionFee;

    @Column(name = "transport_fee", precision = 18, scale = 2, nullable = false)
    private BigDecimal transportFee;

    @Column(name = "books_fee", precision = 18, scale = 2, nullable = false)
    private BigDecimal booksFee;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
}
