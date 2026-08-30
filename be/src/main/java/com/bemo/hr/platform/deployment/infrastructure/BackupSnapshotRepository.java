package com.bemo.hr.platform.deployment.infrastructure;

import com.bemo.hr.platform.deployment.domain.BackupSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupSnapshotRepository extends JpaRepository<BackupSnapshot, String> {

    @Query("SELECT b FROM BackupSnapshot b ORDER BY b.createdAt DESC")
    List<BackupSnapshot> findAllOrdered();

    long countByStatus(String status);
}
