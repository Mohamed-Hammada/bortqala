package com.bemo.hr.docmanagement.infrastructure;

import com.bemo.hr.docmanagement.domain.DocFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocFolderRepository extends JpaRepository<DocFolder, String> {
    List<DocFolder> findByParentIdOrderByNameAsc(String parentId);
    List<DocFolder> findAllByOrderByNameAsc();
}
