from .base import NativeSdkAdapter

class ProwatchHsdkAdapter(NativeSdkAdapter):
    route = 'prowatch-hsdk'
    kind = 'native-sdk'
    env_var = "HONEYWELL_PROWATCH_HSDK_PATH"
