from __future__ import annotations
import json, os, threading
from datetime import datetime, timezone
from pathlib import Path
from uuid import uuid4
from .models import Device, DeviceCreate

class DeviceRegistry:
    def __init__(self):
        self._items: dict[str, Device] = {}
        self._lock=threading.RLock()
        self._path=Path(os.getenv('DEVICE_HUB_REGISTRY_PATH','data/devices.json'))
        self._load()

    def _load(self):
        try:
            if not self._path.exists(): return
            rows=json.loads(self._path.read_text(encoding='utf-8'))
            for row in rows:
                item=Device(**row)
                self._items[item.id]=item
        except Exception as exc:
            raise RuntimeError(f'Cannot load device hub registry {self._path}: {exc}') from exc

    def _persist(self):
        self._path.parent.mkdir(parents=True,exist_ok=True)
        tmp=self._path.with_suffix('.tmp')
        rows=[x.model_dump(mode='json',exclude={'password'}) for x in self._items.values()]
        tmp.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
        tmp.replace(self._path)

    def list(self):
        with self._lock: return list(self._items.values())

    def get(self, device_id: str) -> Device:
        with self._lock:
            if device_id not in self._items: raise KeyError(device_id)
            return self._items[device_id]

    def add(self, req: DeviceCreate) -> Device:
        with self._lock:
            sanitized=req.model_copy(update={'password':None})
            item=Device(id=str(uuid4()),created_at=datetime.now(timezone.utc),**sanitized.model_dump())
            self._items[item.id]=item; self._persist(); return item

    def update(self, device_id: str, req: DeviceCreate) -> Device:
        with self._lock:
            current=self.get(device_id)
            sanitized=req.model_copy(update={'password':None})
            item=Device(id=current.id,created_at=current.created_at,**sanitized.model_dump())
            self._items[item.id]=item; self._persist(); return item

    def delete(self, device_id: str):
        with self._lock:
            item=self.get(device_id); del self._items[device_id]; self._persist(); return item

registry=DeviceRegistry()
