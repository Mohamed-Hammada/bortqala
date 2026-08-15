-- BEMO ERP - one-time enable-all entitlement bootstrap.
-- Safe to run repeatedly: a durable marker in system_settings means the
-- entitlement rows are changed only on the first successful execution.
--
-- This enables the 19 implemented EntitlementCatalog features for every
-- existing tenant/app. It does NOT bypass future subscription-plan changes;
-- applying STARTER/GROWTH later can still intentionally change entitlements.

DO $bemo$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM system_settings
        WHERE setting_key = 'bootstrap.enable_all_features.applied.v1'
    ) THEN
        WITH feature_keys(feature_key) AS (
            VALUES
                ('employeeAttendance.enabled'),
                ('biometric.fileImport.enabled'),
                ('biometric.liveSync.enabled'),
                ('workforce.enabled'),
                ('workforce.attendance.enabled'),
                ('workforce.dashboard.enabled'),
                ('workforce.contractorAccounts.enabled'),
                ('payroll.enabled'),
                ('procurement.enabled'),
                ('purchasing.enabled'),
                ('inventory.advanced.enabled'),
                ('sales.enabled'),
                ('manufacturing.enabled'),
                ('quality.enabled'),
                ('finance.enabled'),
                ('exports.enabled'),
                ('notifications.enabled'),
                ('navigation.favorites.enabled'),
                ('navigation.recents.enabled')
        )
        INSERT INTO tenant_features (
            app_id,
            feature_key,
            enabled,
            config_json,
            version,
            updated_by,
            updated_at,
            change_reason
        )
        SELECT
            a.id,
            f.feature_key,
            TRUE,
            NULL,
            0,
            'enable-all-bootstrap',
            CURRENT_TIMESTAMP,
            'One-time enable-all feature bootstrap'
        FROM apps a
        CROSS JOIN feature_keys f
        ON CONFLICT (app_id, feature_key)
        DO UPDATE SET
            enabled = TRUE,
            version = tenant_features.version + 1,
            updated_by = 'enable-all-bootstrap',
            updated_at = CURRENT_TIMESTAMP,
            change_reason = 'One-time enable-all feature bootstrap';

        INSERT INTO system_settings (
            setting_key,
            setting_value,
            created_at,
            updated_at,
            updated_by,
            change_reason
        )
        VALUES (
            'bootstrap.enable_all_features.applied.v1',
            'true',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP,
            'enable-all-bootstrap',
            'Marks completion of the one-time all-feature entitlement bootstrap'
        )
        ON CONFLICT (setting_key) DO NOTHING;
    END IF;
END
$bemo$;
