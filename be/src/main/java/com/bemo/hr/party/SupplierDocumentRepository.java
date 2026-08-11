package com.bemo.hr.party;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, String> {
    List<SupplierDocument> findBySupplierIdOrderByCreatedAtDesc(String supplierId);
}
