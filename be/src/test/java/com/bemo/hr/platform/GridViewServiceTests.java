package com.bemo.hr.platform;

import com.bemo.hr.platform.api.GridViewApi;
import com.bemo.hr.platform.application.GridViewService;
import com.bemo.hr.platform.domain.GridView;
import com.bemo.hr.platform.domain.GridViewRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GridViewServiceTests {

    @Mock GridViewRepository gridViewRepo;
    GridViewService service;

    @BeforeEach
    void setUp() {
        service = new GridViewService(gridViewRepo);
    }

    @Test
    void saveView_persistsAndReturns() {
        when(gridViewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new GridViewApi.GridViewSaveRequest("employees", "Active Staff", "[]", null, null, null);
        var result = service.saveView("app-1", "user-1", req);

        assertNotNull(result.id());
        assertEquals("employees", result.pageKey());
        assertEquals("Active Staff", result.name());
        assertEquals("user-1", result.userId());
    }

    @Test
    void saveView_withSharedRoles() {
        when(gridViewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new GridViewApi.GridViewSaveRequest("employees", "Shared View", "[]", null, null, "ADMIN,HR_MANAGER");
        var result = service.saveView("app-1", "user-1", req);
        assertEquals("ADMIN,HR_MANAGER", result.sharedRoles());
    }

    @Test
    void listViews_includesOwnAndShared() {
        GridView own = mock(GridView.class);
        when(own.getId()).thenReturn("v1");
        when(own.getUserId()).thenReturn("user-1");
        when(own.getPageKey()).thenReturn("employees");
        when(own.getName()).thenReturn("My View");
        when(own.getFilters()).thenReturn("[]");
        when(own.getHiddenColumns()).thenReturn(null);
        when(own.getSort()).thenReturn(null);
        when(own.getSharedRoles()).thenReturn(null);
        when(own.getCreatedAt()).thenReturn(Instant.now());

        GridView shared = mock(GridView.class);
        when(shared.getId()).thenReturn("v2");
        when(shared.getUserId()).thenReturn("user-2");
        when(shared.getPageKey()).thenReturn("employees");
        when(shared.getName()).thenReturn("Team View");
        when(shared.getFilters()).thenReturn("[]");
        when(shared.getHiddenColumns()).thenReturn(null);
        when(shared.getSort()).thenReturn(null);
        when(shared.getSharedRoles()).thenReturn("ADMIN");
        when(shared.getCreatedAt()).thenReturn(Instant.now());

        when(gridViewRepo.findByAppIdAndUserIdAndPageKeyOrderByCreatedAtDesc("app-1", "user-1", "employees"))
                .thenReturn(List.of(own));
        when(gridViewRepo.findByAppIdAndPageKeyOrderByCreatedAtDesc("app-1", "employees"))
                .thenReturn(List.of(own, shared));

        var result = service.listViews("app-1", "user-1", "employees");
        assertEquals(2, result.views().size());
    }

    @Test
    void listViews_excludesOthersNonShared() {
        GridView other = mock(GridView.class, withSettings().stubOnly());
        when(other.getUserId()).thenReturn("user-3");
        when(other.getSharedRoles()).thenReturn(null);

        when(gridViewRepo.findByAppIdAndUserIdAndPageKeyOrderByCreatedAtDesc("app-1", "user-1", "employees"))
                .thenReturn(List.of());
        when(gridViewRepo.findByAppIdAndPageKeyOrderByCreatedAtDesc("app-1", "employees"))
                .thenReturn(List.of(other));

        var result = service.listViews("app-1", "user-1", "employees");
        assertTrue(result.views().isEmpty());
    }

    @Test
    void deleteView_notFound_throws() {
        when(gridViewRepo.findById("bad")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.deleteView("app-1", "user-1", "bad"));
    }

    @Test
    void deleteView_notOwner_throws() {
        GridView view = mock(GridView.class);
        when(gridViewRepo.findById("v1")).thenReturn(Optional.of(view));
        when(view.getAppId()).thenReturn("app-1");
        when(view.getUserId()).thenReturn("user-2");

        assertThrows(BusinessRuleException.class, () -> service.deleteView("app-1", "user-1", "v1"));
    }

    @Test
    void deleteView_ownerSucceeds() {
        GridView view = mock(GridView.class);
        when(gridViewRepo.findById("v1")).thenReturn(Optional.of(view));
        when(view.getAppId()).thenReturn("app-1");
        when(view.getUserId()).thenReturn("user-1");

        service.deleteView("app-1", "user-1", "v1");
        verify(gridViewRepo).delete(view);
    }
}
