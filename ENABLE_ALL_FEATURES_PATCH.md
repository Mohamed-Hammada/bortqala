# Enable All Implemented Features Patch

Target branch: `fm_bemo_consolidated`  
Prepared against the branch state reviewed on 2026-08-15.

## What this patch changes

- Makes all 19 `EntitlementCatalog` features default to enabled.
- Specifically changes `biometric.liveSync.enabled` and `notifications.enabled` from default-off to default-on.
- Adds a one-time, database-marked entitlement backfill for existing tenants.
- Wires Web Push variables into development and production Docker Compose.
- Enables Web Push in development with a new development-only VAPID pair.
- Makes production require explicit VAPID public/private keys and subject.
- Updates Windows/GraalVM launch scripts.
- Updates Docker one-click deploy scripts so the entitlement backfill runs once after Liquibase is ready.
- Keeps production demo/no-login disabled.

## Existing tenants

`enable-all-features-docker.*` runs `scripts/enable-all-features.sql`. The SQL writes this durable marker:

`bootstrap.enable_all_features.applied.v1`

That means later deployments do **not** re-enable a feature that an administrator intentionally disables after the initial bootstrap.

To deliberately re-run the enable-all operation later, remove that marker first:

```sql
DELETE FROM system_settings
WHERE setting_key = 'bootstrap.enable_all_features.applied.v1';
```

Then run the helper again.

## Subscription behavior

The existing `ENTERPRISE` subscription plan already contains all 19 catalog features, so the patch does not modify subscription-plan seed data. Applying `STARTER` or `GROWTH` later can intentionally disable features according to those plans.

## Web Push production requirement

Generate a unique production VAPID pair and set:

- `HR_WEB_PUSH_ENABLED=true`
- `HR_WEB_PUSH_PUBLIC_KEY=...`
- `HR_WEB_PUSH_PRIVATE_KEY=...`
- `HR_WEB_PUSH_SUBJECT=mailto:...`
- `HR_WEB_PUSH_TTL_SECONDS=86400`

Do not use the development key pair in production.

## Important scope note

This enables features that are already implemented and/or entitlement-controlled. It does not invent missing external infrastructure. For example, a live biometric device still needs a configured device/device-hub, and a separate email-delivery implementation/provider is still required if email notification delivery is not implemented in the codebase.
