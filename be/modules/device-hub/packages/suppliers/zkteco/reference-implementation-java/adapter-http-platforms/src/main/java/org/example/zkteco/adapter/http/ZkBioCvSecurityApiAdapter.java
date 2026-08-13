package org.example.zkteco.adapter.http;

import org.example.zkteco.core.DeviceProtocol;

public final class ZkBioCvSecurityApiAdapter extends AbstractHttpPlatformAdapter {
    public ZkBioCvSecurityApiAdapter() {
        super(DeviceProtocol.ZKBIO_CVSECURITY_API);
    }
}
