from __future__ import annotations
from dataclasses import dataclass, field
from typing import Any, Mapping

@dataclass(frozen=True)
class DeviceIdentity:
    model: str = "UNKNOWN"
    firmware: str = ""
    platform_version: str = ""
    server_version: str = ""
    os_name: str = ""
    architecture: str = ""
    sdk_versions: Mapping[str, str] = field(default_factory=dict)
    api_versions: Mapping[str, str] = field(default_factory=dict)
    capability_hints: tuple[str, ...] = ()
    metadata: Mapping[str, Any] = field(default_factory=dict)

@dataclass(frozen=True)
class RouteCandidate:
    route: str
    kind: str
    status: str
    reason: str
    sdk_version_spec: str = "*"
    api_version_spec: str = "*"
    server_version_spec: str = "*"
    implementation_status: str = "scaffold"
    documentation: tuple[str, ...] = ()

@dataclass(frozen=True)
class RouteDecision:
    supplier: str
    model: str
    matched_rule: str
    routes: tuple[str, ...]
    reason: str
    candidates: tuple[RouteCandidate, ...] = ()

@dataclass(frozen=True)
class AdapterResult:
    ok: bool
    route: str
    detail: str
    data: Mapping[str, Any] = field(default_factory=dict)
