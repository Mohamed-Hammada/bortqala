from .base import HttpAdapter

class WdmsApiAdapter(HttpAdapter):
    route = 'wdms-api'
    kind = 'platform-http'
