# Platform — Deployment, Licensing, Backups & Health (المنصة — النشر والترخيص والنسخ)

**EN:** Platform operations for on-prem/desktop distribution. `deployment/`: offline licensing (`OfflineLicensingService`, `OfflineLicensingController` at `/api/v1/platform/licensing`) validating hashed license keys from `license-app/` against `TenantLicenseCertificate` (install / validate current / revoke lifecycle), powering the Tauri desktop bundle. Companion capabilities shipped with V326–V327: disaster-recovery backups (create/list/restore-point evidence) and system-health probes surfaced in the About screen.

**AR:** عمليات المنصة للتوزيع المحلي وسطح المكتب. حزمة `deployment/`: الترخيص دون اتصال (`OfflineLicensingService` و`OfflineLicensingController` على `/api/v1/platform/licensing`) بالتحقق من مفاتيح مجزأة صادرة من خدمة `license-app/` عبر شهادة ترخيص المستأجر (تثبيت/تحقق حالي/إلغاء)، وتغذي نسخة سطح المكتب Tauri. ومعها من V326–V327: النسخ الاحتياطية للتعافي من الكوارث وفحوصات صحة النظام المعروضة في شاشة "حول".

- Key files: `application/OfflineLicensingService.java`, `domain/TenantLicenseCertificate.java`, `api/OfflineLicensingController.java`.
- Desktop packaging: `desktop/` (Tauri) bundles web+backend+PostgreSQL; license activation is required offline end-to-end.
- Open validation: restore-drill execution evidence on target hardware (`missing-todo.md` §22 blocked list).
