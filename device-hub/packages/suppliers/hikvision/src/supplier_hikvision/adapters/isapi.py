from .base import HttpAdapter

class IsapiAdapter(HttpAdapter):
    route = 'isapi'
    kind = 'http-rest'
    probe_path = "/ISAPI/System/deviceInfo"
