package com.bemo.hr.product.trial;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface DemoSampleRecordRepository extends JpaRepository<DemoSampleRecord,String> {
    List<DemoSampleRecord> findAllByOrderByRecordKey();
    @Modifying @Query("delete from DemoSampleRecord r where r.appId=:appId")
    int deleteOwnedByTenant(@Param("appId") String appId);
}
