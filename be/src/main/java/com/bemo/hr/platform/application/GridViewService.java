package com.bemo.hr.platform.application;

import com.bemo.hr.platform.api.GridViewApi;
import com.bemo.hr.platform.domain.GridView;
import com.bemo.hr.platform.domain.GridViewRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class GridViewService {

    private final GridViewRepository gridViewRepository;

    public GridViewService(GridViewRepository gridViewRepository) {
        this.gridViewRepository = gridViewRepository;
    }

    public GridViewApi.GridViewResponse saveView(String appId, String userId, GridViewApi.GridViewSaveRequest request) {
        GridView view = new GridView(appId, userId, request.pageKey(), request.name(),
                request.filters(), request.hiddenColumns(), request.sort());
        if (request.sharedRoles() != null) {
            view.setSharedRoles(request.sharedRoles());
        }
        gridViewRepository.save(view);
        return toResponse(view);
    }

    public GridViewApi.GridViewListResponse listViews(String appId, String userId, String pageKey) {
        List<GridViewApi.GridViewResponse> own = gridViewRepository
                .findByAppIdAndUserIdAndPageKeyOrderByCreatedAtDesc(appId, userId, pageKey)
                .stream().map(this::toResponse).toList();

        List<GridViewApi.GridViewResponse> shared = gridViewRepository
                .findByAppIdAndPageKeyOrderByCreatedAtDesc(appId, pageKey)
                .stream()
                .filter(v -> !v.getUserId().equals(userId))
                .filter(v -> v.getSharedRoles() != null && !v.getSharedRoles().isBlank())
                .map(this::toResponse).toList();

        List<GridViewApi.GridViewResponse> all = new java.util.ArrayList<>(own);
        all.addAll(shared);
        return new GridViewApi.GridViewListResponse(all);
    }

    public void deleteView(String appId, String userId, String viewId) {
        GridView view = gridViewRepository.findById(viewId)
                .filter(v -> v.getAppId().equals(appId))
                .orElseThrow(() -> new NotFoundException("Grid view not found", "GRID_VIEW_NOT_FOUND"));
        if (!view.getUserId().equals(userId)) {
            throw new BusinessRuleException("Cannot delete a view owned by another user", "GRID_VIEW_NOT_OWNER", HttpStatus.FORBIDDEN);
        }
        gridViewRepository.delete(view);
    }

    private GridViewApi.GridViewResponse toResponse(GridView v) {
        return new GridViewApi.GridViewResponse(
                v.getId(), v.getUserId(), v.getPageKey(), v.getName(),
                v.getFilters(), v.getHiddenColumns(), v.getSort(),
                v.getSharedRoles(), v.getCreatedAt().toEpochMilli()
        );
    }
}
