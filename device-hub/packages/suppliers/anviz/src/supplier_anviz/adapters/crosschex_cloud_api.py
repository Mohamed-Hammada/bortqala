from .base import HttpAdapter

class CrosschexCloudApiAdapter(HttpAdapter):
    route = 'crosschex-cloud-api'
    kind = 'cloud-http'
