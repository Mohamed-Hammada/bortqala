package com.bemo.hr.docmanagement;

import com.bemo.hr.docmanagement.api.DocManagementApi;
import com.bemo.hr.docmanagement.application.DocManagementService;
import com.bemo.hr.docmanagement.domain.DocAttachmentTag;
import com.bemo.hr.docmanagement.domain.DocFolder;
import com.bemo.hr.docmanagement.domain.DocTag;
import com.bemo.hr.docmanagement.infrastructure.DocAttachmentTagRepository;
import com.bemo.hr.docmanagement.infrastructure.DocFolderRepository;
import com.bemo.hr.docmanagement.infrastructure.DocTagRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocManagementServiceTest {

    @Mock private DocFolderRepository folderRepository;
    @Mock private DocTagRepository tagRepository;
    @Mock private DocAttachmentTagRepository attachmentTagRepository;

    @InjectMocks
    private DocManagementService service;

    @Test
    void createFolder_savesAndReturnsResponse() {
        when(folderRepository.save(any(DocFolder.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new DocManagementApi.CreateFolderRequest("Finance", null);
        var res = service.createFolder(req);

        assertThat(res.name()).isEqualTo("Finance");
        assertThat(res.parentId()).isNull();
        verify(folderRepository).save(any(DocFolder.class));
    }

    @Test
    void moveFolder_selfMove_throwsException() {
        DocFolder folder = new DocFolder("HR", null);
        when(folderRepository.findById(folder.getId())).thenReturn(Optional.of(folder));

        assertThatThrownBy(() -> service.moveFolder(folder.getId(), folder.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("DOC_FOLDER_SELF_MOVE"));
    }

    @Test
    void renameFolder_updatesName() {
        DocFolder folder = new DocFolder("HR Old", null);
        when(folderRepository.findById(folder.getId())).thenReturn(Optional.of(folder));
        when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.renameFolder(folder.getId(), "HR New");

        assertThat(folder.getName()).isEqualTo("HR New");
        verify(folderRepository).save(folder);
    }

    @Test
    void createTag_savesTag() {
        when(tagRepository.save(any(DocTag.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new DocManagementApi.CreateTagRequest("Confidential", "#FF0000");
        var res = service.createTag(req);

        assertThat(res.name()).isEqualTo("Confidential");
        assertThat(res.color()).isEqualTo("#FF0000");
    }

    @Test
    void assignTag_savesAttachmentTagIfNotPresent() {
        when(attachmentTagRepository.findByAttachmentId("att-1")).thenReturn(List.of());
        when(attachmentTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignTag("att-1", "tag-1");

        verify(attachmentTagRepository).save(any(DocAttachmentTag.class));
    }

    @Test
    void removeTag_deletesAttachmentTag() {
        DocAttachmentTag tag = new DocAttachmentTag("att-1", "tag-1");
        when(attachmentTagRepository.findByAttachmentId("att-1")).thenReturn(List.of(tag));

        service.removeTag("att-1", "tag-1");

        verify(attachmentTagRepository).delete(tag);
    }

    @Test
    void searchAttachments_filtersByQueryAndTag() {
        DocAttachmentTag tag1 = new DocAttachmentTag("contract-2026.pdf", "tag-1");
        DocAttachmentTag tag2 = new DocAttachmentTag("invoice-001.pdf", "tag-1");

        when(attachmentTagRepository.findByTagId("tag-1")).thenReturn(List.of(tag1, tag2));
        when(attachmentTagRepository.findByAttachmentId("contract-2026.pdf")).thenReturn(List.of(tag1));
        when(tagRepository.findAllById(Set.of("tag-1"))).thenReturn(List.of(new DocTag("Legal", "#00FF00")));

        var results = service.searchAttachments("contract", "tag-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo("contract-2026.pdf");
    }
}
