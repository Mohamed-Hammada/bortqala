package com.bemo.hr.docmanagement.infrastructure;

import com.bemo.hr.docmanagement.domain.DocTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocTagRepository extends JpaRepository<DocTag, String> {
    List<DocTag> findAllByNameContainingIgnoreCase(String name);
}
