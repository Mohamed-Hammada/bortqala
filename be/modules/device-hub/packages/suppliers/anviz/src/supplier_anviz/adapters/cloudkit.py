from .base import NativeSdkAdapter

class CloudkitAdapter(NativeSdkAdapter):
    route = 'cloudkit'
    kind = 'native-sdk'
    env_var = "ANVIZ_CLOUDKIT_PATH"
