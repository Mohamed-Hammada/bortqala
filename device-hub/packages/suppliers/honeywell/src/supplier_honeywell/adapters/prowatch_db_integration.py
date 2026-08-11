from .base import NativeSdkAdapter

class ProwatchDbIntegrationAdapter(NativeSdkAdapter):
    route = 'prowatch-db-integration'
    kind = 'platform-native'
    env_var = "HONEYWELL_PROWATCH_DB_INTEGRATION_PATH"
