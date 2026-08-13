from .base import PushAdapter

class CrosschexCloudWebhookAdapter(PushAdapter):
    route = 'crosschex-cloud-webhook'
    kind = 'push-http'
