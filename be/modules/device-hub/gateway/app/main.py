from __future__ import annotations
import base64, dataclasses, os
from fastapi import FastAPI, HTTPException, Header, Query, Request
from fastapi.responses import JSONResponse
from .models import Vendor, Device, DeviceCreate, RouteResolution, RouteCandidateModel, SupplierInfo
from .registry import registry
from .supplier_loader import (
    SUPPLIERS, resolve_version_aware, route_inventory, documentation_path,
    integration_versions, adapter_for, identity_for
)

app=FastAPI(title='Universal Biometric & Access Hub - Bortqala Edition',version='0.5.0')

@app.middleware('http')
async def optional_api_key(request: Request, call_next):
    required=os.getenv('DEVICE_HUB_API_KEY','').strip()
    if required and request.url.path != '/health' and request.headers.get('X-Device-Hub-Key','') != required:
        return JSONResponse(status_code=401,content={'detail':'invalid device hub API key'})
    return await call_next(request)

@app.get('/health')
def health():
    return {'ok':True,'suppliers':list(SUPPLIERS),'architecture':'supplier-packaged-version-aware','version':'0.5.0'}

@app.get('/v1/suppliers')
def suppliers():
    return [SupplierInfo(supplier=Vendor(v),routes=route_inventory(v),documentation_file=str(documentation_path(v))) for v in SUPPLIERS]

@app.get('/v1/suppliers/{vendor}/routes')
def routes(vendor: Vendor):
    return {'supplier':vendor.value,'routes':integration_versions(vendor.value)}

@app.post('/v1/resolve-route', response_model=RouteResolution)
def resolve_route(req: DeviceCreate):
    d=resolve_version_aware(req.vendor.value,req)
    candidates=[RouteCandidateModel(
        route=c.route,kind=c.kind,status=c.status,reason=c.reason,
        sdk_version_spec=c.sdk_version_spec,api_version_spec=c.api_version_spec,
        server_version_spec=c.server_version_spec,implementation_status=c.implementation_status,
        official_documentation=list(c.documentation)) for c in d.candidates]
    preferred=d.routes[0] if d.routes else None
    docs=[]
    for c in d.candidates:
        for u in c.documentation:
            if u not in docs: docs.append(u)
    return RouteResolution(
        supplier=req.vendor,model_pattern=d.matched_rule,generation_or_version='version-aware',
        preferred_route=preferred,compatible_routes=list(d.routes),candidates=candidates,
        notes=d.reason,official_documentation=docs)

@app.get('/v1/devices', response_model=list[Device])
def list_devices(): return registry.list()

@app.get('/v1/devices/{device_id}', response_model=Device)
def get_device(device_id: str):
    try: return registry.get(device_id)
    except KeyError: raise HTTPException(404,'device not found')

@app.post('/v1/devices', response_model=Device)
def add_device(req: DeviceCreate):
    req=_pin_route(req)
    return registry.add(req)

@app.put('/v1/devices/{device_id}', response_model=Device)
def update_device(device_id: str, req: DeviceCreate):
    try: registry.get(device_id)
    except KeyError: raise HTTPException(404,'device not found')
    return registry.update(device_id,_pin_route(req))

@app.delete('/v1/devices/{device_id}')
def delete_device(device_id: str):
    try: registry.delete(device_id)
    except KeyError: raise HTTPException(404,'device not found')
    return {'deleted':True,'id':device_id}

@app.post('/v1/devices/{device_id}/probe')
def probe_device(device_id: str, authorization: str | None = Header(default=None)):
    device=_get(device_id)
    identity=identity_for(device.vendor.value,device)
    adapter=adapter_for(device.vendor.value,_required_route(device))
    config=_runtime_config(device,authorization)
    try:
        result=adapter.probe(identity,config)
        return dataclasses.asdict(result) if dataclasses.is_dataclass(result) else result
    except Exception as exc:
        return {'ok':False,'route':device.route,'detail':str(exc),'data':{}}

@app.get('/v1/devices/{device_id}/punches')
def device_punches(
    device_id: str,
    since: str | None = Query(default=None),
    authorization: str | None = Header(default=None),
):
    device=_get(device_id)
    identity=identity_for(device.vendor.value,device)
    adapter=adapter_for(device.vendor.value,_required_route(device))
    config=_runtime_config(device,authorization)
    try:
        punches=adapter.fetch_punches(identity,config,since)
        if len(punches)>10000:
            raise HTTPException(409,'vendor response exceeds 10000 punch records; configure pagination/windowing')
        return {'punches':punches,'vendor':device.vendor.value,'route':device.route,'deviceId':device.id}
    except NotImplementedError as exc:
        raise HTTPException(501,str(exc))
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(502,f'{device.vendor.value}/{device.route} event retrieval failed: {exc}')

def _get(device_id: str) -> Device:
    try: return registry.get(device_id)
    except KeyError: raise HTTPException(404,'device not found')

def _required_route(device: Device) -> str:
    if not device.route: raise HTTPException(409,'device has no pinned integration route')
    return device.route

def _pin_route(req: DeviceCreate) -> DeviceCreate:
    if not req.route:
        d=resolve_version_aware(req.vendor.value,req)
        if not d.routes:
            states=', '.join(f'{c.route}:{c.status}' for c in d.candidates)
            raise HTTPException(400,f'no version-verified route can be pinned yet; {states}')
        req=req.model_copy(update={'route':d.routes[0]})
    if req.route not in route_inventory(req.vendor.value):
        raise HTTPException(400,f'route {req.route!r} is not part of supplier package {req.vendor.value!r}')
    # Never persist vendor passwords in the hub registry. Bortqala provides them at runtime over Basic auth.
    return req.model_copy(update={'password':None})

def _runtime_config(device: Device, authorization: str | None) -> dict:
    options=dict(device.options or {})
    username=device.username or ''
    password=''
    if authorization and authorization.lower().startswith('basic '):
        try:
            raw=base64.b64decode(authorization.split(' ',1)[1]).decode('utf-8')
            username,password=(raw.split(':',1)+[''])[:2]
        except Exception:
            raise HTTPException(400,'invalid Basic authorization header')
    config={
        **options,
        'host':device.host,
        'port':device.port,
        'base_url':device.base_url,
        'username':username,
        'password':password,
        'sdk_version':device.sdk_versions.get(device.route or '',''),
        'api_version':device.api_versions.get(device.route or '',''),
    }
    return {k:v for k,v in config.items() if v is not None}
