package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.EffectiveMasterValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EffectiveMasterValueRepository extends JpaRepository<EffectiveMasterValue, String> {
    List<EffectiveMasterValue> findByMasterTypeAndMasterIdAndValueKeyOrderByEffectiveFromDesc(String type, String id, String key);
}
