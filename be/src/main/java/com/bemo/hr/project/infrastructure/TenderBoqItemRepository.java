package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.TenderBoqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenderBoqItemRepository extends JpaRepository<TenderBoqItem, String> {

    List<TenderBoqItem> findByTenderIdOrderBySortOrderAsc(String tenderId);

    void deleteByTenderId(String tenderId);
}
