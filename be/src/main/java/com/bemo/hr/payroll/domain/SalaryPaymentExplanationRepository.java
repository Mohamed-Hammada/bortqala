package com.bemo.hr.payroll.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryPaymentExplanationRepository extends JpaRepository<SalaryPaymentExplanation, String> {
    List<SalaryPaymentExplanation> findBySalaryPaymentIdOrderByCreatedAtAsc(String salaryPaymentId);
    void deleteBySalaryPaymentId(String salaryPaymentId);
}
