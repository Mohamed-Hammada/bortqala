---
name: no-static-labels
description: Enforces a zero-static-labels policy in UI components. Use whenever adding or changing user-facing labels, headings, buttons, tooltips, dialogs, toasts, placeholders, badges, status messages, or backend error copy; it requires bilingual, duplicate-safe database translations and repository validation.
---

# Zero Static Labels & Mandatory Localization Skill

When developing UI components or modifying features in the frontend, **never hardcode inline text or static labels** (in Arabic, English, or any language) inside HTML templates, TypeScript component metadata, tooltips, toasts, or error messages.

## 1. Core Principles

1. **Every Visible String Must Be Localized**:
   - Headers, button labels, dropdown options, form placeholders, tooltips, alert banners, toast notifications, confirmation dialogs, empty state prompts, table column titles, and error messages MUST use `i18n.t('key')` or `I18nService`.

2. **No Ad-Hoc Inline Copy**:
   - Incorrect: `<button>➕ إدخال حضور جديد</button>`
   - Incorrect: `this.notification.success('تم الحفظ بنجاح');`
   - Correct: `<button>{{ i18n.t('manualAttendance.newEntry') }}</button>`
   - Correct: `this.notification.success(this.i18n.t('manualAttendance.saveSuccess'));`

3. **Bilingual Key Registration Protocol**:
   Whenever a new key is introduced or refactored:
   - **Step 1**: Search the complete catalog before adding anything: `python be/tools/check-translation-catalog.py`. Reuse an existing semantic key; never duplicate a key/locale pair in a later CSV.
   - **Step 2**: For exceptional copy required before the first HTTP translation bundle, register the key in `REQUIRED_COPY['ar-EG']` and `REQUIRED_COPY['en-US']` in `fe/src/app/core/i18n.service.ts`. Ordinary feature copy belongs only in the database catalog.
   - **Step 3**: Add both locales with `python be/tools/add-translation.py --file <new-file>.csv --key <key> --ar <arabic> --en <english>`. New seed CSVs omit the technical `id`; Liquibase v228 lets the database generate UUIDs.
   - **Step 4**: Register the CSV in a new versioned `loadData` changeset and in both production and H2 release masters. Roll back by the exact translation key plus `app_id IS NULL`, not by an id prefix.
   - **Step 5**: Run `python be/tools/check-translation-catalog.py`, `npm run check:i18n`, and `npm run check:hardcoded`. The Python gate checks the entire historical catalog for malformed rows, duplicates, and missing Arabic/English pairs.

## 2. Parameterized & Dynamic Translations

For dynamic counts or variables, pass parameters into `i18n.t()`:

```ts
// Service fallback definition:
// 'manualAttendance.saveCount': 'إدخال وحفظ الحضور ({count})' / 'Save Attendance ({count})'

// Component usage:
this.i18n.t('manualAttendance.saveCount', { count: this.dirtyCellKeys().size });
```

## 3. Verification Checklist

Before completing any task:
1. Search the modified `.ts` and `.html` files for inline Arabic/English strings in user-visible attributes or templates.
2. Run `npm run check:i18n` to ensure all key usages are validated.
3. Run `npm run build` to ensure clean compilation.
