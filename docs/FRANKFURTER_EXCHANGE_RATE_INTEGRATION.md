# Frankfurter exchange-rate hint integration

## Purpose

This integration adds online currency rates from Frankfurter as **informational reference hints**.
The ERP's configured `exchangeRate` remains the accounting/business value and is never overwritten
by the external service.

## Provider

- Provider: Frankfurter v2
- Public base URL: `https://api.frankfurter.dev`
- No API key is required.
- The implementation queries `/v2/currencies` first, then requests only the active ERP currencies
  that Frankfurter currently supports through `/v2/rates`.
- The base currency is discovered from the ERP's active currency records.

## Rate direction

Frankfurter returns `1 BASE = X QUOTE`. The existing ERP screen represents the configured value as
`1 QUOTE = X BASE`, so the integration stores the reciprocal as the reference hint.

Example:

- Frankfurter: `1 EGP = 0.01954 USD`
- ERP online hint: `1 USD = 51.177... EGP`

## Refresh policy

- Tenant-scoped.
- Enabled by default.
- Default interval: **4 hours**.
- Admins can configure 1–168 hours.
- A scheduler scans every 5 minutes by default and refreshes only tenants that are due.
- Admins and Finance Managers can trigger a manual refresh.
- Override scheduler scan cadence with:
  `hr.exchange-rate.scheduler-scan-ms`
- Override the provider base URL (e.g. test/stub environment) with:
  `hr.exchange-rate.frankfurter-base-url`

## Failure behavior

Provider failures are non-destructive:

- configured accounting rates remain untouched;
- the last successful online hint remains stored;
- the integration records a status/error code;
- the UI shows the failure as a warning.

## API

- `GET /api/v1/finance/exchange-rate-hints/settings`
- `PUT /api/v1/finance/exchange-rate-hints/settings` — Admin/Super Admin
- `POST /api/v1/finance/exchange-rate-hints/refresh` — Admin/Super Admin/Finance Manager
- `GET /api/v1/finance/currencies` now returns the reference-rate metadata.

## UI

Open:

`المالية → الضرائب والعملات → العملات`

The page shows:

- configured system exchange rate;
- Frankfurter online reference rate;
- provider date;
- percentage difference between the configured value and reference;
- last/next synchronization status;
- Admin refresh interval configuration;
- manual refresh action.

## Database

Migration `v148` adds:

- `exchange_rate_hint_settings`
- reference-rate metadata columns on `currencies`

Migration `v149` adds Arabic and English UI translations.

## Safety boundary

The reference rate is intentionally not used by posting, invoicing, payroll, inventory valuation,
journal entries, or other financial calculations unless a future business workflow explicitly asks
the user to adopt/copy a reference value.
