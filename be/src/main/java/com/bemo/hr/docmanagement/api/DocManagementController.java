package com.bemo.hr.docmanagement.api;

import com.bemo.hr.docmanagement.application.DocManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocManagementController {

    private final DocManagementService service;

    public DocManagementController(DocManagementService service) {
        this.service = service;
    }

    @GetMapping("/folders")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<DocManagementApi.FolderResponse>> listFolders() {
        return ResponseEntity.ok(service.listFolders());
    }

    @PostMapping("/folders")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<DocManagementApi.FolderResponse> createFolder(
            @RequestBody @Valid DocManagementApi.CreateFolderRequest request) {
        return ResponseEntity.ok(service.createFolder(request));
    }

    @PutMapping("/folders/{id}/rename")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<Void> renameFolder(@PathVariable String id, @RequestParam String name) {
        service.renameFolder(id, name);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/folders/{id}/move")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<Void> moveFolder(@PathVariable String id, @RequestParam(required = false) String parentId) {
        service.moveFolder(id, parentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<Void> deleteFolder(@PathVariable String id) {
        service.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<DocManagementApi.TagResponse>> listTags() {
        return ResponseEntity.ok(service.listTags());
    }

    @PostMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<DocManagementApi.TagResponse> createTag(
            @RequestBody @Valid DocManagementApi.CreateTagRequest request) {
        return ResponseEntity.ok(service.createTag(request));
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<Void> deleteTag(@PathVariable String id) {
        service.deleteTag(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/attachments/{attachmentId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<Void> assignTag(@PathVariable String attachmentId, @PathVariable String tagId) {
        service.assignTag(attachmentId, tagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/attachments/{attachmentId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<Void> removeTag(@PathVariable String attachmentId, @PathVariable String tagId) {
        service.removeTag(attachmentId, tagId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attachments/{attachmentId}/tags")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<DocManagementApi.TagResponse>> getTagsForAttachment(@PathVariable String attachmentId) {
        return ResponseEntity.ok(service.getTagsForAttachment(attachmentId));
    }

    @GetMapping("/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<DocManagementApi.DocumentSearchResult>> searchAttachments(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(service.searchAttachments(q, tag));
    }
}
