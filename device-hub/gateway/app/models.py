from __future__ import annotations
from datetime import datetime
from enum import Enum
from typing import Any
from pydantic import BaseModel, Field

class Vendor(str, Enum):
    zkteco='zkteco'; hikvision='hikvision'; dahua='dahua'; suprema='suprema'; virdi='virdi'; anviz='anviz'; honeywell='honeywell'

class DeviceCreate(BaseModel):
    name: str
    vendor: Vendor
    model: str = 'UNKNOWN'
    firmware: str = ''
    platform_version: str = ''
    server_version: str = ''
    os_name: str = ''
    architecture: str = ''
    sdk_versions: dict[str,str] = Field(default_factory=dict)
    api_versions: dict[str,str] = Field(default_factory=dict)
    capability_hints: list[str] = Field(default_factory=list)
    host: str | None = None
    port: int | None = None
    route: str | None = None
    username: str | None = None
    password: str | None = None
    base_url: str | None = None
    options: dict[str, Any] = Field(default_factory=dict)

class Device(DeviceCreate):
    id: str
    created_at: datetime

class RouteCandidateModel(BaseModel):
    route: str
    kind: str
    status: str
    reason: str
    sdk_version_spec: str = '*'
    api_version_spec: str = '*'
    server_version_spec: str = '*'
    implementation_status: str = 'scaffold'
    official_documentation: list[str] = Field(default_factory=list)

class RouteResolution(BaseModel):
    supplier: Vendor
    model_pattern: str
    generation_or_version: str
    preferred_route: str | None = None
    compatible_routes: list[str] = Field(default_factory=list)
    candidates: list[RouteCandidateModel] = Field(default_factory=list)
    notes: str = ''
    official_documentation: list[str] = Field(default_factory=list)

class SupplierInfo(BaseModel):
    supplier: Vendor
    routes: list[str]
    documentation_file: str
