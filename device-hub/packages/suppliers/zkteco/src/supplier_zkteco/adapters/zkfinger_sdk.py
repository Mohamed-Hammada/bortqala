from .base import NativeSdkAdapter

class ZkfingerSdkAdapter(NativeSdkAdapter):
    route = 'zkfinger-sdk'
    kind = 'native-sdk'
    env_var = "ZKTECO_ZKFINGER_SDK_PATH"
