from .base import NativeSdkAdapter

class IsupSdkAdapter(NativeSdkAdapter):
    route = 'isup-sdk'
    kind = 'native-sdk'
    env_var = "HIKVISION_ISUP_SDK_PATH"
