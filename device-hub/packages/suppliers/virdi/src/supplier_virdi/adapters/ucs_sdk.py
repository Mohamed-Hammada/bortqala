from .base import NativeSdkAdapter

class UcsSdkAdapter(NativeSdkAdapter):
    route = 'ucs-sdk'
    kind = 'native-sdk'
    env_var = "VIRDI_UCS_SDK_PATH"
