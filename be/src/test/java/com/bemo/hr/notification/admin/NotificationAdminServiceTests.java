package com.bemo.hr.notification.admin;

import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationAdminServiceTests {
    @Mock private AppUserRepository appUserRepository;
    @Mock private TenantApplicationRepository tenantApplicationRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private Authentication authentication;
    @InjectMocks private NotificationAdminService notificationAdminService;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void previewsTheDownloadedCsvRecipientTemplateFormat() {
        TenantApplication app = new TenantApplication("CSV-IMPORT", "CSV import test");
        AppUser user = new AppUser(app.getId(), "jdoe", "Jane Doe", "hash", Set.of(), Set.of(), true, true);
        TenantContext.set(app.getId());
        when(authentication.getAuthorities()).thenReturn(List.of());
        when(tenantApplicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
        when(appUserRepository.findAllByAppIdOrderByDisplayNameAsc(app.getId())).thenReturn(List.of(user));
        var file = new MockMultipartFile("file", "notification-recipients-template.csv", "text/csv",
                "\uFEFF\"username\"\n\"jdoe\"\n".getBytes(StandardCharsets.UTF_8));

        var preview = notificationAdminService.previewExcel(app.getId(), file, authentication);

        assertThat(preview.validCount()).isEqualTo(1);
        assertThat(preview.validUsernames()).containsExactly("jdoe");
        assertThat(preview.notFoundCount()).isZero();
    }
}
