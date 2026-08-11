#!/usr/bin/env python3
"""Bortqala device-hub -> be/modules/device-hub migration/recovery helper.

Target branch: fm_bemo_consolidated

Architecture after migration:
Angular -> Spring /api/v1/device-integrations -> VendorHubClient
       -> be/modules/device-hub -> vendor device/platform
       -> existing BiometricDeviceSyncService -> attendance/payroll

The browser never calls device-hub directly.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

EXPECTED_BRANCH = "fm_bemo_consolidated"
SOURCE_REL = Path("device-hub")
MODULE_REL = Path("be/modules/device-hub")


class IntegrationError(RuntimeError):
    pass


@dataclass
class Ctx:
    root: Path
    dry_run: bool
    copy_mode: bool
    run_builds: bool
    allow_other_branch: bool
    backup_root: Path
    changed: list[str]
    warnings: list[str]


def log(msg: str) -> None:
    print(f"[device-hub] {msg}")


def git_available(root: Path) -> bool:
    return (root / ".git").exists() and shutil.which("git") is not None


def current_branch(root: Path) -> str | None:
    if not git_available(root):
        return None
    p = subprocess.run(
        ["git", "branch", "--show-current"], cwd=root, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.DEVNULL
    )
    return p.stdout.strip() or None if p.returncode == 0 else None


HUB_REQUIRED = [
    Path("Dockerfile.bortqala"),
    Path("gateway/requirements.txt"),
    Path("gateway/app"),
    Path("packages"),
]


def preflight(ctx: Ctx) -> None:
    # Only validate the stable ERP/FE anchors here. Device Hub recovery happens
    # afterwards so a partial v1/v2 migration can heal itself.
    required = [
        Path("be"), Path("fe"), Path("be/compose.yaml"),
        Path("docker-compose.yml"), Path("docker-compose.prod.yml"),
        Path("be/src/main/java/com/bemo/hr/attendance/application/DeviceIntegrationService.java"),
        Path("be/src/main/java/com/bemo/hr/attendance/infrastructure/VendorHubClient.java"),
        Path("fe/src/app/app.routes.ts"),
        Path("fe/src/app/features/imports/imports.page.html"),
    ]
    missing = [str(p) for p in required if not (ctx.root / p).exists()]
    if missing:
        raise IntegrationError("Expected Bortqala files are missing:\n  - " + "\n  - ".join(missing))

    branch = current_branch(ctx.root)
    if branch and branch != EXPECTED_BRANCH:
        msg = f"Current branch {branch!r}; expected {EXPECTED_BRANCH!r}."
        if ctx.allow_other_branch:
            ctx.warnings.append(msg)
        else:
            raise IntegrationError(msg + " Use --allow-other-branch only intentionally.")


def hub_missing(hub: Path) -> list[Path]:
    return [rel for rel in HUB_REQUIRED if not (hub / rel).exists()]


def git_head_hub_entries(ctx: Ctx) -> list[tuple[str, str, str]]:
    """Return (mode, blob_sha, repo_path) for HEAD:device-hub tracked files."""
    if not git_available(ctx.root):
        return []
    p = subprocess.run(
        ["git", "ls-tree", "-r", "HEAD", "--", SOURCE_REL.as_posix()],
        cwd=ctx.root,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if p.returncode != 0:
        return []
    entries: list[tuple[str, str, str]] = []
    for line in p.stdout.splitlines():
        # Format: <mode> <type> <sha>\t<path>
        m = re.match(r"^(\d+)\s+blob\s+([0-9a-f]+)\t(.+)$", line)
        if m:
            entries.append((m.group(1), m.group(2), m.group(3)))
    return entries


def restore_missing_hub_files_from_git(ctx: Ctx, target: Path) -> int:
    """
    Restore only missing tracked files from HEAD:device-hub into target.

    This intentionally never overwrites an already migrated file. It is safe
    for the partial-migration case left by v1/v2 and avoids recreating the old
    top-level directory.
    """
    entries = git_head_hub_entries(ctx)
    if not entries:
        return 0

    restored = 0
    prefix = SOURCE_REL.as_posix().rstrip("/") + "/"
    for mode, blob_sha, repo_path in entries:
        if not repo_path.startswith(prefix):
            continue
        rel = Path(repo_path[len(prefix):])
        out = target / rel
        if out.exists():
            continue
        restored += 1
        if ctx.dry_run:
            continue
        out.parent.mkdir(parents=True, exist_ok=True)
        p = subprocess.run(
            ["git", "cat-file", "blob", blob_sha],
            cwd=ctx.root,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        if p.returncode != 0:
            raise IntegrationError(
                f"Could not restore {repo_path} from Git HEAD: "
                + p.stderr.decode("utf-8", errors="replace")
            )
        out.write_bytes(p.stdout)
        if mode == "100755" and os.name != "nt":
            out.chmod(out.stat().st_mode | 0o111)
    if restored:
        log(f"restore {restored} missing device-hub tracked file(s) from Git HEAD")
        if f"restore missing files into {MODULE_REL}/" not in ctx.changed:
            ctx.changed.append(f"restore missing files into {MODULE_REL}/")
    return restored


def merge_existing_source_into_target(ctx: Ctx, src: Path, dst: Path) -> None:
    """Merge an old top-level source into an already-created backend module."""
    conflicts: list[str] = []
    copied = 0
    for source_file in src.rglob("*"):
        if not source_file.is_file():
            continue
        rel = source_file.relative_to(src)
        target_file = dst / rel
        if target_file.exists():
            try:
                same = source_file.read_bytes() == target_file.read_bytes()
            except OSError:
                same = False
            if not same:
                conflicts.append(rel.as_posix())
            continue
        copied += 1
        if not ctx.dry_run:
            target_file.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source_file, target_file)

    if conflicts:
        sample = "\n  - ".join(conflicts[:20])
        raise IntegrationError(
            "Both device-hub locations contain different versions of the same file. "
            "Refusing to overwrite migrated work. Conflicts:\n  - " + sample
        )

    if copied:
        log(f"merge {copied} missing file(s) from {SOURCE_REL}/ into {MODULE_REL}/")

    if not ctx.copy_mode:
        log(f"remove old {SOURCE_REL}/ after safe merge")
        if not ctx.dry_run:
            shutil.rmtree(src)
    else:
        log(f"copy mode: keep old {SOURCE_REL}/")


def recover_or_move_hub(ctx: Ctx) -> None:
    src, dst = ctx.root / SOURCE_REL, ctx.root / MODULE_REL

    # Case 1: partial/complete target already exists (typical after v1 failure).
    if dst.exists():
        if src.exists():
            merge_existing_source_into_target(ctx, src, dst)
        # Fill any still-missing tracked files directly from current Git HEAD.
        restore_missing_hub_files_from_git(ctx, dst)
        missing = hub_missing(dst)
        if missing and not ctx.dry_run:
            raise IntegrationError(
                "device-hub recovery could not reconstruct required content:\n  - "
                + "\n  - ".join(str(MODULE_REL / p) for p in missing)
                + "\nRun `git status --short` and verify HEAD still contains the original device-hub tree."
            )
        log(f"{MODULE_REL}/ ready" if not missing else f"{MODULE_REL}/ recovery planned")
        return

    # Case 2: original top-level source still exists.
    if src.exists():
        # Heal the source first if an earlier operation partially damaged it.
        restore_missing_hub_files_from_git(ctx, src)
        missing = hub_missing(src)
        if missing and not ctx.dry_run:
            raise IntegrationError(
                "top-level device-hub is incomplete and Git HEAD could not restore it:\n  - "
                + "\n  - ".join(str(SOURCE_REL / p) for p in missing)
            )
        move_hub(ctx)
        return

    # Case 3: neither directory exists, but Git HEAD still has the tracked tree.
    entries = git_head_hub_entries(ctx)
    if entries:
        log(f"reconstruct {MODULE_REL}/ directly from Git HEAD")
        if not ctx.dry_run:
            dst.mkdir(parents=True, exist_ok=True)
        restore_missing_hub_files_from_git(ctx, dst)
        missing = hub_missing(dst)
        if missing and not ctx.dry_run:
            raise IntegrationError(
                "Git recovery completed but required device-hub content is still missing:\n  - "
                + "\n  - ".join(str(MODULE_REL / p) for p in missing)
            )
        return

    raise IntegrationError(
        f"Neither {SOURCE_REL}/ nor {MODULE_REL}/ exists, and Git HEAD does not contain {SOURCE_REL}/."
    )


def backup(ctx: Ctx, rel: Path) -> None:
    src = ctx.root / rel
    if ctx.dry_run or not src.exists():
        return
    dst = ctx.backup_root / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    if not dst.exists():
        shutil.copy2(src, dst)


def write(ctx: Ctx, rel: Path, text: str) -> None:
    path = ctx.root / rel
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old == text:
        return
    if old is not None:
        backup(ctx, rel)
    log(f"update {rel}")
    if not ctx.dry_run:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
    if str(rel) not in ctx.changed:
        ctx.changed.append(str(rel))


def move_hub(ctx: Ctx) -> None:
    src, dst = ctx.root / SOURCE_REL, ctx.root / MODULE_REL
    if dst.exists() and not src.exists():
        log(f"{MODULE_REL}/ already present")
        return
    if src.exists() and dst.exists():
        if ctx.copy_mode:
            log(f"{MODULE_REL}/ already copied; leaving {SOURCE_REL}/ in place")
            return
        raise IntegrationError(f"Both {SOURCE_REL}/ and {MODULE_REL}/ exist; refusing to overwrite.")

    log(f"{'copy' if ctx.copy_mode else 'move'} {SOURCE_REL}/ -> {MODULE_REL}/")
    if ctx.dry_run:
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    if ctx.copy_mode:
        shutil.copytree(src, dst)
    elif git_available(ctx.root):
        p = subprocess.run(["git", "mv", str(SOURCE_REL), str(MODULE_REL)], cwd=ctx.root)
        if p.returncode != 0:
            shutil.move(str(src), str(dst))
    else:
        shutil.move(str(src), str(dst))
    ctx.changed.append(f"{SOURCE_REL}/ -> {MODULE_REL}/")


def patch_be_compose(ctx: Ctx) -> None:
    rel = Path("be/compose.yaml")
    text = (ctx.root / rel).read_text(encoding="utf-8")
    new = text.replace("context: ../device-hub", "context: ./modules/device-hub")
    if new != text:
        write(ctx, rel, new)


def add_root_hub_service(text: str) -> str:
    if "\n  device-hub:\n" in text:
        return text
    anchor = "\n  # 2. Spring Boot Backend Service\n"
    if anchor not in text:
        anchor = "\n  backend:\n"
    if anchor not in text:
        raise IntegrationError("Cannot locate backend service in docker-compose.yml")
    block = '''
  # Backend-owned vendor/protocol adapter. Angular never calls this directly.
  device-hub:
    build:
      context: ./be/modules/device-hub
      dockerfile: Dockerfile.bortqala
    container_name: bemo-erp-device-hub
    restart: unless-stopped
    environment:
      DEVICE_HUB_REGISTRY_PATH: /data/devices.json
      DEVICE_HUB_API_KEY: ${DEVICE_HUB_API_KEY:-}
    ports:
      - "${DEVICE_HUB_PORT:-8090}:8090"
    volumes:
      - device_hub_data:/data
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8090/health', timeout=2)"]
      interval: 10s
      timeout: 4s
      retries: 10
      start_period: 10s
    networks:
      - internal
'''
    return text.replace(anchor, "\n" + block.rstrip() + "\n" + anchor.lstrip("\n"), 1)


def add_backend_dependency(text: str) -> str:
    if re.search(r"(?ms)^  backend:\n.*?^    depends_on:\n.*?^      device-hub:\n", text):
        return text
    anchor = "      db:\n        condition: service_healthy\n"
    backend_pos = text.find("\n  backend:\n")
    if backend_pos < 0:
        raise IntegrationError("Cannot locate backend service")
    pos = text.find(anchor, backend_pos)
    if pos < 0:
        raise IntegrationError("Cannot locate backend depends_on.db")
    end = pos + len(anchor)
    return text[:end] + "      device-hub:\n        condition: service_healthy\n" + text[end:]


def add_backend_env(text: str, production: bool) -> str:
    backend_start = text.find("\n  backend:\n")
    if backend_start < 0:
        raise IntegrationError("Cannot locate backend service for environment patch")
    next_service = text.find("\n  ", backend_start + len("\n  backend:\n"))
    # Search a bounded backend section more reliably by common anchors.
    if production:
        anchor = "      HR_DEVICE_CREDENTIALS_SECRET: ${HR_DEVICE_CREDENTIALS_SECRET:?HR_DEVICE_CREDENTIALS_SECRET must be a Base64-encoded 32-byte key}\n"
        addition = (
            "      # Internal Spring Boot -> backend device module communication.\n"
            "      BEMO_DEVICE_HUB_BASE_URL: http://device-hub:8090\n"
            "      DEVICE_HUB_API_KEY: ${DEVICE_HUB_API_KEY:?DEVICE_HUB_API_KEY must be set}\n"
        )
    else:
        anchor = "      HR_CORS_ALLOWED_ORIGINS: ${HR_CORS_ALLOWED_ORIGINS:-http://localhost:4200,http://127.0.0.1:4200,http://localhost:80}\n"
        addition = (
            "      # Internal Spring Boot -> backend device module communication.\n"
            "      BEMO_DEVICE_HUB_BASE_URL: http://device-hub:8090\n"
            "      DEVICE_HUB_API_KEY: ${DEVICE_HUB_API_KEY:-}\n"
        )
    if "BEMO_DEVICE_HUB_BASE_URL:" in text[backend_start:]:
        return text
    if anchor not in text:
        raise IntegrationError("Cannot locate backend environment insertion point")
    return text.replace(anchor, anchor + addition, 1)


def ensure_root_named_volume(text: str) -> str:
    """
    Ensure device_hub_data is declared under the TOP-LEVEL volumes section.

    Also repairs the v1 script bug which could produce:

        db:
          ...
          volumes:
      device_hub_data:
            - postgres_data:/var/lib/postgresql/data

    by removing the misplaced key and restoring the db volume indentation.
    """

    # Repair the exact malformed pattern produced by v1.
    malformed = (
        "    volumes:\n"
        "  device_hub_data:\n"
        "      - postgres_data:/var/lib/postgresql/data\n"
    )
    fixed = (
        "    volumes:\n"
        "      - postgres_data:/var/lib/postgresql/data\n"
    )
    if malformed in text:
        text = text.replace(malformed, fixed, 1)

    lines = text.splitlines(keepends=True)

    # Remove any stray top-level device_hub_data entry that is not inside the
    # actual root `volumes:` block. This is deliberately conservative: only the
    # exact two-space key with no child content is removed.
    cleaned: list[str] = []
    top_volumes_index: int | None = None
    for line in lines:
        if re.match(r"^volumes:\s*$", line.rstrip("\r\n")):
            top_volumes_index = len(cleaned)
        if (
            line.rstrip("\r\n") == "  device_hub_data:"
            and top_volumes_index is None
        ):
            continue
        cleaned.append(line)
    lines = cleaned

    # Locate the top-level volumes section using column 0 only.
    top_volumes_index = None
    for i, line in enumerate(lines):
        if re.match(r"^volumes:\s*$", line.rstrip("\r\n")):
            top_volumes_index = i
            break
    if top_volumes_index is None:
        raise IntegrationError("Cannot locate top-level compose volumes section")

    # Find the end of the root volumes block (next non-empty top-level key).
    block_end = len(lines)
    for i in range(top_volumes_index + 1, len(lines)):
        raw = lines[i].rstrip("\r\n")
        if raw and not raw.startswith((" ", "\t", "#")):
            block_end = i
            break

    if any(
        line.rstrip("\r\n") == "  device_hub_data:"
        for line in lines[top_volumes_index + 1:block_end]
    ):
        return "".join(lines)

    newline = "\r\n" if any(line.endswith("\r\n") for line in lines) else "\n"
    lines.insert(top_volumes_index + 1, f"  device_hub_data:{newline}")
    return "".join(lines)


def patch_root_compose(ctx: Ctx) -> None:
    rel = Path("docker-compose.yml")
    text = (ctx.root / rel).read_text(encoding="utf-8")
    new = add_root_hub_service(text)
    new = add_backend_dependency(new)
    new = add_backend_env(new, production=False)
    new = ensure_root_named_volume(new)
    if new != text:
        write(ctx, rel, new)


def patch_prod_compose(ctx: Ctx) -> None:
    rel = Path("docker-compose.prod.yml")
    text = (ctx.root / rel).read_text(encoding="utf-8")
    new = add_backend_env(text, production=True)
    if "\n  device-hub:\n" not in new:
        anchor = "\n  backend:\n"
        block = '''
  device-hub:
    # Production: internal network only; never publish the adapter port.
    ports: !reset []
    environment:
      DEVICE_HUB_REGISTRY_PATH: /data/devices.json
      DEVICE_HUB_API_KEY: ${DEVICE_HUB_API_KEY:?DEVICE_HUB_API_KEY must be set}
'''
        if anchor not in new:
            raise IntegrationError("Cannot locate backend in production compose")
        new = new.replace(anchor, "\n" + block.rstrip() + "\n" + anchor.lstrip("\n"), 1)
    if new != text:
        write(ctx, rel, new)


def patch_env_examples(ctx: Ctx) -> None:
    specs = {
        Path(".env.development.example"): (
            "\n# Backend device module\n"
            "BEMO_DEVICE_HUB_BASE_URL=http://localhost:8090\n"
            "DEVICE_HUB_API_KEY=\n"
            "DEVICE_HUB_PORT=8090\n"
        ),
        Path(".env.production.example"): (
            "\n# Backend device module\n"
            "# Docker sets BEMO_DEVICE_HUB_BASE_URL=http://device-hub:8090\n"
            "DEVICE_HUB_API_KEY=CHANGE_ME_TO_A_LONG_RANDOM_SECRET\n"
        ),
    }
    for rel, addition in specs.items():
        path = ctx.root / rel
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        if "DEVICE_HUB_API_KEY=" not in text:
            write(ctx, rel, text.rstrip() + "\n" + addition)


def ensure_frontend_route(ctx: Ctx) -> None:
    rel = Path("fe/src/app/app.routes.ts")
    text = (ctx.root / rel).read_text(encoding="utf-8")
    if "path: 'imports/device-integrations'" in text:
        return
    anchor = """      {
        path: 'imports',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/imports/imports.page').then((module) => module.ImportsPage),
      },
