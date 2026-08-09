package org.example.zkteco.core;

/** Integration route, not a marketing device family. */
public enum DeviceProtocol {
    AUTO,
    ZK_PULL,              // legacy/new PULL TCP/UDP device protocol / Standalone SDK family
    ADMS_PUSH,            // TA PUSH / iClock ADMS style device initiated HTTP
    AC_PUSH,              // access-control PUSH variant
    ZKBIO_TIME_API,
    ZKBIO_CVSECURITY_API,
    ZKBIO_CVACCESS_API,
    WDMS_API,
    ZKBIO_TIME_CLOUD_API,
    ZKBIO_ZLINK_API,
    WINDOWS_SDK_BRIDGE,   // zkemkeeper / Standalone SDK COM bridge
    PLCOMM_PRO_SDK,       // access-panel PULL SDK (plcommpro) bridge
    ZKFINGER_SCANNER,     // ZKFinger Windows/Linux/Android SDK family
    VISIBLE_LIGHT_SDK,    // visible-light / biometric module SDK integration point
    WIEGAND_READER,       // reader behind a controller; not normally an IP API
    RS485_READER,
    USB_IMPORT_EXPORT
}
