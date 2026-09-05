# Claude Context Pack

This directory is intentionally modular.

- `rules/` contains focused context that should be loaded only for relevant work, including `menu-registration.md` (sidebar/menu wiring) and `security.md` (tenancy/authorization invariants).
- `commands/` contains reusable task workflows.
- Root `CLAUDE.md` contains only stable, high-value project guidance.

Existing repository context:
- `.claude/skills/` and `.agents/skills/` already contain TestSprite-related skills (intentionally mirrored across both directories for cross-tool support — keep both in sync, don't treat either as redundant).
- `be/skills/hr-backend/SKILL.md` and `fe/skills/hr-frontend/SKILL.md` are the authoritative, detailed backend/frontend domain skills; `.claude/rules/backend.md` and `.claude/rules/frontend.md` are short checklists that point to them rather than duplicating them.
- `AGENTS.md` is a large session/history document; it is explicitly marked historical-only at its top and must not be treated as a source of truth. Do not load it for routine tasks, and do not add any instruction telling an agent to read it in full — live rules that used to live inside it (e.g. menu registration) have been extracted into `rules/`.
- `PROJECT_MAP.md` and `docs/TECHNICAL_GUIDE_CHECKLIST.md` are better starting points for architecture and verification context.

Goal: keep the always-loaded context small and make task context selective.
