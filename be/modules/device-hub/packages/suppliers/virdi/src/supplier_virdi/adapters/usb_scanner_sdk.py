from .base import NativeSdkAdapter

class UsbScannerSdkAdapter(NativeSdkAdapter):
    route = 'usb-scanner-sdk'
    kind = 'native-sdk'
    env_var = "VIRDI_USB_SCANNER_SDK_PATH"
