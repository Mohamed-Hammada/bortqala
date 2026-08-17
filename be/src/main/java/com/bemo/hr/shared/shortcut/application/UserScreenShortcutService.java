package com.bemo.hr.shared.shortcut.application;

import com.bemo.hr.access.api.AccessApi;
import com.bemo.hr.access.application.AccessCatalogService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.shortcut.api.ScreenShortcutApi;
import com.bemo.hr.shared.shortcut.domain.ShortcutAvailability;
import com.bemo.hr.shared.shortcut.domain.ShortcutProfileMode;
import com.bemo.hr.shared.shortcut.domain.UserScreenShortcut;
import com.bemo.hr.shared.shortcut.domain.UserShortcutProfile;
import com.bemo.hr.shared.shortcut.infrastructure.UserScreenShortcutRepository;
import com.bemo.hr.shared.shortcut.infrastructure.UserShortcutProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserScreenShortcutService {

    private static final int MAX_SHORTCUTS = 20;
    private static final Pattern ALLOWED_KEY_CODE = Pattern.compile("Key[A-Z]|Digit[0-9]");

    private final UserShortcutProfileRepository profileRepository;
    private final UserScreenShortcutRepository shortcutRepository;
    private final AppUserRepository userRepository;
    private final AccessCatalogService accessCatalogService;
    private final DefaultScreenShortcutProvider defaultProvider;
    private final AuditService auditService;

    public UserScreenShortcutService(
            UserShortcutProfileRepository profileRepository,
            UserScreenShortcutRepository shortcutRepository,
            AppUserRepository userRepository,
            AccessCatalogService accessCatalogService,
            DefaultScreenShortcutProvider defaultProvider,
            AuditService auditService
    ) {
        this.profileRepository = profileRepository;
        this.shortcutRepository = shortcutRepository;
        this.userRepository = userRepository;
        this.accessCatalogService = accessCatalogService;
        this.defaultProvider = defaultProvider;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ScreenShortcutApi.ProfileResponse getProfile(String username) {
        AppUser user = requireCurrentUser(username);
        UserShortcutProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);

        List<AccessApi.AccessPageResponse> availablePages = accessCatalogService.availablePagesForUser(
                getRoleCodes(user),
                effectiveMenus(user)
        );

        List<ScreenShortcutApi.DestinationResponse> destinationResponses = availablePages.stream()
                .map(p -> new ScreenShortcutApi.DestinationResponse(
                        p.code(),
                        p.menuId(),
                        p.route(),
                        p.titleKey(),
                        p.module(),
                        p.requiredFeature()
                ))
                .toList();

        if (profile == null || profile.getProfileMode() == ShortcutProfileMode.DEFAULT) {
            return buildDefaultProfile(profile, availablePages, destinationResponses);
        }

        return buildCustomProfile(profile, availablePages, destinationResponses);
    }

    public ScreenShortcutApi.ProfileResponse replace(
            String username,
            ScreenShortcutApi.ReplaceShortcutsRequest request
    ) {
        AppUser user = requireCurrentUser(username);
        UserShortcutProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> profileRepository.save(new UserShortcutProfile(user.getId())));

        if (profile.getVersion() != request.expectedVersion()) {
            throw new BusinessRuleException(
                    "Your shortcut settings changed in another session. Please reload.",
                    "SHORTCUT_PROFILE_VERSION_CONFLICT",
                    HttpStatus.CONFLICT
            );
        }

        validateRequest(user, request.shortcuts());

        List<UserScreenShortcut> previous = shortcutRepository.findByProfileIdOrderBySortOrderAsc(profile.getId());
        shortcutRepository.deleteByProfileId(profile.getId());

        int order = 0;
        for (var item : request.shortcuts()) {
            shortcutRepository.save(new UserScreenShortcut(
                    profile.getId(),
                    item.pageCode(),
                    item.secondKeyCode(),
                    item.enabled(),
                    order++
            ));
        }

        profile.useCustomProfile();
        profileRepository.save(profile);

        auditService.record(
                "USER_SHORTCUTS_UPDATE",
                "USER_SHORTCUT_PROFILE",
                profile.getId(),
                username,
                buildChangeDetails(previous, request.shortcuts()),
                null
        );

        return getProfile(username);
    }

    public ScreenShortcutApi.ProfileResponse resetToDefaults(String username) {
        AppUser user = requireCurrentUser(username);
        UserShortcutProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> profileRepository.save(new UserShortcutProfile(user.getId())));

        shortcutRepository.deleteByProfileId(profile.getId());
        profile.resetToDefault();
        profileRepository.save(profile);

        auditService.record(
                "USER_SHORTCUTS_RESET",
                "USER_SHORTCUT_PROFILE",
                profile.getId(),
                username,
                "Reset personal screen shortcuts to defaults",
                null
        );

        return getProfile(username);
    }

    private void validateRequest(AppUser user, List<ScreenShortcutApi.ShortcutItemRequest> shortcuts) {
        if (shortcuts.size() > MAX_SHORTCUTS) {
            throw new BusinessRuleException(
                    "Shortcut limit exceeded (max 20).",
                    "SHORTCUT_LIMIT_EXCEEDED",
                    HttpStatus.BAD_REQUEST
            );
        }

        Set<String> keys = new HashSet<>();
        Set<String> pages = new HashSet<>();

        Map<String, AccessApi.AccessPageResponse> catalogPageMap = accessCatalogService.catalog().pages().stream()
                .collect(Collectors.toMap(AccessApi.AccessPageResponse::code, p -> p, (a, b) -> a));

        List<AccessApi.AccessPageResponse> availablePages = accessCatalogService.availablePagesForUser(
                getRoleCodes(user),
                effectiveMenus(user)
        );
        Set<String> availablePageCodes = availablePages.stream()
                .map(AccessApi.AccessPageResponse::code)
                .collect(Collectors.toSet());

        for (var item : shortcuts) {
            if (!ALLOWED_KEY_CODE.matcher(item.secondKeyCode()).matches()) {
                throw new BusinessRuleException(
                        "Invalid key code: " + item.secondKeyCode(),
                        "SHORTCUT_KEY_INVALID",
                        HttpStatus.BAD_REQUEST
                );
            }
            if (!keys.add(item.secondKeyCode())) {
                throw new BusinessRuleException(
                        "Duplicate shortcut key: " + item.secondKeyCode(),
                        "SHORTCUT_KEY_DUPLICATE",
                        HttpStatus.BAD_REQUEST
                );
            }
            if (!pages.add(item.pageCode())) {
                throw new BusinessRuleException(
                        "Duplicate destination page: " + item.pageCode(),
                        "SHORTCUT_DESTINATION_DUPLICATE",
                        HttpStatus.BAD_REQUEST
                );
            }
            if (!catalogPageMap.containsKey(item.pageCode())) {
                throw new BusinessRuleException(
                        "Unknown destination page: " + item.pageCode(),
                        "SHORTCUT_PAGE_UNKNOWN",
                        HttpStatus.BAD_REQUEST
                );
            }
            if (!availablePageCodes.contains(item.pageCode())) {
                throw new BusinessRuleException(
                        "Destination page is not currently available to user: " + item.pageCode(),
                        "SHORTCUT_PAGE_NOT_AVAILABLE",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    private ScreenShortcutApi.ProfileResponse buildDefaultProfile(
            UserShortcutProfile profile,
            List<AccessApi.AccessPageResponse> availablePages,
            List<ScreenShortcutApi.DestinationResponse> destinationResponses
    ) {
        Map<String, AccessApi.AccessPageResponse> catalogPageMap = accessCatalogService.catalog().pages().stream()
                .collect(Collectors.toMap(AccessApi.AccessPageResponse::code, p -> p, (a, b) -> a));

        Set<String> availablePageCodes = availablePages.stream()
                .map(AccessApi.AccessPageResponse::code)
                .collect(Collectors.toSet());

        List<ScreenShortcutApi.ShortcutResponse> shortcutResponses = new ArrayList<>();
        List<DefaultScreenShortcutProvider.DefaultShortcut> defaultList = defaultProvider.defaults();

        for (var def : defaultList) {
            AccessApi.AccessPageResponse catalogPage = catalogPageMap.get(def.pageCode());
            if (catalogPage == null) {
                continue; // Skip unknown catalog pages
            }

            boolean isAvailable = availablePageCodes.contains(def.pageCode());
            if (!isAvailable) {
                // Default template skips non-available pages
                continue;
            }

            shortcutResponses.add(new ScreenShortcutApi.ShortcutResponse(
                    null,
                    catalogPage.code(),
                    catalogPage.menuId(),
                    catalogPage.route(),
                    catalogPage.titleKey(),
                    def.secondKeyCode(),
                    displayKey(def.secondKeyCode()),
                    true,
                    true,
                    ShortcutAvailability.AVAILABLE.name(),
                    null
            ));
        }

        long version = profile != null ? profile.getVersion() : 0L;
        java.time.Instant updatedAt = profile != null ? profile.getUpdatedAt() : null;

        return new ScreenShortcutApi.ProfileResponse(
                ShortcutProfileMode.DEFAULT.name(),
                version,
                shortcutResponses,
                destinationResponses,
                updatedAt
        );
    }

    private ScreenShortcutApi.ProfileResponse buildCustomProfile(
            UserShortcutProfile profile,
            List<AccessApi.AccessPageResponse> availablePages,
            List<ScreenShortcutApi.DestinationResponse> destinationResponses
    ) {
        List<UserScreenShortcut> savedShortcuts = shortcutRepository.findByProfileIdOrderBySortOrderAsc(profile.getId());

        Map<String, AccessApi.AccessPageResponse> catalogPageMap = accessCatalogService.catalog().pages().stream()
                .collect(Collectors.toMap(AccessApi.AccessPageResponse::code, p -> p, (a, b) -> a));

        Set<String> availablePageCodes = availablePages.stream()
                .map(AccessApi.AccessPageResponse::code)
                .collect(Collectors.toSet());

        List<ScreenShortcutApi.ShortcutResponse> shortcutResponses = new ArrayList<>();

        for (UserScreenShortcut s : savedShortcuts) {
            AccessApi.AccessPageResponse catalogPage = catalogPageMap.get(s.getPageCode());
            if (catalogPage == null) {
                shortcutResponses.add(new ScreenShortcutApi.ShortcutResponse(
                        s.getId(),
                        s.getPageCode(),
                        s.getPageCode().toLowerCase(),
                        "",
                        "shortcuts.pageRemoved",
                        s.getSecondKeyCode(),
                        displayKey(s.getSecondKeyCode()),
                        s.isEnabled(),
                        false,
                        ShortcutAvailability.PAGE_REMOVED.name(),
                        "shortcuts.pageRemoved"
                ));
                continue;
            }

            String status;
            String reasonKey = null;

            if (!s.isEnabled()) {
                status = ShortcutAvailability.DISABLED.name();
                reasonKey = "shortcuts.disabled";
            } else if (availablePageCodes.contains(s.getPageCode())) {
                status = ShortcutAvailability.AVAILABLE.name();
            } else {
                // Determine exact reason why page is unavailable
                status = ShortcutAvailability.NO_ROLE.name();
                reasonKey = "shortcuts.noRole";
            }

            shortcutResponses.add(new ScreenShortcutApi.ShortcutResponse(
                    s.getId(),
                    catalogPage.code(),
                    catalogPage.menuId(),
                    catalogPage.route(),
                    catalogPage.titleKey(),
                    s.getSecondKeyCode(),
                    displayKey(s.getSecondKeyCode()),
                    s.isEnabled(),
                    false,
                    status,
                    reasonKey
            ));
        }

        return new ScreenShortcutApi.ProfileResponse(
                ShortcutProfileMode.CUSTOM.name(),
                profile.getVersion(),
                shortcutResponses,
                destinationResponses,
                profile.getUpdatedAt()
        );
    }

    private AppUser requireCurrentUser(String username) {
        String appId = TenantContext.currentOrSystem();
        if ("SYSTEM".equals(appId)) {
            return userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new NotFoundException("User not found: " + username, "USER_NOT_FOUND"));
        }
        return userRepository.findByAppIdAndUsernameIgnoreCase(appId, username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username, "USER_NOT_FOUND"));
    }

    private Set<String> getRoleCodes(AppUser user) {
        if (user == null || user.getRoles() == null) {
            return Set.of();
        }
        return user.getRoles().stream()
                .map(r -> r.getCode().name())
                .collect(Collectors.toSet());
    }

    private Set<String> effectiveMenus(AppUser user) {
        return user.getAllowedMenus() != null ? user.getAllowedMenus() : Set.of();
    }

    private String displayKey(String secondKeyCode) {
        if (secondKeyCode.startsWith("Key")) {
            return secondKeyCode.substring(3);
        }
        if (secondKeyCode.startsWith("Digit")) {
            return secondKeyCode.substring(5);
        }
        return secondKeyCode;
    }

    private String buildChangeDetails(
            List<UserScreenShortcut> previous,
            List<ScreenShortcutApi.ShortcutItemRequest> updated
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"previousCount\":").append(previous.size())
                .append(",\"newCount\":").append(updated.size())
                .append(",\"items\":[");
        for (int i = 0; i < updated.size(); i++) {
            var item = updated.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"key\":\"").append(item.secondKeyCode())
                    .append("\",\"page\":\"").append(item.pageCode())
                    .append("\",\"enabled\":").append(item.enabled()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
