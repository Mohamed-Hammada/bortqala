package com.bemo.hr.product.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerHealthSnapshotRepository extends JpaRepository<CustomerHealthSnapshot, String> {
    Optional<CustomerHealthSnapshot> findByOperationId(String operationId);

    Optional<CustomerHealthSnapshot> findFirstByOrderByCalculatedAtDesc();
}
