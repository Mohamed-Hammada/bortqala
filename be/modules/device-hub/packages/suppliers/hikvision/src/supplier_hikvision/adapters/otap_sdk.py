from .base import NativeSdkAdapter

class OtapSdkAdapter(NativeSdkAdapter):
    route = 'otap-sdk'
    kind = 'native-sdk'
    env_var = "HIKVISION_OTAP_SDK_PATH"
