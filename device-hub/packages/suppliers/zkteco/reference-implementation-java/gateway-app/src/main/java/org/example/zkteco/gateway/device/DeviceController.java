package org.example.zkteco.gateway.device;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.example.zkteco.core.DeviceEndpoint;
import org.example.zkteco.core.DeviceProbeResult;
import org.example.zkteco.core.DeviceProtocol;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceRegistryService service;

    public DeviceController(DeviceRegistryService service) {
        this.service = service;
    }

    @GetMapping
    List<DeviceEndpoint> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    DeviceEndpoint get(@PathVariable UUID id) {
        return service.require(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DeviceEndpoint create(@Valid @RequestBody CreateDeviceRequest request) {
        return service.save(new DeviceEndpoint(
                UUID.randomUUID(),
                request.label(),
                request.preferredProtocol(),
                request.host(),
                request.port(),
                request.baseUri(),
                request.secretRef(),
                request.properties()));
    }

    @PostMapping("/{id}/probe")
    DeviceProbeResult probe(@PathVariable UUID id) {
        return service.probe(id);
    }

    public record CreateDeviceRequest(
            @NotBlank String label,
            DeviceProtocol preferredProtocol,
            String host,
            Integer port,
            URI baseUri,
            String secretRef,
            Map<String, String> properties
    ) {}
}
