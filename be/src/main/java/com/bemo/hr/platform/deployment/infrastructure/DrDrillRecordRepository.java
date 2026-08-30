package com.bemo.hr.platform.deployment.infrastructure;

import com.bemo.hr.platform.deployment.domain.DrDrillRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrDrillRecordRepository extends JpaRepository<DrDrillRecord, String> {

    @Query("SELECT d FROM DrDrillRecord d ORDER BY d.conductedAt DESC")
    List<DrDrillRecord> findAllOrdered();
}
