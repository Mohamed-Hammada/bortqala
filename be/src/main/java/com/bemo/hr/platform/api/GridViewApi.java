package com.bemo.hr.platform.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class GridViewApi {
    private GridViewApi() {}

    public record GridViewSaveRequest(
            @NotBlank String pageKey,
            @NotBlank String name,
            String filters,
            String hiddenColumns,
            String sort,
            String sharedRoles
    ) {}

    public record GridViewResponse(
            String id, String userId, String pageKey, String name,
            String filters, String hiddenColumns, String sort,
            String sharedRoles, long createdAtEpochMs
    ) {}

    public record GridViewListResponse(List<GridViewResponse> views) {}
}
