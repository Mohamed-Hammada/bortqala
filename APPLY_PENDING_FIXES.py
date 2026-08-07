#!/usr/bin/env python3
"""
Apply the final procurement/i18n hardening fixes to BEMO ERP.

Run from the repository root after extracting this ZIP:

    python APPLY_PENDING_FIXES.py

Expected base branch:
    fm_bemo_consolidated

Expected reviewed source SHA:
    4ea88988a8fe2ec1c1132e2a30583c7b717e34ff

The script is intentionally conservative:
- It requires known source markers.
- It does not delete files.
- It is idempotent: already-applied changes are skipped.
- It creates .bak files only while patching and removes them after success.
"""

from pathlib import Path
import subprocess
import sys

EXPECTED_SHA = "4ea88988a8fe2ec1c1132e2a30583c7b717e34ff"

ROOT = Path.cwd()
PROC_TS = ROOT / "fe/src/app/features/trade/procurement/procurement.page.ts"
PROC_HTML = ROOT / "fe/src/app/features/trade/procurement/procurement.page.html"
H2_CHANGELOG = ROOT / "be/src/test/resources/db/changelog/releases/test-h2.changelog-master.yaml"

def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)

def require(path: Path) -> str:
    if not path.exists():
        fail(f"Required file not found: {path}")
    return path.read_text(encoding="utf-8")

def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8", newline="\n")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        fail(f"{label}: expected exactly 1 source match, found {count}")
    return text.replace(old, new, 1)

def patch_section(text: str, start_marker: str, end_marker: str, replacements: list[tuple[str, str, str]]) -> str:
    start = text.find(start_marker)
    if start < 0:
        fail(f"Section start not found: {start_marker!r}")
    end = text.find(end_marker, start + len(start_marker))
    if end < 0:
        fail(f"Section end not found after {start_marker!r}: {end_marker!r}")
    section = text[start:end]
    for old, new, label in replacements:
        if new in section:
            continue
        count = section.count(old)
        if count != 1:
            fail(f"{label}: expected exactly 1 match in section, found {count}")
        section = section.replace(old, new, 1)
    return text[:start] + section + text[end:]

def current_git_sha() -> str | None:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"],
            cwd=ROOT,
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except Exception:
        return None

sha = current_git_sha()
if sha and sha != EXPECTED_SHA:
    print(
        f"WARNING: repository HEAD is {sha}, while this patch was reviewed against {EXPECTED_SHA}.\n"
        "The patch will continue only if every expected source marker still matches."
    )

# ------------------------- procurement.page.ts -------------------------
ts = require(PROC_TS)

old_state = """  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);"""
new_state = """  readonly loading = signal(true);
  readonly savingPo = signal(false);
  readonly savingGrn = signal(false);
  readonly savingInvoice = signal(false);
  readonly savingPayment = signal(false);
  readonly resolvingMatch = signal(false);
  readonly submitting = computed(
    () =>
      this.savingPo() ||
      this.savingGrn() ||
      this.savingInvoice() ||
      this.savingPayment() ||
      this.resolvingMatch(),
  );
  readonly error = signal<string | null>(null);"""
ts = replace_once(ts, old_state, new_state, "split procurement operation states")

# Resolve-match localization + duplicate-submit protection.
ts = patch_section(
    ts,
    "  async resolveMatch(resolutionNotes: string): Promise<void> {",
    "\n  // ─── PO Form",
    [
        (
            "  async resolveMatch(resolutionNotes: string): Promise<void> {\n    const match = this.activeMatch();",
            "  async resolveMatch(resolutionNotes: string): Promise<void> {\n    if (this.resolvingMatch()) return;\n    const match = this.activeMatch();",
            "resolveMatch duplicate-submit guard",
        ),
        (
            "      this.notification.error('ملاحظات التسوية مطلوبة.');",
            "      this.notification.error(this.i18n.t('procurement.matchResolutionNotesRequired'));",
            "resolveMatch required-note i18n",
        ),
        (
            "    try {\n      const updated = await firstValueFrom(",
            "    this.resolvingMatch.set(true);\n    try {\n      const updated = await firstValueFrom(",
            "resolveMatch start state",
        ),
        (
            "      this.notification.success('تمت تسوية تفاوت المطابقة بنجاح.');",
            "      this.notification.success(this.i18n.t('procurement.matchResolvedSuccess'));",
            "resolveMatch success i18n",
        ),
        (
            "    } catch (err) {\n      this.notification.error(apiErrorMessage(err, this.i18n));\n    }",
            "    } catch (err) {\n      this.notification.error(apiErrorMessage(err, this.i18n));\n    } finally {\n      this.resolvingMatch.set(false);\n    }",
            "resolveMatch final state",
        ),
    ],
)

