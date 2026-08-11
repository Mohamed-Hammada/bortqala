from __future__ import annotations
import json, re
from pathlib import Path
from .models import DeviceIdentity, RouteDecision, RouteCandidate
from .versioning import version_satisfies

SUPPLIER = 'zkteco'

def _rules():
    p = Path(__file__).resolve().parents[2] / "profiles" / "device_routes.json"
    return json.loads(p.read_text(encoding="utf-8"))

def _catalog():
    p = Path(__file__).resolve().parents[2] / "profiles" / "integration_versions.json"
    rows=json.loads(p.read_text(encoding="utf-8"))
    return {x["route"]:x for x in rows}

def _rule_matches(rule, identity: DeviceIdentity):
    if not re.search(rule["model_pattern"], identity.model or "UNKNOWN", re.I): return False
    for field,spec_key in ((identity.firmware,"firmware_spec"),(identity.platform_version,"platform_version_spec"),(identity.server_version,"server_version_spec")):
        spec=rule.get(spec_key,"*")
        if spec not in ("*","any","") and not version_satisfies(field,spec): return False
    return True

def _candidate(route, identity: DeviceIdentity, meta):
    kind=meta.get("kind","other")
    docs=tuple(meta.get("official_documentation",[]))
    impl=meta.get("implementation_status","scaffold")
    sdk_spec=meta.get("sdk_version_spec","*")
    api_spec=meta.get("api_version_spec","*")
    server_spec=meta.get("server_version_spec","*")
    allowed_os=[x.lower() for x in meta.get("os",[])]
    allowed_arch=[x.lower() for x in meta.get("architectures",[])]
    if allowed_os and identity.os_name and identity.os_name.lower() not in allowed_os:
        return RouteCandidate(route,kind,"INCOMPATIBLE",f"OS {identity.os_name} is not in {allowed_os}",sdk_spec,api_spec,server_spec,impl,docs)
    if allowed_arch and identity.architecture and identity.architecture.lower() not in allowed_arch:
        return RouteCandidate(route,kind,"INCOMPATIBLE",f"architecture {identity.architecture} is not in {allowed_arch}",sdk_spec,api_spec,server_spec,impl,docs)
    if server_spec not in ("*","any",""):
        actual_platform = identity.server_version or identity.platform_version
        if not actual_platform:
            return RouteCandidate(route,kind,"NEEDS_SERVER_VERSION",f"server/platform version required: {server_spec}",sdk_spec,api_spec,server_spec,impl,docs)
        if not version_satisfies(actual_platform,server_spec):
            return RouteCandidate(route,kind,"INCOMPATIBLE",f"server/platform {actual_platform} does not satisfy {server_spec}",sdk_spec,api_spec,server_spec,impl,docs)
    if kind=="sdk":
        installed=identity.sdk_versions.get(route,"")
        if meta.get("requires_explicit_sdk_version",True) and not installed:
            return RouteCandidate(route,kind,"NEEDS_SDK_VERSION","exact installed/vendor SDK version must be supplied before this route can be pinned",sdk_spec,api_spec,server_spec,impl,docs)
        if installed and sdk_spec not in ("*","any","") and not version_satisfies(installed,sdk_spec):
            return RouteCandidate(route,kind,"INCOMPATIBLE",f"SDK {installed} does not satisfy {sdk_spec}",sdk_spec,api_spec,server_spec,impl,docs)
        if meta.get("compatibility_matrix_status") == "vendor-matrix-required":
            return RouteCandidate(route,kind,"NEEDS_VENDOR_MATRIX",f"SDK {installed or 'version'} must be validated against the vendor device/firmware matrix",sdk_spec,api_spec,server_spec,impl,docs)
    if kind=="api" and meta.get("requires_explicit_api_version",False):
        actual=identity.api_versions.get(route,"")
        if not actual:
            return RouteCandidate(route,kind,"NEEDS_API_VERSION",f"API version required: {api_spec}",sdk_spec,api_spec,server_spec,impl,docs)
        if api_spec not in ("*","any","") and not version_satisfies(actual,api_spec):
            return RouteCandidate(route,kind,"INCOMPATIBLE",f"API {actual} does not satisfy {api_spec}",sdk_spec,api_spec,server_spec,impl,docs)
    return RouteCandidate(route,kind,"COMPATIBLE","version constraints satisfied or capability-negotiated",sdk_spec,api_spec,server_spec,impl,docs)

def select_routes(identity: DeviceIdentity) -> RouteDecision:
    rules=_rules(); catalog=_catalog(); unknown=None; matched=None
    for rule in rules:
        if rule["model_pattern"] == "UNKNOWN": unknown=rule; continue
        if _rule_matches(rule,identity): matched=rule; break
    matched=matched or unknown
    if not matched: return RouteDecision(SUPPLIER,identity.model,"NONE",(),"No routing rule",())
    candidates=[]
    for route in matched.get("routes",[]):
        candidates.append(_candidate(route,identity,catalog.get(route,{"route":route})))
    # Only positively compatible routes are auto-routable. All NEEDS_* states require more facts / vendor verification.
    compatible=tuple(c.route for c in candidates if c.status=="COMPATIBLE")
    return RouteDecision(SUPPLIER,identity.model,matched["model_pattern"],compatible,matched.get("notes",""),tuple(candidates))
