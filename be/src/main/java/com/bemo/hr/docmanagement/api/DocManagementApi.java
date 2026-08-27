package com.bemo.hr.docmanagement.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class DocManagementApi {

    private DocManagementApi() {
    }

    public record FolderResponse(String id, String name, String parentId, long createdAt) {
    }

    public record CreateFolderRequest(@NotBlank String name, String parentId) {
    }

    public record TagResponse(String id, String name, String color) {
    }

    public record CreateTagRequest(@NotBlank String name, String color) {
    }

    public record DocumentSearchResult(
            String id,
            String name,
            String kind,
            String folderId,
            String folderName,
            List<TagResponse> tags,
            long createdAt
    ) {
    }

    public record AssignTagRequest(@NotBlank String tagId) {
    }
}
