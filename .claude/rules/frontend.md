# Frontend Rules

Use this rule set for Angular work.

See also: `fe/skills/hr-frontend/SKILL.md` is the authoritative, detailed frontend architecture spec (folder structure, Angular conventions, current shell/auth state) — this file is a short checklist, not a substitute for it. `fe/skills/no-static-labels/SKILL.md` is the authoritative i18n/no-hardcoding enforcement spec. See `.claude/rules/menu-registration.md` when the change adds a sidebar menu item.

## Architecture
- Standalone Angular components
- Prefer existing Signals/services/stores patterns over introducing new state management
- Respect existing feature boundaries and route guards

## UX / i18n
- Arabic RTL is the default experience
- Reuse existing UI primitives and icon system
- Zero-hardcoded-strings policy and translation workflow: see `fe/skills/no-static-labels/SKILL.md`

## API
Match backend DTOs and error/transition contracts. Do not silently transform server contracts in a feature-specific way without a documented reason.

## Security
See `.claude/rules/security.md`. UI permission checks improve UX but never replace backend authorization.

## Verification
For changed pages/components:
- focused unit tests
- `npm run check:i18n`
- `npm run check:hardcoded`
- `npm run build`
