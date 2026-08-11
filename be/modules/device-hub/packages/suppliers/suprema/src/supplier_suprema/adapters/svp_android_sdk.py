from .base import NativeSdkAdapter

class SvpAndroidSdkAdapter(NativeSdkAdapter):
    route = 'svp-android-sdk'
    kind = 'native-sdk'
    env_var = "SUPREMA_SVP_ANDROID_SDK_PATH"
