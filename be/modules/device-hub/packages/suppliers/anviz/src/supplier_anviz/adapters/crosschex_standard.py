from .base import NativeSdkAdapter

class CrosschexStandardAdapter(NativeSdkAdapter):
    route = 'crosschex-standard'
    kind = 'platform-native'
    env_var = "ANVIZ_CROSSCHEX_STANDARD_PATH"
