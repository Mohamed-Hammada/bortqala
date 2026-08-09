#!/usr/bin/env python3
"""Apply the small edits required in existing Bortqala files.

Run from the root of the fm_bemo_consolidated checkout AFTER extracting this ZIP.
New files are already placed by extraction; this script only patches existing files that
should not be blindly replaced: Angular routes, Imports page header, and Liquibase next release.
"""
from __future__ import annotations
from pathlib import Path
import sys

ROOT = Path.cwd()


def fail(message: str) -> None:
    raise SystemExit(f"[device-integrations] {message}")


def patch_once(path: Path, marker: str, needle: str, replacement: str) -> bool:
    if not path.exists():
        fail(f"Required file not found: {path}. Run this script from the Bortqala repository root.")
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"[skip] {path}: already patched")
        return False
    if needle not in text:
        fail(f"Could not find expected patch anchor in {path}. The branch may have moved; no partial edit was made for this file.")
    path.write_text(text.replace(needle, replacement, 1), encoding="utf-8")
    print(f"[ok]   {path}")
    return True


def main() -> None:
    if not (ROOT / "AGENTS.md").exists() or not (ROOT / "be").exists() or not (ROOT / "fe").exists():
        fail("This does not look like the Bortqala repository root.")

    routes = ROOT / "fe/src/app/app.routes.ts"
    imports_html = ROOT / "fe/src/app/features/imports/imports.page.html"
    next_release = ROOT / "be/src/main/resources/db/changelog/releases/next.changelog-master.yaml"

    imports_route = """      {
        path: 'imports',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/imports/imports.page').then((module) => module.ImportsPage),
      },"""
    new_routes = """      {
        // device-integrations-route: shares the Attendance Imports permission/menu scope.
        path: 'imports/device-integrations',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/device-integrations/device-integrations.page').then(
            (module) => module.DeviceIntegrationsPage,
          ),
      },
""" + imports_route
    patch_once(routes, "device-integrations-route", imports_route, new_routes)

    header_anchor = """    <div class=\"header-actions\" style=\"display: flex; gap: 0.5rem; align-items: center;\">\n      <button class=\"button gold\" (click)=\"showTemplateModal.set(true)\">"""
    header_replacement = """    <div class=\"header-actions\" style=\"display: flex; gap: 0.5rem; align-items: center;\">\n      <!-- device-integrations-entry -->\n      <a class=\"button secondary\" href=\"/imports/device-integrations\">\n        {{ i18n.t('deviceIntegrations.open', {}, i18n.locale() === 'ar-EG' ? 'تكامل الأجهزة' : 'Device integrations') }}\n      </a>\n      <button class=\"button gold\" (click)=\"showTemplateModal.set(true)\">"""
    patch_once(imports_html, "device-integrations-entry", header_anchor, header_replacement)

    release_text = next_release.read_text(encoding="utf-8") if next_release.exists() else ""
    if "20260809_v145_biometric_device_integrations.yaml" in release_text:
        print(f"[skip] {next_release}: already patched")
    else:
        addition = """\n  - include:\n      file: db/changelog/schema/create/20260809_v145_biometric_device_integrations.yaml\n  - include:\n      file: db/changelog/data/insert/20260809_v146_device_integration_translations.yaml\n"""
        if not release_text.rstrip().endswith("20260808_v144_workforce_category_error_translations.yaml"):
            print("[warn] Liquibase file no longer ends at v144; appending v145/v146 after current entries.")
        next_release.write_text(release_text.rstrip() + addition, encoding="utf-8")
        print(f"[ok]   {next_release}")

    print("\nDevice integrations patch applied. Next:")
    print("  1. docker compose -f be/compose.yaml up -d postgres device-hub")
    print("  2. cd be && ./gradlew bootRun   (Windows: gradlew.bat bootRun)")
    print("  3. Open /imports/device-integrations")


if __name__ == "__main__":
    main()