"""
    addition = anchor + """      {
        // Device Hub is backend-owned; browser traffic stays behind Spring Boot.
        path: 'imports/device-integrations',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/device-integrations/device-integrations.page').then(
            (module) => module.DeviceIntegrationsPage,
          ),
      },
"""
    if anchor not in text:
        raise IntegrationError("Cannot locate imports route in app.routes.ts")
    write(ctx, rel, text.replace(anchor, addition, 1))


def ensure_imports_entry(ctx: Ctx) -> None:
    rel = Path("fe/src/app/features/imports/imports.page.html")
    text = (ctx.root / rel).read_text(encoding="utf-8")
    if 'href="/imports/device-integrations"' in text:
        return
    idx = text.find('<div class="header-actions"')
    if idx < 0:
        raise IntegrationError("Cannot locate imports page header actions")
    gt = text.find(">", idx)
    addition = '''
      <!-- device-integrations-entry -->
      <a class="button secondary" href="/imports/device-integrations">
        {{ i18n.t('deviceIntegrations.open', {}, i18n.locale() === 'ar-EG' ? 'تكامل الأجهزة' : 'Device integrations') }}
      </a>'''
    write(ctx, rel, text[:gt+1] + addition + text[gt+1:])


def patch_frontend_note(ctx: Ctx) -> None:
    rel = Path("fe/src/app/features/device-integrations/device-integrations.page.html")
    path = ctx.root / rel
    if not path.exists():
        ctx.warnings.append("Device integrations page is missing; no communication note added.")
        return
    text = path.read_text(encoding="utf-8")
    if "deviceIntegrations.communicationPath" not in text:
        anchor = '  <section class="summary-grid">'
        note = '''  <div class="integration-communication-note">
    <strong>{{ i18n.t('deviceIntegrations.communicationTitle', {}, 'Secure communication path') }}</strong>
    <span>{{ i18n.t('deviceIntegrations.communicationPath', {}, 'Browser → Bortqala API → backend device module → vendor device/platform') }}</span>
    <small>{{ i18n.t('deviceIntegrations.communicationHint', {}, 'The browser never connects directly to device credentials or Device Hub.') }}</small>
  </div>

'''
        if anchor in text:
            write(ctx, rel, text.replace(anchor, note + anchor, 1))
        else:
            ctx.warnings.append("Could not add FE communication note; summary-grid anchor missing.")

    scss_rel = Path("fe/src/app/features/device-integrations/device-integrations.page.scss")
    scss_path = ctx.root / scss_rel
    if scss_path.exists():
        scss = scss_path.read_text(encoding="utf-8")
        if ".integration-communication-note" not in scss:
            css = '''

.integration-communication-note {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem 0.75rem;
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface-2);

  strong { font-weight: 700; }
  span { direction: ltr; unicode-bidi: plaintext; }
  small { flex-basis: 100%; color: var(--secondary-text); }
}
'''
            write(ctx, scss_rel, scss.rstrip() + "\n" + css)


def verify_frontend_boundary(ctx: Ctx) -> None:
    path = ctx.root / "fe/src/app/features/device-integrations/device-integrations.store.ts"
    if not path.exists():
        raise IntegrationError("DeviceIntegrationsStore is missing")
    text = path.read_text(encoding="utf-8")
    offenders = [x for x in ["localhost:8090", "device-hub:8090", "/v1/resolve-route", "/v1/suppliers"] if x in text]
    if offenders:
        raise IntegrationError(f"Angular appears to call Device Hub directly: {offenders}")
    if "/api/v1/device-integrations" not in text:
        raise IntegrationError("Angular is not using Spring /api/v1/device-integrations")


def patch_docs(ctx: Ctx) -> None:
    rel = Path("README_DEVICE_INTEGRATIONS.md")
    path = ctx.root / rel
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    new = text.replace("`device-hub/` — all supplier packages", "`be/modules/device-hub/` — all supplier packages")
    new = new.replace("device-hub/packages/suppliers/", "be/modules/device-hub/packages/suppliers/")
    new = new.replace("device-hub/docs/OFFICIAL_DOCUMENTATION_INDEX.md", "be/modules/device-hub/docs/OFFICIAL_DOCUMENTATION_INDEX.md")
    if "## Backend-owned module layout" not in new:
        section = '''

## Backend-owned module layout

```text
be/
├─ src/main/java/com/bemo/hr/attendance/
│  ├─ api/                         # /api/v1/device-integrations
│  ├─ application/                 # orchestration + attendance sync
│  └─ infrastructure/VendorHubClient.java
└─ modules/device-hub/             # Python vendor/protocol adapter runtime
```

Angular communicates only with Spring Boot. Spring Boot communicates with the
Device Hub module over the internal service URL. Credentials remain encrypted
and owned by Bortqala.
'''
        anchor = "\n## Architecture\n"
        new = new.replace(anchor, section + anchor, 1) if anchor in new else new + section
    if new != text:
        write(ctx, rel, new)


def write_module_readme(ctx: Ctx) -> None:
    rel = Path("be/modules/README.md")
    path = ctx.root / rel
    if path.exists() and "device-hub" in path.read_text(encoding="utf-8"):
        return
    content = '''# Backend auxiliary modules

## device-hub

`device-hub/` is Bortqala's backend-owned vendor/protocol adapter runtime.
It is not a second ERP backend and is not an Angular API.

```text
Angular
  -> /api/v1/device-integrations
  -> Spring Boot
  -> VendorHubClient
  -> http://device-hub:8090 (Docker internal network)
  -> vendor device/platform
```

Rules:

1. Angular never calls device-hub directly.
2. Bortqala remains the system of record.
3. Device credentials remain encrypted in Bortqala.
4. Device Hub stores route/protocol metadata, not reusable device secrets.
5. Normalized punches return to the existing `BiometricDeviceSyncService`.
'''
    write(ctx, rel, content)


def validate_static(ctx: Ctx) -> None:
    if not ctx.dry_run:
        if not (ctx.root / MODULE_REL).exists():
            raise IntegrationError(f"{MODULE_REL}/ was not created")
        if not ctx.copy_mode and (ctx.root / SOURCE_REL).exists():
            raise IntegrationError(f"{SOURCE_REL}/ still exists after move mode")

    root_compose = (ctx.root / "docker-compose.yml").read_text(encoding="utf-8")
    for token in ["device-hub:", "context: ./be/modules/device-hub", "BEMO_DEVICE_HUB_BASE_URL: http://device-hub:8090", "device_hub_data:"]:
        if token not in root_compose and not ctx.dry_run:
            raise IntegrationError(f"Root compose validation failed: missing {token}")

    if not ctx.dry_run:
        try:
            import yaml  # type: ignore
            parsed = yaml.safe_load(root_compose)
            if not isinstance(parsed, dict):
                raise IntegrationError("docker-compose.yml did not parse as a YAML mapping")
            services = parsed.get("services")
            volumes = parsed.get("volumes")
            if not isinstance(services, dict) or "device-hub" not in services:
                raise IntegrationError("device-hub is not under top-level services")
            if not isinstance(volumes, dict) or "device_hub_data" not in volumes:
                raise IntegrationError("device_hub_data is not under top-level volumes")
        except ImportError:
            pass
        except IntegrationError:
            raise
        except Exception as exc:
            raise IntegrationError(f"docker-compose.yml YAML parse failed: {exc}") from exc

    be_compose = (ctx.root / "be/compose.yaml").read_text(encoding="utf-8")
    if "context: ./modules/device-hub" not in be_compose and not ctx.dry_run:
        raise IntegrationError("be/compose.yaml still points outside be/")

    vendor_client = (ctx.root / "be/src/main/java/com/bemo/hr/attendance/infrastructure/VendorHubClient.java").read_text(encoding="utf-8")
    if "BEMO_DEVICE_HUB_BASE_URL" not in vendor_client:
        raise IntegrationError("VendorHubClient is missing BEMO_DEVICE_HUB_BASE_URL")

    service = (ctx.root / "be/src/main/java/com/bemo/hr/attendance/application/DeviceIntegrationService.java").read_text(encoding="utf-8")
    if "BiometricDeviceSyncService" not in service:
        raise IntegrationError("DeviceIntegrationService no longer uses existing biometric sync flow")
    verify_frontend_boundary(ctx)


def validate_runtime(ctx: Ctx) -> None:
    validate_static(ctx)
    if ctx.dry_run:
        return
    module = ctx.root / MODULE_REL
    p = subprocess.run([sys.executable, "-m", "compileall", "-q", str(module / "gateway"), str(module / "packages")], cwd=ctx.root)
    if p.returncode != 0:
        raise IntegrationError("Python device-hub compile check failed")

    if shutil.which("docker"):
        for cmd in [
            ["docker", "compose", "-f", "docker-compose.yml", "config", "--quiet"],
            ["docker", "compose", "-f", "be/compose.yaml", "config", "--quiet"],
        ]:
            p = subprocess.run(cmd, cwd=ctx.root, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            if p.returncode != 0:
                raise IntegrationError("Compose validation failed:\n" + p.stdout)
    else:
        ctx.warnings.append("Docker unavailable; compose runtime validation skipped.")

    if ctx.run_builds:
        gradle = ctx.root / "be" / ("gradlew.bat" if os.name == "nt" else "gradlew")
        if gradle.exists():
            p = subprocess.run([str(gradle), "test"], cwd=ctx.root / "be")
            if p.returncode != 0:
                raise IntegrationError("Gradle tests failed")
        else:
            ctx.warnings.append("Gradle wrapper missing; backend build skipped.")
        npm = shutil.which("npm")
        if npm and (ctx.root / "fe/package.json").exists():
            p = subprocess.run([npm, "run", "build"], cwd=ctx.root / "fe")
            if p.returncode != 0:
                raise IntegrationError("Angular build failed")
        else:
            ctx.warnings.append("npm unavailable; Angular build skipped.")


def write_report(ctx: Ctx) -> None:
    rel = Path("DEVICE_HUB_BE_MODULE_MIGRATION_REPORT.md")
    changed = "\n".join(f"- `{x}`" for x in ctx.changed) or "- No changes required."
    warnings = "\n".join(f"- {x}" for x in ctx.warnings) or "- None."
    report = f'''# Device Hub → Backend Module Migration Report

Generated: {dt.datetime.now().astimezone().isoformat(timespec="seconds")}

## Architecture

```text
Angular
  -> /api/v1/device-integrations
  -> Spring Boot DeviceIntegrationService
  -> VendorHubClient
  -> be/modules/device-hub
  -> vendor device/platform
  -> normalized punches
  -> BiometricDeviceSyncService
  -> attendance/payroll
```

## Changed paths

{changed}

## Enforced communication rules

- Frontend uses only `/api/v1/device-integrations`.
- Spring Boot uses `BEMO_DEVICE_HUB_BASE_URL`.
- Docker uses internal `http://device-hub:8090`.
- Production requires `DEVICE_HUB_API_KEY`.
- Production does not publish the Device Hub port.
- Existing Bortqala encrypted credentials and biometric sync remain authoritative.

## Warnings

{warnings}

## Recommended validation

```bash
docker compose -f docker-compose.yml config
docker compose -f be/compose.yaml config
cd be && ./gradlew test
cd ../fe && npm run build
```

Open `/imports/device-integrations` after startup.
'''
    write(ctx, rel, report)


def save_manifest(ctx: Ctx) -> None:
    if ctx.dry_run:
        return
    ctx.backup_root.mkdir(parents=True, exist_ok=True)
    (ctx.backup_root / "manifest.json").write_text(json.dumps({
        "createdAt": dt.datetime.now().astimezone().isoformat(),
        "branch": EXPECTED_BRANCH,
        "mode": "copy" if ctx.copy_mode else "move",
        "changed": ctx.changed,
        "warnings": ctx.warnings,
    }, indent=2), encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description="Move/recover Bortqala device-hub into be/modules and wire FE/BE communication.")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--copy", action="store_true", help="Copy instead of move; useful for staged testing.")
    ap.add_argument("--run-builds", action="store_true", help="Run Gradle tests and Angular build after migration.")
    ap.add_argument("--allow-other-branch", action="store_true")
    args = ap.parse_args()

    root = Path(args.repo_root).expanduser().resolve()
    stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
    ctx = Ctx(root, args.dry_run, args.copy, args.run_builds, args.allow_other_branch,
              root / f".device-hub-be-module-backup-{stamp}", [], [])
    try:
        log(f"repo: {root}")
        preflight(ctx)
        recover_or_move_hub(ctx)
        patch_be_compose(ctx)
        patch_root_compose(ctx)
        patch_prod_compose(ctx)
        patch_env_examples(ctx)
        ensure_frontend_route(ctx)
        ensure_imports_entry(ctx)
        patch_frontend_note(ctx)
        patch_docs(ctx)
        write_module_readme(ctx)
        validate_runtime(ctx)
        write_report(ctx)
        save_manifest(ctx)
        log("completed successfully")
        if ctx.dry_run:
            log("dry-run: no files changed")
        else:
            log(f"backup: {ctx.backup_root.relative_to(ctx.root)}")
            log("report: DEVICE_HUB_BE_MODULE_MIGRATION_REPORT.md")
        for w in ctx.warnings:
            print(f"WARNING: {w}")
        return 0
    except IntegrationError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    except Exception as exc:
        print(f"UNEXPECTED ERROR: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 99


if __name__ == "__main__":
    raise SystemExit(main())