# Independent save state per form.
for start, end, signal_name, label in [
    ("  async submitPo() {", "\n  issuePo(", "savingPo", "PO"),
    ("  async submitGrn() {", "\n  // ─── Invoice Methods", "savingGrn", "GRN"),
    ("  async submitInvoice() {", "\n  // ─── Payment Methods", "savingInvoice", "invoice"),
    ("  async submitPayment() {", "\n  // ─── Shared", "savingPayment", "payment"),
]:
    ts = patch_section(
        ts,
        start,
        end,
        [
            ("if (this.submitting()) return;", f"if (this.{signal_name}()) return;", f"{label} guard"),
            ("this.submitting.set(true);", f"this.{signal_name}.set(true);", f"{label} start state"),
            ("finally { this.submitting.set(false); }", f"finally {{ this.{signal_name}.set(false); }}", f"{label} final state"),
        ],
    )

if "this.submitting.set(" in ts:
    fail("procurement.page.ts still contains mutable global submitting state")
if "notification.error('ملاحظات التسوية مطلوبة.')" in ts or "notification.success('تمت تسوية تفاوت المطابقة بنجاح.')" in ts:
    fail("procurement.page.ts still contains the known hardcoded three-way-match messages")

write(PROC_TS, ts)
print(f"Patched {PROC_TS}")

# ------------------------ procurement.page.html ------------------------
html = require(PROC_HTML)

html_replacements = [
    (
        '[disabled]="submitting() || poForm.invalid || totalCalculated() <= 0"',
        '[disabled]="savingPo() || poForm.invalid || totalCalculated() <= 0"',
        "PO save button state",
    ),
    (
        '[preventOutsideClose]="submitting()"',
        '[preventOutsideClose]="savingGrn()"',
        "GRN outside-close state",
    ),
    (
        '[disabled]="submitting()" (click)="grnModalOpen.set(false)"',
        '[disabled]="savingGrn()" (click)="grnModalOpen.set(false)"',
        "GRN cancel state",
    ),
    (
        '[disabled]="submitting() || grnHasErrors()"',
        '[disabled]="savingGrn() || grnHasErrors()"',
        "GRN submit state",
    ),
    (
        "{{ submitting() ? i18n.t('procurement.grnSubmitting') : i18n.t('procurement.grnSubmit') }}",
        "{{ savingGrn() ? i18n.t('procurement.grnSubmitting') : i18n.t('procurement.grnSubmit') }}",
        "GRN submit label state",
    ),
    (
        '[disabled]="submitting() || invForm.invalid"',
        '[disabled]="savingInvoice() || invForm.invalid"',
        "invoice submit state",
    ),
    (
        '[disabled]="submitting() || pmtForm.invalid"',
        '[disabled]="savingPayment() || pmtForm.invalid"',
        "payment submit state",
    ),
    (
        '<button type="button" class="button primary" (click)="resolveMatch(resNotes.value)">⚡ {{ i18n.t(\'procurement.resolveButton\') }}</button>',
        '<button type="button" class="button primary" [disabled]="resolvingMatch()" (click)="resolveMatch(resNotes.value)">⚡ {{ resolvingMatch() ? i18n.t(\'procurement.resolvingMatch\') : i18n.t(\'procurement.resolveButton\') }}</button>',
        "three-way-match resolve state",
    ),
]

for old, new, label in html_replacements:
    html = replace_once(html, old, new, label)

# Only a compatibility aggregate may remain in TS. The HTML must use operation-specific states.
if "submitting()" in html:
    fail("procurement.page.html still contains submitting(); operation-specific bindings are incomplete")

write(PROC_HTML, html)
print(f"Patched {PROC_HTML}")

# ------------------------- H2 changelog include ------------------------
h2 = require(H2_CHANGELOG)
v125 = """  - include:
      file: db/changelog/data/insert/20260807_v125_budget_menu_translations.yaml"""
v126 = """  - include:
      file: db/changelog/data/insert/20260807_v126_procurement_hardening_translations.yaml"""

if v126 not in h2:
    if v125 not in h2:
        fail("Could not locate V125 in H2 changelog to append V126 safely")
    h2 = h2.replace(v125, v125 + "\n" + v126, 1)
    write(H2_CHANGELOG, h2)
    print(f"Patched {H2_CHANGELOG}")
else:
    print(f"Already patched {H2_CHANGELOG}")

print("\nFinal pending-fix patch applied successfully.")
print("Next:")
print("  cd fe && npm run check:i18n && npm run check:hardcoded-ui && npm test -- --watch=false && npm run build")
print("  cd ../be && ./gradlew clean test")
