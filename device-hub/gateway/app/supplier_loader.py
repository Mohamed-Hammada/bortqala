from __future__ import annotations
import json, re, sys, importlib
from pathlib import Path

SUPPLIERS = ("zkteco","hikvision","dahua","suprema","virdi","anviz","honeywell")
ROOT = Path(__file__).resolve().parents[2]

def package_root(supplier: str) -> Path:
    if supplier not in SUPPLIERS: raise KeyError(supplier)
    return ROOT / "packages" / "suppliers" / supplier

def rules_for(supplier: str):
    return json.loads((package_root(supplier)/"profiles"/"device_routes.json").read_text(encoding="utf-8"))

def integration_versions(supplier: str):
    return json.loads((package_root(supplier)/"profiles"/"integration_versions.json").read_text(encoding="utf-8"))

def _modules(supplier: str):
    src=package_root(supplier)/"src"
    if str(src) not in sys.path: sys.path.insert(0,str(src))
    m=importlib.import_module(f"supplier_{supplier}.models")
    r=importlib.import_module(f"supplier_{supplier}.router")
    return m,r

def resolve_version_aware(supplier: str, req):
    m,r=_modules(supplier)
    ident=m.DeviceIdentity(model=req.model,firmware=req.firmware,platform_version=req.platform_version,server_version=req.server_version,os_name=req.os_name,architecture=req.architecture,sdk_versions=req.sdk_versions,api_versions=req.api_versions,capability_hints=tuple(req.capability_hints),metadata=req.options)
    return r.select_routes(ident)

def route_inventory(supplier: str):
    path=package_root(supplier)/"integrations"
    return sorted(p.name for p in path.iterdir() if p.is_dir())

def documentation_path(supplier: str): return package_root(supplier)/"DOCUMENTATION.md"

def identity_for(supplier: str, req):
    m,_=_modules(supplier)
    return m.DeviceIdentity(
        model=req.model,
        firmware=req.firmware,
        platform_version=req.platform_version,
        server_version=req.server_version,
        os_name=req.os_name,
        architecture=req.architecture,
        sdk_versions=req.sdk_versions,
        api_versions=req.api_versions,
        capability_hints=tuple(req.capability_hints),
        metadata=req.options,
    )

def adapter_for(supplier: str, route: str):
    if route not in route_inventory(supplier):
        raise KeyError(f'route {route!r} is not part of supplier {supplier!r}')
    src=package_root(supplier)/"src"
    if str(src) not in sys.path: sys.path.insert(0,str(src))
    module_name=route.replace('-','_')
    module=importlib.import_module(f"supplier_{supplier}.adapters.{module_name}")
    base=importlib.import_module(f"supplier_{supplier}.adapters.base").Adapter
    for value in vars(module).values():
        if isinstance(value,type) and issubclass(value,base) and value is not base and getattr(value,'route',None)==route:
            return value()
    raise LookupError(f'No adapter class found for {supplier}/{route}')
