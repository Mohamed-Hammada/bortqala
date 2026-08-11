from __future__ import annotations
import base64, json, os, socket, urllib.parse, urllib.request, urllib.error
from ..models import AdapterResult, DeviceIdentity

class Adapter:
    route = "base"
    kind = "base"
    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        return AdapterResult(False, self.route, "Probe is not implemented for this route")
    def fetch_punches(self, identity: DeviceIdentity, config: dict, since: str | None = None) -> list[dict]:
        raise NotImplementedError(f"Punch/event retrieval is not implemented for route {self.route}")

class HttpAdapter(Adapter):
    probe_path = "/"
    punch_path = ""

    def _opener(self, config: dict):
        auth=(config.get("auth") or "").lower()
        if auth == "digest":
            mgr=urllib.request.HTTPPasswordMgrWithDefaultRealm()
            mgr.add_password(None, config.get("base_url",""), config.get("username",""), config.get("password",""))
            return urllib.request.build_opener(urllib.request.HTTPDigestAuthHandler(mgr))
        return urllib.request.build_opener()

    def request(self, config: dict, method: str, path: str, *, body=None, headers=None):
        base=config.get("base_url")
        if not base: raise ValueError("base_url is required")
        url=base.rstrip('/') + ('/' + path.lstrip('/') if path else '')
        hdrs={str(k):str(v) for k,v in (config.get("headers") or {}).items()}
        hdrs.update({str(k):str(v) for k,v in (headers or {}).items()})
        auth=(config.get("auth") or "").lower()
        token=config.get("token")
        if token: hdrs.setdefault("Authorization",f"Bearer {token}")
        if auth=="bearer" and config.get("password"):
            hdrs.setdefault("Authorization",f"Bearer {config.get('password')}")
        if auth in ("api-key","apikey","header") and config.get("password"):
            hdrs.setdefault(str(config.get("auth_header") or "X-API-Key"),str(config.get("password")))
        if auth=="basic":
            raw=f"{config.get('username','')}:{config.get('password','')}".encode()
            hdrs.setdefault("Authorization","Basic "+base64.b64encode(raw).decode())
        data=None
        if body is not None:
            if isinstance(body,(dict,list)):
                data=json.dumps(body).encode('utf-8'); hdrs.setdefault('Content-Type','application/json')
            elif isinstance(body,str): data=body.encode('utf-8')
            else: data=body
        req=urllib.request.Request(url,data=data,headers=hdrs,method=method.upper())
        timeout=float(config.get("timeout",10))
        try:
            with self._opener(config).open(req,timeout=timeout) as r:
                payload=r.read()
                ctype=r.headers.get('Content-Type','')
                parsed=payload
                if 'json' in ctype:
                    try: parsed=json.loads(payload.decode('utf-8'))
                    except Exception: parsed=payload.decode('utf-8','replace')
                else:
                    try: parsed=payload.decode('utf-8')
                    except Exception: pass
                return {'ok':True,'status':r.status,'headers':dict(r.headers),'data':parsed,'url':url}
        except urllib.error.HTTPError as e:
            payload=e.read()
            try: payload=payload.decode('utf-8','replace')
            except Exception: pass
            return {'ok':False,'status':e.code,'headers':dict(e.headers or {}),'data':payload,'url':url}

    def get(self, config: dict, path: str, **kw): return self.request(config,'GET',path,**kw)
    def post(self, config: dict, path: str, **kw): return self.request(config,'POST',path,**kw)
    def put(self, config: dict, path: str, **kw): return self.request(config,'PUT',path,**kw)
    def delete(self, config: dict, path: str, **kw): return self.request(config,'DELETE',path,**kw)

    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        try:
            r=self.get(config, config.get("probe_path",self.probe_path))
            status=r.get('status',0)
            ok=bool(r.get('ok') or status in (401,403))
            return AdapterResult(ok,self.route,f"HTTP {status}",r)
        except Exception as e:
            return AdapterResult(False,self.route,str(e))

    def fetch_punches(self, identity: DeviceIdentity, config: dict, since: str | None = None) -> list[dict]:
        path=config.get("punch_path") or self.punch_path
        if not path:
            raise NotImplementedError(
                f"Route {self.route} is HTTP-capable, but its attendance/event endpoint is not declared for this device/API version. "
                "Set options.punch_path and field mappings after verifying the vendor API manual."
            )
        method=str(config.get("punch_method") or "GET").upper()
        body=config.get("punch_body")
        if isinstance(body,dict): body=dict(body)
        if since:
            if method == "GET":
                key=config.get("since_param","since")
                sep='&' if '?' in path else '?'
                path=f"{path}{sep}{urllib.parse.quote(str(key))}={urllib.parse.quote(since)}"
            elif config.get("since_body_field"):
                if body is None: body={}
                if not isinstance(body,dict): raise ValueError("punch_body must be an object when since_body_field is used")
                body[str(config.get("since_body_field"))]=since
        result=self.request(config,method,path,body=body,headers=config.get("punch_headers"))
        if not result.get('ok'):
            raise RuntimeError(f"Vendor API returned HTTP {result.get('status')}: {result.get('data')}")
        data=result.get('data')
        rows=self._extract(data, config.get("punch_array_path","punches"))
        if not isinstance(rows,list):
            if isinstance(data,list): rows=data
            else: raise ValueError("Configured punch/event response does not contain an array")
        user_fields=self._field_list(config,"user_fields",["deviceUserId","userId","pin","employeeNoString","employeeNo","personId","cardNo"])
        time_fields=self._field_list(config,"time_fields",["punchedAt","timestamp","dateTime","time","eventTime","date"])
        name_fields=self._field_list(config,"name_fields",["employeeName","name","personName"])
        normalized=[]
        for row in rows:
            if not isinstance(row,dict): continue
            user=self._first(row,user_fields); timestamp=self._first(row,time_fields)
            if user is None or timestamp is None: continue
            normalized.append({
                "deviceUserId": str(user),
                "employeeName": None if (name:=self._first(row,name_fields)) is None else str(name),
                "punchedAt": str(timestamp),
                "rawLine": json.dumps(row,ensure_ascii=False,separators=(',',':')),
            })
        return normalized

    def _field_list(self,config:dict,key:str,defaults:list[str]):
        value=config.get(key)
        if isinstance(value,str): return [x.strip() for x in value.split(',') if x.strip()]
        if isinstance(value,list): return [str(x) for x in value]
        return defaults

    def _first(self,row:dict,fields:list[str]):
        for field in fields:
            value=self._extract(row,field)
            if value is not None and str(value).strip()!='': return value
        return None

    def _extract(self,value,path):
        if path in (None,'','$'): return value
        current=value
        for part in str(path).strip('.').split('.'):
            if isinstance(current,dict): current=current.get(part)
            else: return None
        return current

