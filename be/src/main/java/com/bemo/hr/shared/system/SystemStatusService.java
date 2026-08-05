package com.bemo.hr.shared.system;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.DemoNoLoginService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
class SystemStatusService {
    private final SystemSettingRepository systemSettingRepository;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final ObjectProvider<DemoNoLoginService> demoNoLoginServiceProvider;
    private final String serviceName;

    SystemStatusService(
            SystemSettingRepository systemSettingRepository,
            ObjectProvider<BuildProperties> buildPropertiesProvider,
            ObjectProvider<DemoNoLoginService> demoNoLoginServiceProvider,
            @Value("${spring.application.name:bemo-erp}") String serviceName) {
        this.systemSettingRepository = systemSettingRepository;
        this.buildPropertiesProvider = buildPropertiesProvider;
        this.demoNoLoginServiceProvider = demoNoLoginServiceProvider;
        this.serviceName = serviceName;
    }

    @Transactional(readOnly = true)
    SystemStatusApi.StatusResponse status() {
        return response(requireCacheSetting());
    }

    @Transactional
    SystemStatusApi.StatusResponse rotateCacheVersion(String actor, String reason) {
        SystemSetting setting = requireCacheSetting();
        setting.rotate(actor, reason);
        return response(systemSettingRepository.save(setting));
    }

    private SystemSetting requireCacheSetting() {
        return systemSettingRepository.findById(SystemSetting.CLIENT_CACHE_VERSION)
                .orElseThrow(() -> new BusinessRuleException(
                        "إعداد إصدار ذاكرة التخزين المؤقت غير موجود. راجع ترحيلات قاعدة البيانات.",
                        "SYS_CACHE_VERSION_SETTING_MISSING", HttpStatus.CONFLICT));
    }

    private SystemStatusApi.StatusResponse response(SystemSetting setting) {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        String version = buildProperties == null ? "development" : buildProperties.getVersion();
        Instant updatedAt = setting.getUpdatedAt();
        DemoNoLoginService demoNoLoginService = demoNoLoginServiceProvider.getIfAvailable();
        return new SystemStatusApi.StatusResponse(
                "UP",
                serviceName,
                version,
                setting.getValue(),
                Instant.now().toEpochMilli(),
                updatedAt == null ? null : updatedAt.toEpochMilli(),
                setting.getUpdatedBy(),
                demoNoLoginService != null && demoNoLoginService.isAvailable());
    }
}
