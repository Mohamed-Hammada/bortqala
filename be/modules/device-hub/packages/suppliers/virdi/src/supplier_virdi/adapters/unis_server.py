from .base import NativeSdkAdapter

class UnisServerAdapter(NativeSdkAdapter):
    route = 'unis-server'
    kind = 'platform-native'
    env_var = "VIRDI_UNIS_SERVER_PATH"
