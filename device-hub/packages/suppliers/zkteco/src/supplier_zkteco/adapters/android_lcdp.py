from .base import NativeSdkAdapter

class AndroidLcdpAdapter(NativeSdkAdapter):
    route = 'android-lcdp'
    kind = 'native-sdk'
    env_var = "ZKTECO_ANDROID_LCDP_PATH"
