package com.bemo.hr.shared.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    public UserPreference currentOrCreate(String userId) {
        return userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> userPreferenceRepository.save(new UserPreference(userId)));
    }

    public UserPreference update(String userId, AuthApi.PreferenceRequest request) {
        var preference = currentOrCreate(userId);
        preference.update(request.theme(), request.tableDensity(), request.locale(), request.excelTableStyle(), request.defaultPageSize(), request.defaultPage());
        return preference;
    }

    public UserPreference updateNavigation(String userId, AuthApi.NavigationPreferenceRequest request) {
        var preference = currentOrCreate(userId);
        preference.updateNavigation(request.showFavorites(), request.showRecentlyUsed(), request.maxRecentlyUsed(),
                request.favoriteMenuIds(), request.recentMenuIds());
        return preference;
    }

    public UserPreference updateDashboard(String userId, AuthApi.DashboardPreferenceRequest request, boolean layoutAllowed) {
        var preference = currentOrCreate(userId);
        preference.updateDashboard(request.widgetIds(), request.animationsEnabled(), layoutAllowed);
        return preference;
    }
}
