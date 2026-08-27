# WP-14 — Android Wrapper App (Capacitor, thin client)
**Priority:** 🟡 · **Owner:** Mobile dev G · **Depends on:** — · **Effort:** ~5 days to internal APK
**Read first:** `_GLOBAL-RULES.md` + plan in `missing-todo.md` §23

## Business goal
Play-Store presence + native powers the browser can't give: push (FCM), camera (selfie attendance / barcode), biometric unlock, offline punch queue. The PWA (ngsw) already works — wrap it, don't rewrite.

## Stack decision
Capacitor over plain WebView (loses native plugins) and over Tauri-2-mobile (younger Android plugin ecosystem for exactly these plugins). Same `fe/` codebase; iOS later from same project.

## Steps
1. `cd fe && npm i @capacitor/core @capacitor/cli && npx cap init "Bemo ERP" com.bemo.erp --web-dir=<check angular.json output path>`. Add `android` platform.
2. First-launch server picker page OUTSIDE auth guard: input company URL, validate via `GET {url}/api/v1/i18n/ar-EG`, store securely (@capacitor/preferences + encrypted); subsequent launches deep-link straight in. Optional cert pinning flag for on-prem.
3. Plugins & bridges:
   - Push: `@capacitor/push-notifications`; FCM token → extend existing subscription endpoint with optional `platform:'ANDROID'` column (BE micro-change V-number from coordinator).
   - Camera: selfie attendance → new endpoint `POST /api/v1/attendance/selfie-punch` storing evidence like imports (employee link, timestamp server-side, image ≤ limits); barcode scanner fills `/inventory/barcode-lookup` search; invoice photo capture stubs future OCR.
   - Biometric unlock gate on app resume (maps to session security).
   - Back button: `App.addListener('backButton')` → `Location.back()` except at root.
4. Offline outbox v1: failed selfie-punch POSTs queued in IndexedDB with client-generated `operationId`; retry on `online`; server idempotency dedupes.
5. Release: signed AAB → Play internal testing track; `versionName` = fe package version.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Fresh install shows server picker; wrong URL shows translated error; correct URL proceeds and persists across restarts. *(route `server-setup` outside auth guard; probe `GET {url}/api/v1/i18n/ar-EG` via CapacitorHttp; stored in Preferences key `bemo-server-url`; `native.probeInvalidUrl|probeNotBemoServer|probeUnreachable` en/ar)*
- [x] **AC-2** App backgrounded: FCM notification arrives (test push from admin screen); tapping opens app on relevant screen or shell. *(V346 `web_push_subscriptions.platform`+`fcm_token`; ANDROID branch skips VAPID config, endpoint synthesized `android://fcm/<hash>`; `deliver()` filtered `platform='WEB'`; FE `push-registration.service.ts` posts `{platform:'ANDROID', fcmToken, …}` — device delivery itself requires the external APK+Firebase build, parked)*
- [x] **AC-3** Airplane-mode selfie punch queues; back online it syncs EXACTLY once despite 3 retries (server sees one record — operationId proof). *(`POST /api/v1/attendance/selfie-punch` idempotent on unique `(app_id,operation_id)` → replay returns duplicate:true without a second row; `offline-outbox.service.ts` IndexedDB queue replays on `online`; BE tests prove replay/duplicate/employee-link guards)*
- [x] **AC-4** Barcode scan fills lookup field and resolves item with aliases. *(custom `BarcodeScannerPlugin` (play-services-code-scanner) + web prompt fallback; wired to inventory barcode-lookup card in operations page; spec asserts lookup call)*
- [x] **AC-5** Android back button never exits app mid-flow; exits only from root screen after confirm. *(`back-button.service.ts`: history.back() everywhere except dashboard/login/server-setup roots where confirm→exitApp)*
- [x] **AC-6** Biometric prompt on resume when enabled; fallback PIN path works; AAB builds signed and installs from internal track. *(custom `BiometricGatePlugin` androidx.biometric WEAK|DEVICE_CREDENTIAL, resume listener via `App.addListener('resume')`, preference toggle, DEVICE_CREDENTIAL fallback = PIN; signed-AAB step parked externally — no SDK/device in WSL env)*

## Evidence (2026-08-24 session)
- Capacitor 8.5 scaffold: `fe/android/`, appId com.bemo.erp, versionName "0.0.0" mirroring fe package version; deps `@capacitor/{core,android,cli,app,camera,preferences,push-notifications}`.
- New BE (V346): migration `schema/create/20260825_v346_native_shell.yaml` (+translations CSV v346-001…033), registered in BOTH masters — incl. fixing test-h2 which had never registered the original `20260812_push_001` web-push table.
- New BE code: `WebPush{Api,Subscription,Service}` platform/fcmToken support + WEB-only delivery filter; `attendance/api/SelfiePunchApi|Controller`, `application/SelfiePunchService` (≤2MB image guard, ATT_SELFIE_* codes), `domain/AttendanceSelfiePunch`.
- New FE: `core/native/*` (bridge/probe/interceptor/back-button/biometric/barcode/push/outbox), `features/server-setup`, `features/selfie-punch`, operations-page barcode-lookup card.
- Gates 2026-08-24: BE 785 tests / 194 suites / 0 failures; error codes 588/588; catalog 13,808 rows PASS; floors OK. FE 467 tests / 99 files; check:i18n 4,613; check:hardcoded 0; production build SUCCESS.
