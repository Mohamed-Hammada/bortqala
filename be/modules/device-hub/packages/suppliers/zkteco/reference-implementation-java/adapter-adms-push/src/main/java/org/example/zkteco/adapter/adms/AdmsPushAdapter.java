package org.example.zkteco.adapter.adms;

import org.example.zkteco.core.DeviceAdapter;
import org.example.zkteco.core.DeviceEndpoint;
import org.example.zkteco.core.DeviceProbeResult;
import org.example.zkteco.core.DeviceProtocol;

public final class AdmsPushAdapter implements DeviceAdapter {
    @Override
    public DeviceProtocol protocol() {
        return DeviceProtocol.ADMS_PUSH;
    }

    @Override
    public boolean supports(DeviceEndpoint endpoint) {
        return endpoint.preferredProtocol() == DeviceProtocol.ADMS_PUSH;
    }

    @Override
    public DeviceProbeResult probe(DeviceEndpoint endpoint) {
        return DeviceProbeResult.offline(protocol(),
                "ADMS/PUSH is device-initiated. Confirm support from the latest inbound heartbeat instead of an outbound probe.");
    }
}
