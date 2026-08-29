package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.SiteCustodyReturn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteCustodyReturnRepository extends JpaRepository<SiteCustodyReturn, String> {
    List<SiteCustodyReturn> findByCustodyIdOrderByReturnDateDesc(String custodyId);
}
