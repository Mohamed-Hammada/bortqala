package com.bemo.hr.assets.infrastructure;

import com.bemo.hr.assets.domain.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, String> {

    List<FixedAsset> findAllByOrderByAcquisitionDateDesc();

    /** Run scope: assets that still carry a depreciable balance. */
    List<FixedAsset> findByStatusInOrderByAcquisitionDateAsc(List<String> statuses);
}
