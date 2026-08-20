package com.bemo.hr.verticals.infrastructure;

import com.bemo.hr.verticals.domain.TuitionInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TuitionInvoiceRepository extends JpaRepository<TuitionInvoice, String> {
    List<TuitionInvoice> findByEnrollmentIdOrderByDueDateAsc(String enrollmentId);
    List<TuitionInvoice> findAllByOrderByCreatedAtDesc();
}
