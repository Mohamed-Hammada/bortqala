from .base import HttpAdapter

class ZkbioTimeApiAdapter(HttpAdapter):
    route = 'zkbio-time-api'
    kind = 'platform-http'
