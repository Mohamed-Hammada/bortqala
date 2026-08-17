package com.bemo.hr.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    public UserPreference currentOrCreate(String userId) {
        log.debug("currentOrCreate called with userId={}", userId);
        return userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> userPreferenceRepository.save(new UserPreference(userId)));
    }

    public UserPreference update(String userId, AuthApi.PreferenceRequest request) {
        log.debug("update called with userId={}", userId);
        var preference = currentOrCreate(userId);
        preference.update(request.theme(), request.tableDensity(), request.locale(), request.excelTableStyle(), request.defaultPageSize(), request.defaultPage());
        return preference;
    }

    public UserPreference updateNavigation(String userId, AuthApi.NavigationPreferenceRequest request) {
        log.debug("updateNavigation called with userId={}", userId);
        var preference = currentOrCreate(userId);
        preference.updateNavigation(request.showFavorites(), request.showRecentlyUsed(), request.maxRecentlyUsed(),
                request.favoriteMenuIds(), request.recentMenuIds());
        return preference;
    }

    public UserPreference updateDashboard(String userId, AuthApi.DashboardPreferenceRequest request, boolean layoutAllowed) {
        log.debug("updateDashboard called with userId={}, layoutAllowed={}", userId, layoutAllowed);
        var preference = currentOrCreate(userId);
        preference.updateDashboard(request.widgetIds(), request.animationsEnabled(), layoutAllowed);
        return preference;
    }
}
