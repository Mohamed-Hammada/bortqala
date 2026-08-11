package org.example.zkteco.adapter.http;

import org.example.zkteco.core.DeviceProtocol;

public final class ZkBioTimeApiAdapter extends AbstractHttpPlatformAdapter {
    public ZkBioTimeApiAdapter() {
        super(DeviceProtocol.ZKBIO_TIME_API);
    }
}
