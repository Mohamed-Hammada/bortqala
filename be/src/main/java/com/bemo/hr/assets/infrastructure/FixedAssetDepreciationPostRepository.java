package com.bemo.hr.assets.infrastructure;

import com.bemo.hr.assets.domain.FixedAssetDepreciationPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FixedAssetDepreciationPostRepository extends JpaRepository<FixedAssetDepreciationPost, String> {

    boolean existsByAssetIdAndYearMonth(String assetId, String yearMonth);
}
