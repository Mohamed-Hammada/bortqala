from .base import NativeSdkAdapter

class StandaloneSdkAdapter(NativeSdkAdapter):
    route = 'standalone-sdk'
    kind = 'native-sdk'
    env_var = "ZKTECO_STANDALONE_SDK_PATH"
