from .base import NativeSdkAdapter

class PlcommproPullAdapter(NativeSdkAdapter):
    route = 'plcommpro-pull'
    kind = 'native-sdk'
    env_var = "ZKTECO_PLCOMMPRO_PULL_PATH"
