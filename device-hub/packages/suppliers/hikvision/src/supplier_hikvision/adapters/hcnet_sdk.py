from .base import NativeSdkAdapter

class HcnetSdkAdapter(NativeSdkAdapter):
    route = 'hcnet-sdk'
    kind = 'native-sdk'
    env_var = "HIKVISION_HCNET_SDK_PATH"
