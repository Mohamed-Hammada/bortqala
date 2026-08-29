package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRoomRepository extends JpaRepository<HospitalRoom, String> {

    Optional<HospitalRoom> findByAppIdAndId(String appId, String id);

    List<HospitalRoom> findAllByAppIdAndWardIdOrderByRoomNumberAsc(String appId, String wardId);

    List<HospitalRoom> findAllByAppIdOrderByRoomNumberAsc(String appId);
}
