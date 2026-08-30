package com.bemo.hr.platform.deployment.infrastructure;

import com.bemo.hr.platform.deployment.domain.DeploymentHealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentHealthSnapshotRepository extends JpaRepository<DeploymentHealthSnapshot, String> {

    @Query("SELECT s FROM DeploymentHealthSnapshot s ORDER BY s.createdAt DESC")
    List<DeploymentHealthSnapshot> findRecentSnapshots();

    Optional<DeploymentHealthSnapshot> findFirstByOrderByCreatedAtDesc();
}
