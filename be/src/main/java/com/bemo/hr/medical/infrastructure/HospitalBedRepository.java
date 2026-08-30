package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalBed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalBedRepository extends JpaRepository<HospitalBed, String> {

    Optional<HospitalBed> findByAppIdAndId(String appId, String id);

    List<HospitalBed> findAllByAppIdAndRoomIdOrderByBedNumberAsc(String appId, String roomId);

    List<HospitalBed> findAllByAppIdOrderByBedNumberAsc(String appId);

    List<HospitalBed> findAllByAppIdAndStatus(String appId, HospitalBed.Status status);

    @Query("SELECT COUNT(b) FROM HospitalBed b WHERE b.appId = :appId AND b.active = true")
    long countTotalActiveBeds(@Param("appId") String appId);

    @Query("SELECT COUNT(b) FROM HospitalBed b WHERE b.appId = :appId AND b.status = 'OCCUPIED' AND b.active = true")
    long countOccupiedBeds(@Param("appId") String appId);
}
