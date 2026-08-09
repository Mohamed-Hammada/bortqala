from .base import NativeSdkAdapter

class UsbSdkAdapter(NativeSdkAdapter):
    route = 'usb-sdk'
    kind = 'native-sdk'
    env_var = "HIKVISION_USB_SDK_PATH"
