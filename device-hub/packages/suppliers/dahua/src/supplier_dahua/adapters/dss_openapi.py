from .base import HttpAdapter

class DssOpenapiAdapter(HttpAdapter):
    route = 'dss-openapi'
    kind = 'platform-http'
