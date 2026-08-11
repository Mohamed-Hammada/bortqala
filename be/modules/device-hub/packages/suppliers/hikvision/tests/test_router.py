import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]/"src"))
from supplier_hikvision import DeviceIdentity, select_routes

d = select_routes(DeviceIdentity(model="UNKNOWN"))
assert d.supplier == 'hikvision'
assert len(d.candidates) > 0
# SDK candidates must never be silently approved without an exact SDK version.
for c in d.candidates:
    if c.kind == "sdk":
        assert c.status in ("NEEDS_SDK_VERSION","NEEDS_VENDOR_MATRIX","INCOMPATIBLE","COMPATIBLE")
print(d)
