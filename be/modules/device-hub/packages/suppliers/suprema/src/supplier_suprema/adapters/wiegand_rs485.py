from .base import BusAdapter

class WiegandRs485Adapter(BusAdapter):
    route = 'wiegand-rs485'
    kind = 'bus'
