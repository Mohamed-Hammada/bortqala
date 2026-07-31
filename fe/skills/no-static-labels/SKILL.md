---
name: no-static-labels
description: Enforces a zero-static-labels policy in UI components. Ensures all user-facing text (labels, headings, buttons, tooltips, dialogs, toasts, placeholders, badges, status messages) are localized using I18nService keys (i18n.t) and registered in fallback dictionaries and translation bundles instead of hardcoded inline text.
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
   - **Step 1**: Register the key in `DEFAULT_FALLBACKS['ar-EG']` AND `DEFAULT_FALLBACKS['en-US']` inside `src/app/core/i18n.service.ts`.
   - **Step 2**: Add translation rows to the backend database Liquibase CSV file (`be/src/main/resources/db/changelog/csv/i18n_translations.csv`) for both `ar-EG` and `en-US`.
   - **Step 3**: Run `npm run check:i18n` to verify that 100% of literal keys used in `.ts` and `.html` files exist in both locales in the database changelog.

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
