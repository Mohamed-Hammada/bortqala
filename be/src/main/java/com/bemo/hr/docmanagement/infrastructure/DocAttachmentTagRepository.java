package com.bemo.hr.docmanagement.infrastructure;

import com.bemo.hr.docmanagement.domain.DocAttachmentTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocAttachmentTagRepository extends JpaRepository<DocAttachmentTag, Long> {
    List<DocAttachmentTag> findByAttachmentId(String attachmentId);
    List<DocAttachmentTag> findByTagId(String tagId);
    void deleteByAttachmentId(String attachmentId);
}
