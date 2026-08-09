package org.example.zkteco.gateway.compatibility;

import org.example.zkteco.core.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/compatibility")
public class CompatibilityController {
    private final AdapterRegistry registry;

    public CompatibilityController(AdapterRegistry registry) { this.registry = registry; }

    @GetMapping("/protocols")
    List<ProtocolView> protocols() {
        return registry.all().stream().map(DeviceAdapter::protocol)
                .map(protocol -> new ProtocolView(protocol, status(protocol))).toList();
    }

    @GetMapping("/capabilities")
    List<DeviceCapability> capabilities() { return Arrays.asList(DeviceCapability.values()); }

    private String status(DeviceProtocol protocol) {
        return switch (protocol) {
            case ZK_PULL -> "IMPLEMENTED_CORE";
            case ADMS_PUSH -> "IMPLEMENTED_INGRESS";
            case ZKBIO_TIME_API, ZKBIO_CVSECURITY_API, ZKBIO_CVACCESS_API, WDMS_API,
                 ZKBIO_TIME_CLOUD_API, ZKBIO_ZLINK_API -> "CONFIGURABLE_PLATFORM_CONNECTOR";
            case WINDOWS_SDK_BRIDGE, PLCOMM_PRO_SDK, ZKFINGER_SCANNER -> "VENDOR_RUNTIME_REQUIRED";
            default -> "PROFILE_OR_EXTERNAL_BRIDGE";
        };
    }

    public record ProtocolView(DeviceProtocol protocol, String status) {}
}
