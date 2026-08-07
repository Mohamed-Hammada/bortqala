package com.bemo.hr.budget.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncumbranceRepository extends JpaRepository<Encumbrance, String> {

    List<Encumbrance> findByPurchaseOrderId(String purchaseOrderId);

    List<Encumbrance> findAllByOrderByCommittedAtDesc();

    Optional<Encumbrance> findFirstByPurchaseOrderIdAndStatus(String purchaseOrderId, EncumbranceStatus status);

    List<Encumbrance> findByBudgetId(String budgetId);
}
