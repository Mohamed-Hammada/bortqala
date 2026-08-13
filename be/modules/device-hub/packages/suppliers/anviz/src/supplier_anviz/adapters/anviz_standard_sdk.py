from .base import NativeSdkAdapter

class AnvizStandardSdkAdapter(NativeSdkAdapter):
    route = 'anviz-standard-sdk'
    kind = 'native-sdk'
    env_var = "ANVIZ_ANVIZ_STANDARD_SDK_PATH"
