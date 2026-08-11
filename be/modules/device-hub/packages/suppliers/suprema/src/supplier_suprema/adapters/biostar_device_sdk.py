from .base import NativeSdkAdapter

class BiostarDeviceSdkAdapter(NativeSdkAdapter):
    route = 'biostar-device-sdk'
    kind = 'native-sdk'
    env_var = "SUPREMA_BIOSTAR_DEVICE_SDK_PATH"
