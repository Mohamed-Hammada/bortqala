package com.bemo.hr.expenses.infrastructure;

import com.bemo.hr.operations.EmployeeAdvanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseReimbursementEntryRepository extends JpaRepository<EmployeeAdvanceEntry, String> {
}
