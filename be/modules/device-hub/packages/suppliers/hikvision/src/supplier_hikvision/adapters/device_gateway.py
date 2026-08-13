from .base import HttpAdapter

class DeviceGatewayAdapter(HttpAdapter):
    route = 'device-gateway'
    kind = 'platform-http'
