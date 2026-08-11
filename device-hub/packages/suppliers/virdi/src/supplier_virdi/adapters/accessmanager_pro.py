from .base import NativeSdkAdapter

class AccessmanagerProAdapter(NativeSdkAdapter):
    route = 'accessmanager-pro'
    kind = 'platform-native'
    env_var = "VIRDI_ACCESSMANAGER_PRO_PATH"
