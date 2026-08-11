from .base import HttpAdapter

class OnvifDiscoveryAdapter(HttpAdapter):
    route = 'onvif-discovery'
    kind = 'http-rest'
