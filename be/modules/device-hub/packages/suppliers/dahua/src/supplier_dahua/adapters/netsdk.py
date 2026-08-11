from .base import NativeSdkAdapter

class NetsdkAdapter(NativeSdkAdapter):
    route = 'netsdk'
    kind = 'native-sdk'
    env_var = "DAHUA_NETSDK_PATH"
