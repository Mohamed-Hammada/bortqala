package com.bemo.hr.notification.admin;

import com.bemo.hr.notification.push.NotificationCreatedEvent;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAdminService {
    private static final int MAX_RECIPIENTS = 5_000;
    private static final long MAX_EXCEL_BYTES = 5L * 1024 * 1024;
    private static final int PREVIEW_ERROR_LIMIT = 100;

    private final AppUserRepository userRepository;
    private final TenantApplicationRepository appRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (current == ',' && !quoted) {
                values.add(value.toString().strip());
                value.setLength(0);
            } else value.append(current);
        }
        values.add(value.toString().strip());
        return values;
    }

    private static String normalize(String v) {
        return v == null ? "" : v.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeHeader(String v) {
        return normalize(v).replace('-', '_');
    }

    private static List<String> limit(List<String> values) {
        return values.stream().limit(PREVIEW_ERROR_LIMIT).toList();
    }

    private static boolean isSuperAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private static BusinessRuleException badRequest(String message, String code) {
        return new BusinessRuleException(message, code, HttpStatus.BAD_REQUEST);
    }

    @Transactional(readOnly = true)
    public List<NotificationAdminApi.AppSummary> apps(Authentication auth) {
        log.debug("apps called");
        String currentApp = TenantContext.require();
        if (!isSuperAdmin(auth)) {
            return appRepository.findById(currentApp).filter(TenantApplication::isActive)
                    .map(a -> List.of(new NotificationAdminApi.AppSummary(a.getId(), a.getCode(), a.getName())))
                    .orElse(List.of());
        }
        return appRepository.findAll().stream().filter(TenantApplication::isActive)
                .sorted(Comparator.comparing(TenantApplication::getName, String.CASE_INSENSITIVE_ORDER))
                .map(a -> new NotificationAdminApi.AppSummary(a.getId(), a.getCode(), a.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationAdminApi.UserSummary> users(String appId, String query, Authentication auth) {
        log.debug("users called with appId={}, query={}", appId, query);
        String target = authorizeTargetApp(appId, auth);
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return userRepository.findAllByAppIdOrderByDisplayNameAsc(target).stream()
                .filter(u -> q.isBlank() || u.getUsername().contains(q) || u.getDisplayName().toLowerCase(Locale.ROOT).contains(q))
                .limit(200)
                .map(u -> new NotificationAdminApi.UserSummary(u.getUsername(), u.getDisplayName(), u.isActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationAdminApi.ExcelPreview previewExcel(String appId, MultipartFile file, Authentication auth) {
        log.debug("previewExcel called with appId={}, fileName={}", appId, file != null ? file.getOriginalFilename() : null);
        String target = authorizeTargetApp(appId, auth);
        if (file == null || file.isEmpty()) throw badRequest("Excel file is required", "NOTIFICATION_EXCEL_REQUIRED");
        if (file.getSize() > MAX_EXCEL_BYTES)
            throw badRequest("Excel file must be 5 MB or smaller", "NOTIFICATION_EXCEL_TOO_LARGE");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".csv"))) {
            throw badRequest("Only .xlsx, .xls, and .csv files are supported", "NOTIFICATION_EXCEL_TYPE");
        }

        List<String> rows = name.endsWith(".csv") ? readCsvUsernames(file) : readWorkbookUsernames(file);
        if (rows.size() > MAX_RECIPIENTS)
            throw badRequest("Excel recipient limit is 5000", "NOTIFICATION_RECIPIENT_LIMIT");
        Map<String, AppUser> users = userRepository.findAllByAppIdOrderByDisplayNameAsc(target).stream()
                .collect(Collectors.toMap(u -> normalize(u.getUsername()), u -> u, (a, b) -> a, LinkedHashMap::new));

        Set<String> seen = new LinkedHashSet<>();
        List<String> valid = new ArrayList<>(), duplicates = new ArrayList<>(), missing = new ArrayList<>(), inactive = new ArrayList<>();
        for (String raw : rows) {
            String username = normalize(raw);
            if (username.isBlank()) continue;
            if (!seen.add(username)) {
                duplicates.add(username);
                continue;
            }
            AppUser user = users.get(username);
            if (user == null) missing.add(username);
            else if (!user.isActive()) inactive.add(username);
            else valid.add(user.getUsername());
        }
        return new NotificationAdminApi.ExcelPreview(rows.size(), valid.size(), duplicates.size(), missing.size(), inactive.size(),
                valid, limit(duplicates), limit(missing), limit(inactive));
    }

    @Transactional
    public NotificationAdminApi.BulkSendResult send(NotificationAdminApi.BulkSendPayload payload, Authentication auth) {
        log.debug("send called with targetAppId={}, mode={}, notificationType={}", payload.targetAppId(), payload.mode(), payload.notificationType());
        String target = authorizeTargetApp(payload.targetAppId(), auth);
        List<AppUser> allUsers = userRepository.findAllByAppIdOrderByDisplayNameAsc(target);
        Map<String, AppUser> byUsername = allUsers.stream().collect(Collectors.toMap(
                u -> normalize(u.getUsername()), u -> u, (a, b) -> a, LinkedHashMap::new));

        LinkedHashSet<String> requestedNames = new LinkedHashSet<>();
        if ("APP".equals(payload.mode())) {
            allUsers.stream().filter(AppUser::isActive).map(AppUser::getUsername).forEach(requestedNames::add);
        } else {
            if (payload.usernames() != null) payload.usernames().stream().map(NotificationAdminService::normalize)
                    .filter(v -> !v.isBlank()).forEach(requestedNames::add);
        }
        if (requestedNames.size() > MAX_RECIPIENTS)
            throw badRequest("Recipient limit is 5000", "NOTIFICATION_RECIPIENT_LIMIT");
        if (requestedNames.isEmpty())
            throw badRequest("At least one recipient is required", "NOTIFICATION_NO_RECIPIENTS");

        List<AppUser> valid = new ArrayList<>();
        int missing = 0, inactive = 0;
        for (String username : requestedNames) {
            AppUser user = byUsername.get(username);
            if (user == null) missing++;
            else if (!user.isActive()) inactive++;
            else valid.add(user);
        }
        if (valid.isEmpty()) throw badRequest("No active valid recipients", "NOTIFICATION_NO_VALID_RECIPIENTS");

        String bulkId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        List<NotificationCreatedEvent> events = new ArrayList<>(valid.size());
        List<Object[]> rows = new ArrayList<>(valid.size());
        for (AppUser user : valid) {
            String id = UUID.randomUUID().toString();
            rows.add(new Object[]{id, target, user.getUsername(), payload.titleAr(), payload.titleEn(), payload.messageAr(), payload.messageEn(),
                    payload.notificationType(), payload.priority(), payload.actionLink(), Timestamp.from(now)});
            events.add(new NotificationCreatedEvent(target, id, user.getUsername(), payload.titleAr(), payload.titleEn(), payload.messageAr(),
                    payload.messageEn(), payload.notificationType(), payload.priority(), payload.actionLink()));
        }
        jdbcTemplate.batchUpdate("""
                insert into business_notifications
                  (id,app_id,recipient_username,title_ar,title_en,message_ar,message_en,notification_type,priority,action_link,is_read,created_at)
                values (?,?,?,?,?,?,?,?,?,?,false,?)
                """, rows);
        log.info("Bulk notifications sent successfully: bulkId={}, recipients={}", bulkId, valid.size());
        events.forEach(eventPublisher::publishEvent);
        return new NotificationAdminApi.BulkSendResult(bulkId, requestedNames.size(), valid.size(), missing, inactive);
    }

    private String authorizeTargetApp(String appId, Authentication auth) {
        String current = TenantContext.require();
        String target = appId == null || appId.isBlank() ? current : appId.strip();
        if (!isSuperAdmin(auth) && !current.equals(target)) throw new BusinessRuleException(
                "You cannot send notifications to another application", "NOTIFICATION_APP_FORBIDDEN", HttpStatus.FORBIDDEN);
        TenantApplication app = appRepository.findById(target).filter(TenantApplication::isActive)
                .orElseThrow(() -> badRequest("Target application was not found or is inactive", "NOTIFICATION_APP_INVALID"));
        return app.getId();
    }

    private List<String> readWorkbookUsernames(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) throw badRequest("Excel workbook has no sheets", "NOTIFICATION_EXCEL_EMPTY");
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw badRequest("Excel header is missing", "NOTIFICATION_EXCEL_HEADER");
            int usernameColumn = -1;
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String h = normalizeHeader(formatter.formatCellValue(header.getCell(c)));
                if (Set.of("username", "user", "login", "اسم المستخدم", "اسم_المستخدم").contains(h)) {
                    usernameColumn = c;
                    break;
                }
            }
            if (usernameColumn < 0)
                throw badRequest("Excel must contain a username column", "NOTIFICATION_EXCEL_HEADER");
            List<String> out = new ArrayList<>();
            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String value = formatter.formatCellValue(row.getCell(usernameColumn)).strip();
                if (!value.isBlank()) out.add(value);
                if (out.size() > MAX_RECIPIENTS) break;
            }
            return out;
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw badRequest("Unable to read Excel file", "NOTIFICATION_EXCEL_INVALID");
        }
    }

    private List<String> readCsvUsernames(MultipartFile file) {
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw badRequest("CSV header is missing", "NOTIFICATION_EXCEL_HEADER");
            List<String> header = parseCsvLine(headerLine.replace("\uFEFF", ""));
            int usernameColumn = -1;
            for (int index = 0; index < header.size(); index++) {
                String value = normalizeHeader(header.get(index));
                if (Set.of("username", "user", "login", "اسم المستخدم", "اسم_المستخدم").contains(value)) {
                    usernameColumn = index;
                    break;
                }
            }
            if (usernameColumn < 0) throw badRequest("CSV must contain a username column", "NOTIFICATION_EXCEL_HEADER");
            List<String> usernames = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> values = parseCsvLine(line);
                if (usernameColumn < values.size() && !values.get(usernameColumn).isBlank())
                    usernames.add(values.get(usernameColumn).strip());
                if (usernames.size() > MAX_RECIPIENTS) break;
            }
            return usernames;
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw badRequest("Unable to read CSV file", "NOTIFICATION_EXCEL_INVALID");
        }
    }
}
