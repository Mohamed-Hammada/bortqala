import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from app.supplier_loader import resolve, route_inventory
assert resolve('hikvision','DS-K1T341')['preferred_route']=='isapi'
assert resolve('suprema','BioEntry Plus v2')['preferred_route']=='biostar1-legacy-sdk'
assert 'netsdk' in route_inventory('dahua')
print('gateway supplier resolution OK')
