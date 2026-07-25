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
        preference.update(request.theme(), request.tableDensity(), request.locale(), request.excelTableStyle(), request.defaultPageSize());
        return preference;
    }
}
