from .base import PushAdapter

class AcPushAdapter(PushAdapter):
    route = 'ac-push'
    kind = 'push-http'
