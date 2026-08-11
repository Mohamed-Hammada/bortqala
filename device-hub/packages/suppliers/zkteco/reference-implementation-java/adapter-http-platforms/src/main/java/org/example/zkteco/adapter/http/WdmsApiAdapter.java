package org.example.zkteco.adapter.http;

import org.example.zkteco.core.DeviceProtocol;

public final class WdmsApiAdapter extends AbstractHttpPlatformAdapter {
    public WdmsApiAdapter() {
        super(DeviceProtocol.WDMS_API);
    }
}
