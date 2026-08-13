from .base import GrpcAdapter

class GSdkAdapter(GrpcAdapter):
    route = 'g-sdk'
    kind = 'grpc'
