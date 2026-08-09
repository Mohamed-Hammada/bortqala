from .base import HttpAdapter

class ProwatchWebServicesAdapter(HttpAdapter):
    route = 'prowatch-web-services'
    kind = 'platform-http'
