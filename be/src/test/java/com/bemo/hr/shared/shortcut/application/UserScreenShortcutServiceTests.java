package com.bemo.hr.shared.shortcut.application;

import com.bemo.hr.access.api.AccessApi;
import com.bemo.hr.access.application.AccessCatalogService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.Role;
import com.bemo.hr.shared.security.RoleCode;
import com.bemo.hr.shared.shortcut.api.ScreenShortcutApi;
import com.bemo.hr.shared.shortcut.domain.ShortcutProfileMode;
import com.bemo.hr.shared.shortcut.domain.UserShortcutProfile;
import com.bemo.hr.shared.shortcut.infrastructure.UserScreenShortcutRepository;
import com.bemo.hr.shared.shortcut.infrastructure.UserShortcutProfileRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserScreenShortcutServiceTests {

    @Mock
    private UserShortcutProfileRepository profileRepository;

    @Mock
    private UserScreenShortcutRepository shortcutRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AccessCatalogService accessCatalogService;

    @Mock
    private AuditService auditService;

    private DefaultScreenShortcutProvider defaultProvider;
    private UserScreenShortcutService service;

    private AppUser testUser;
    private AccessApi.AccessPageResponse pageDashboard;
    private AccessApi.AccessPageResponse pageEmployees;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        defaultProvider = new DefaultScreenShortcutProvider();
        service = new UserScreenShortcutService(
                profileRepository,
                shortcutRepository,
                userRepository,
                accessCatalogService,
                defaultProvider,
                auditService
        );

        Role adminRole = new Role(RoleCode.ADMIN, "Admin");
        testUser = new AppUser("test-app", "testuser", "Test User", "encoded_pass", Set.of(adminRole), Set.of("dashboard", "employees"), true, true);
        lenient().when(userRepository.findByAppIdAndUsernameIgnoreCase(any(), eq("testuser"))).thenReturn(Optional.of(testUser));
        lenient().when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

        pageDashboard = new AccessApi.AccessPageResponse(
                "DASHBOARD", "DASHBOARD", "/dashboard", "dashboard", "nav.dashboard",
                List.of("dashboard.view"), List.of("ADMIN"), null, List.of()
        );
        pageEmployees = new AccessApi.AccessPageResponse(
                "EMPLOYEES", "HR", "/employees", "employees", "nav.employees",
                List.of("employees.read"), List.of("ADMIN"), null, List.of()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void defaultProfileContainsOnlyAvailablePages() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
        when(accessCatalogService.availablePagesForUser(anySet(), anySet()))
                .thenReturn(List.of(pageDashboard, pageEmployees));
        when(accessCatalogService.catalog()).thenReturn(
                new AccessApi.AccessCatalogResponse(List.of(), List.of(pageDashboard, pageEmployees), List.of(), List.of(), List.of())
        );

        ScreenShortcutApi.ProfileResponse profile = service.getProfile("testuser");

        assertThat(profile.profileMode()).isEqualTo(ShortcutProfileMode.DEFAULT.name());
        assertThat(profile.shortcuts()).hasSize(2);
        assertThat(profile.shortcuts().stream().map(ScreenShortcutApi.ShortcutResponse::pageCode))
                .containsExactlyInAnyOrder("DASHBOARD", "EMPLOYEES");
    }

    @Test
    void replaceValidatesAndSavesCustomProfile() {
        UserShortcutProfile userProfile = new UserShortcutProfile(testUser.getId());
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userProfile));
        when(accessCatalogService.availablePagesForUser(anySet(), anySet()))
                .thenReturn(List.of(pageDashboard, pageEmployees));
        when(accessCatalogService.catalog()).thenReturn(
                new AccessApi.AccessCatalogResponse(List.of(), List.of(pageDashboard, pageEmployees), List.of(), List.of(), List.of())
        );

        ScreenShortcutApi.ReplaceShortcutsRequest req = new ScreenShortcutApi.ReplaceShortcutsRequest(
                0L,
                List.of(
                        new ScreenShortcutApi.ShortcutItemRequest("KeyE", "EMPLOYEES", true),
                        new ScreenShortcutApi.ShortcutItemRequest("KeyD", "DASHBOARD", true)
                )
        );

        ScreenShortcutApi.ProfileResponse result = service.replace("testuser", req);

        assertThat(result.profileMode()).isEqualTo(ShortcutProfileMode.CUSTOM.name());
        verify(shortcutRepository).deleteByProfileId(userProfile.getId());
        verify(auditService).record(eq("USER_SHORTCUTS_UPDATE"), eq("USER_SHORTCUT_PROFILE"), eq(userProfile.getId()), eq("testuser"), any(), any());
    }

    @Test
    void duplicateKeyIsRejected() {
        UserShortcutProfile userProfile = new UserShortcutProfile(testUser.getId());
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userProfile));
        when(accessCatalogService.availablePagesForUser(anySet(), anySet()))
                .thenReturn(List.of(pageDashboard, pageEmployees));
        when(accessCatalogService.catalog()).thenReturn(
                new AccessApi.AccessCatalogResponse(List.of(), List.of(pageDashboard, pageEmployees), List.of(), List.of(), List.of())
        );

        ScreenShortcutApi.ReplaceShortcutsRequest req = new ScreenShortcutApi.ReplaceShortcutsRequest(
                0L,
                List.of(
                        new ScreenShortcutApi.ShortcutItemRequest("KeyE", "EMPLOYEES", true),
                        new ScreenShortcutApi.ShortcutItemRequest("KeyE", "DASHBOARD", true)
                )
        );

        Throwable thrown = catchThrowable(() -> service.replace("testuser", req));
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        assertThat(((BusinessRuleException) thrown).getCode()).isEqualTo("SHORTCUT_KEY_DUPLICATE");
    }

    @Test
    void duplicateDestinationIsRejected() {
        UserShortcutProfile userProfile = new UserShortcutProfile(testUser.getId());
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userProfile));
        when(accessCatalogService.availablePagesForUser(anySet(), anySet()))
                .thenReturn(List.of(pageDashboard, pageEmployees));
        when(accessCatalogService.catalog()).thenReturn(
                new AccessApi.AccessCatalogResponse(List.of(), List.of(pageDashboard, pageEmployees), List.of(), List.of(), List.of())
        );

        ScreenShortcutApi.ReplaceShortcutsRequest req = new ScreenShortcutApi.ReplaceShortcutsRequest(
                0L,
                List.of(
                        new ScreenShortcutApi.ShortcutItemRequest("KeyE", "EMPLOYEES", true),
                        new ScreenShortcutApi.ShortcutItemRequest("KeyH", "EMPLOYEES", true)
                )
        );

        Throwable thrown = catchThrowable(() -> service.replace("testuser", req));
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        assertThat(((BusinessRuleException) thrown).getCode()).isEqualTo("SHORTCUT_DESTINATION_DUPLICATE");
    }

    @Test
    void staleVersionIsRejectedWithConflict() {
        UserShortcutProfile userProfile = new UserShortcutProfile(testUser.getId());
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userProfile));

        ScreenShortcutApi.ReplaceShortcutsRequest req = new ScreenShortcutApi.ReplaceShortcutsRequest(
                5L, // expected 5, actual 0
                List.of()
        );

        Throwable thrown = catchThrowable(() -> service.replace("testuser", req));
        assertThat(thrown).isInstanceOf(BusinessRuleException.class);
        BusinessRuleException bre = (BusinessRuleException) thrown;
        assertThat(bre.getCode()).isEqualTo("SHORTCUT_PROFILE_VERSION_CONFLICT");
        assertThat(bre.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void resetRestoresFilteredDefaults() {
        UserShortcutProfile userProfile = new UserShortcutProfile(testUser.getId());
        userProfile.useCustomProfile();

        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userProfile));
        when(accessCatalogService.availablePagesForUser(anySet(), anySet()))
                .thenReturn(List.of(pageDashboard));
        when(accessCatalogService.catalog()).thenReturn(
                new AccessApi.AccessCatalogResponse(List.of(), List.of(pageDashboard), List.of(), List.of(), List.of())
        );

        ScreenShortcutApi.ProfileResponse profile = service.resetToDefaults("testuser");

        assertThat(profile.profileMode()).isEqualTo(ShortcutProfileMode.DEFAULT.name());
        verify(shortcutRepository).deleteByProfileId(userProfile.getId());
        verify(auditService).record(eq("USER_SHORTCUTS_RESET"), eq("USER_SHORTCUT_PROFILE"), eq(userProfile.getId()), eq("testuser"), any(), any());
    }
}
