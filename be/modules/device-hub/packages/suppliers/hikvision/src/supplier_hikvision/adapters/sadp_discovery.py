from .base import NativeSdkAdapter

class SadpDiscoveryAdapter(NativeSdkAdapter):
    route = 'sadp-discovery'
    kind = 'native-sdk'
    env_var = "HIKVISION_SADP_DISCOVERY_PATH"
