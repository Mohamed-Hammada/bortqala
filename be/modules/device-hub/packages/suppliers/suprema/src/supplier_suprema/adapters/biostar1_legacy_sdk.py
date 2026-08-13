from .base import NativeSdkAdapter

class Biostar1LegacySdkAdapter(NativeSdkAdapter):
    route = 'biostar1-legacy-sdk'
    kind = 'native-sdk'
    env_var = "SUPREMA_BIOSTAR1_LEGACY_SDK_PATH"