class NativeSdkAdapter(Adapter):
    env_var = "VENDOR_SDK_PATH"
    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        path=config.get("library_path") or os.getenv(self.env_var,"")
        declared=config.get("sdk_version") or identity.sdk_versions.get(self.route,"")
        if not declared:
            return AdapterResult(False,self.route,"exact SDK version is required before native SDK route can be verified")
        if not path:
            return AdapterResult(False,self.route,f"Set {self.env_var} or library_path; proprietary SDK binaries are not redistributed",{'sdk_version':declared})
        return AdapterResult(os.path.exists(path),self.route,"SDK path/version supplied" if os.path.exists(path) else "SDK path not found",{'path':path,'sdk_version':declared})

class PushAdapter(Adapter):
    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        return AdapterResult(True,self.route,"Server-side push endpoint is available; device firmware capability still requires conformance verification")

class GrpcAdapter(Adapter):
    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        target=config.get("target")
        if not target: return AdapterResult(False,self.route,"target host:port is required")
        host,_,port=target.rpartition(":")
        try:
            with socket.create_connection((host,int(port)),timeout=float(config.get("timeout",5))):
                return AdapterResult(True,self.route,"gRPC TCP target reachable",{"target":target})
        except Exception as e: return AdapterResult(False,self.route,str(e),{"target":target})

class DirectTcpAdapter(Adapter):
    default_port=0
    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        host=config.get("host"); port=int(config.get("port",self.default_port))
        if not host or not port: return AdapterResult(False,self.route,"host and port are required")
        try:
            with socket.create_connection((host,port),timeout=float(config.get("timeout",5))):
                return AdapterResult(True,self.route,"TCP reachable",{"host":host,"port":port})
        except Exception as e: return AdapterResult(False,self.route,str(e),{"host":host,"port":port})

class BusAdapter(Adapter):
    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        return AdapterResult(True,self.route,"Physical reader bus is modeled; integrate at the controller/serial adapter layer")

class FileAdapter(Adapter):
    def probe(self, identity: DeviceIdentity, config: dict) -> AdapterResult:
        path=config.get("path","")
        return AdapterResult(bool(path and os.path.exists(path)),self.route,"Import path available" if path and os.path.exists(path) else "Set an existing export/import path")
