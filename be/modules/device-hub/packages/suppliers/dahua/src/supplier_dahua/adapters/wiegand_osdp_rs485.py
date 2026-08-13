from .base import BusAdapter

class WiegandOsdpRs485Adapter(BusAdapter):
    route = 'wiegand-osdp-rs485'
    kind = 'bus'
