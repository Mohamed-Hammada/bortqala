# WP-09 — First-Login "Enable Notifications on this Device?" Prompt
**Priority:** 🟠 · **Owner:** Frontend dev E · **Depends on:** infra already DONE · **Effort:** 1–2 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §16

## Business goal
Nobody discovers push notifications today. On first login ask: "تفعيل الإشعارات على هذا الجهاز؟ / Enable notifications on this device?" — per user × browser, non-blocking.

## Current state (infra complete — do NOT rebuild)
`fe/src/app/core/notification-center/web-push.service.ts`: VAPID config fetch, `enable(preferences)` triggers browser permission via SwPush, per-device subscription POST `/api/v1/notifications/push/subscriptions`, test send, detach-on-logout, detach-all-on-revoke. Settings page has manual toggles.

## Implementation steps
1. Component `core/shell/push-permission-prompt/` reusing `.shortcut-overlay/.shortcut-dialog` classes; register it with the DialogState service from WP-13 if that landed (else document follow-up).
2. Show when ALL true: `supported()` && `configured()` && `!subscribed()` && `Notification.permission==='default'` && stored answer not blocking && shell initialized (fire ~2s post-login effect).
3. Device memory: `localStorage['bemo_push_prompt_v1'] = {userId, answer:'enabled'|'later'|'never', askedAt:epochMillis}`; 'later' snoozes 14 days; 'never' permanent for this user×browser.
4. Actions: Enable → `webPush.enable(storedPreferences)` + success toast linking Settings; Not now → snooze; small "Never on this device" link.
5. permission==='denied' → never prompt; show inline hint in Settings instead.
6. Keys: `auth.pushPromptTitle/Hint/Enable/NotNow/NeverAsk/DeniedHint` + Liquibase CSV rows.
7. Tests: shows once then respects answers; enable calls service (spy); denied path renders Settings hint; prompt absent when unsupported/config-disabled.

## Acceptance Criteria (QA sign-off)
- [ ] **AC-1** Fresh browser+user: prompt appears ~2s after login, does not block navigation (can click behind/cancel anytime).
- [ ] **AC-2** Enable → native permission dialog → grant → subscribed=true and backend receives subscription with locale+prefs (verify network tab); toast confirms.
- [ ] **AC-3** "Not now" hides for exactly 14 days (test with clock override); "Never" hides permanently for that user in that browser only — another user on same browser still gets asked.
- [ ] **AC-4** Browser with permission=denied never sees the dialog; Settings shows translated denied hint with link to system settings.
- [ ] **AC-5** With push disabled server-side (`config.enabled=false`) or unsupported browser: zero prompts, zero console errors.
- [ ] **AC-6** Logout clears no stored answer but next user login evaluates independently; all copy DB-translated; check:i18n/check:hardcoded green.
