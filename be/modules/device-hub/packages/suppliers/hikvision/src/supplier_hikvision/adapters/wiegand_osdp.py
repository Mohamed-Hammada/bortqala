from .base import BusAdapter

class WiegandOsdpAdapter(BusAdapter):
    route = 'wiegand-osdp'
    kind = 'bus'
