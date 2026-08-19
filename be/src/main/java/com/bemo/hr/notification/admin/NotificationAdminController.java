package com.bemo.hr.notification.admin;

import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/admin")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN_ONLY)
public class NotificationAdminController {
    private final NotificationAdminService service;

    @GetMapping("/apps")
    public List<NotificationAdminApi.AppSummary> apps(Authentication auth) {
        return service.apps(auth);
    }

    @GetMapping("/users")
    public List<NotificationAdminApi.UserSummary> users(@RequestParam String appId, @RequestParam(defaultValue = "") String q, Authentication auth) {
        return service.users(appId, q, auth);
    }

    @PostMapping(value = "/excel/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NotificationAdminApi.ExcelPreview previewExcel(@RequestParam String appId, @RequestPart("file") MultipartFile file, Authentication auth) {
        return service.previewExcel(appId, file, auth);
    }

    @PostMapping("/send")
    public NotificationAdminApi.BulkSendResult send(@Valid @RequestBody NotificationAdminApi.BulkSendPayload payload, Authentication auth) {
        return service.send(payload, auth);
    }
}
