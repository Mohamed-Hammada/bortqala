package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.domain.BiometricSource.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BiometricSourceRepository extends JpaRepository<BiometricSource, String> {
    Optional<BiometricSource> findBySourceTypeAndNormalizedCode(SourceType sourceType, String normalizedCode);
    List<BiometricSource> findAllByOrderBySourceTypeAscNameAsc();

    /**
     * Registers a source without ever failing on a concurrent duplicate, so two
     * device syncs racing to create the same source both succeed.
     */
    @Modifying
    @Query(value = """
            INSERT INTO biometric_sources (
                id, app_id, source_type, name, normalized_code, active, created_at
            ) VALUES (
                :id, :appId, :sourceType, :name, :normalizedCode, TRUE, CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("appId") String appId,
                       @Param("sourceType") String sourceType, @Param("name") String name,
                       @Param("normalizedCode") String normalizedCode);
}
