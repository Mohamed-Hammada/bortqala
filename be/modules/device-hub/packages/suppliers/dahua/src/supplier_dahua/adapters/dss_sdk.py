from .base import NativeSdkAdapter

class DssSdkAdapter(NativeSdkAdapter):
    route = 'dss-sdk'
    kind = 'native-sdk'
    env_var = "DAHUA_DSS_SDK_PATH"
