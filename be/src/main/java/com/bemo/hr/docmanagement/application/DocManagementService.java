package com.bemo.hr.docmanagement.application;

import com.bemo.hr.docmanagement.api.DocManagementApi;
import com.bemo.hr.docmanagement.domain.DocAttachmentTag;
import com.bemo.hr.docmanagement.domain.DocFolder;
import com.bemo.hr.docmanagement.domain.DocTag;
import com.bemo.hr.docmanagement.infrastructure.DocAttachmentTagRepository;
import com.bemo.hr.docmanagement.infrastructure.DocFolderRepository;
import com.bemo.hr.docmanagement.infrastructure.DocTagRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocManagementService {

    private final DocFolderRepository folderRepository;
    private final DocTagRepository tagRepository;
    private final DocAttachmentTagRepository attachmentTagRepository;

    public DocManagementService(DocFolderRepository folderRepository,
                                DocTagRepository tagRepository,
                                DocAttachmentTagRepository attachmentTagRepository) {
        this.folderRepository = folderRepository;
        this.tagRepository = tagRepository;
        this.attachmentTagRepository = attachmentTagRepository;
    }

    // ---- Folders ----

    @Transactional(readOnly = true)
    public List<DocManagementApi.FolderResponse> listFolders() {
        return folderRepository.findAllByOrderByNameAsc().stream()
                .map(f -> new DocManagementApi.FolderResponse(f.getId(), f.getName(), f.getParentId(), f.getCreatedAt()))
                .toList();
    }

    @Transactional
    public DocManagementApi.FolderResponse createFolder(DocManagementApi.CreateFolderRequest request) {
        DocFolder folder = new DocFolder(request.name(), request.parentId());
        DocFolder saved = folderRepository.save(folder);
        return new DocManagementApi.FolderResponse(saved.getId(), saved.getName(), saved.getParentId(), saved.getCreatedAt());
    }

    @Transactional
    public void renameFolder(String id, String newName) {
        DocFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Folder not found", "DOC_FOLDER_NOT_FOUND", HttpStatus.NOT_FOUND));
        folder.rename(newName);
        folderRepository.save(folder);
    }

    @Transactional
    public void moveFolder(String id, String newParentId) {
        DocFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Folder not found", "DOC_FOLDER_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (newParentId != null && newParentId.equals(id)) {
            throw new BusinessRuleException("Cannot move folder into itself", "DOC_FOLDER_SELF_MOVE", HttpStatus.CONFLICT);
        }
        folder.move(newParentId);
        folderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(String id) {
        if (!folderRepository.existsById(id)) {
            throw new BusinessRuleException("Folder not found", "DOC_FOLDER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        folderRepository.deleteById(id);
    }

    // ---- Tags ----

    @Transactional(readOnly = true)
    public List<DocManagementApi.TagResponse> listTags() {
        return tagRepository.findAll().stream()
                .map(t -> new DocManagementApi.TagResponse(t.getId(), t.getName(), t.getColor()))
                .toList();
    }

    @Transactional
    public DocManagementApi.TagResponse createTag(DocManagementApi.CreateTagRequest request) {
        DocTag tag = new DocTag(request.name(), request.color());
        DocTag saved = tagRepository.save(tag);
        return new DocManagementApi.TagResponse(saved.getId(), saved.getName(), saved.getColor());
    }

    @Transactional
    public void deleteTag(String id) {
        if (!tagRepository.existsById(id)) {
            throw new BusinessRuleException("Tag not found", "DOC_TAG_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        tagRepository.deleteById(id);
    }

    // ---- Tag assignment ----

    @Transactional
    public void assignTag(String attachmentId, String tagId) {
        boolean exists = attachmentTagRepository.findByAttachmentId(attachmentId).stream()
                .anyMatch(t -> t.getTagId().equals(tagId));
        if (!exists) {
            attachmentTagRepository.save(new DocAttachmentTag(attachmentId, tagId));
        }
    }

    @Transactional
    public void removeTag(String attachmentId, String tagId) {
        attachmentTagRepository.findByAttachmentId(attachmentId).stream()
                .filter(t -> t.getTagId().equals(tagId))
                .findFirst()
                .ifPresent(attachmentTagRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<DocManagementApi.TagResponse> getTagsForAttachment(String attachmentId) {
        Set<String> tagIds = attachmentTagRepository.findByAttachmentId(attachmentId).stream()
                .map(DocAttachmentTag::getTagId).collect(Collectors.toSet());
        return tagRepository.findAllById(tagIds).stream()
                .map(t -> new DocManagementApi.TagResponse(t.getId(), t.getName(), t.getColor()))
                .toList();
    }

    // ---- Search ----

    @Transactional(readOnly = true)
    public List<DocManagementApi.DocumentSearchResult> searchAttachments(String q, String tagId) {
        Set<String> attachmentIds = new LinkedHashSet<>();
        if (tagId != null && !tagId.isBlank()) {
            attachmentTagRepository.findByTagId(tagId).forEach(t -> attachmentIds.add(t.getAttachmentId()));
        } else {
            attachmentTagRepository.findAll().forEach(t -> attachmentIds.add(t.getAttachmentId()));
        }
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase(Locale.ROOT);
            attachmentIds.removeIf(id -> !id.toLowerCase(Locale.ROOT).contains(needle));
        }
        List<DocManagementApi.DocumentSearchResult> results = new ArrayList<>();
        for (String attachmentId : attachmentIds) {
            List<DocManagementApi.TagResponse> tags = getTagsForAttachment(attachmentId);
            results.add(new DocManagementApi.DocumentSearchResult(
                    attachmentId, attachmentId, "attachment", null, null, tags, 0L));
        }
        results.sort(Comparator.comparing(DocManagementApi.DocumentSearchResult::name));
        return results;
    }
}
