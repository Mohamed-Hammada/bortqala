package com.bemo.hr.attendance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "biometric_device_integrations")
public class BiometricDeviceIntegration {
    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false, length = 64)
    private String appId;

    @Column(name = "biometric_device_id", nullable = false, length = 36)
    private String biometricDeviceId;

    @Column(name = "hub_device_id", nullable = false, length = 64)
    private String hubDeviceId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 40)
    private String vendor;

    @Column(nullable = false, length = 160)
    private String model;

    @Column(name = "serial_number", length = 180)
    private String serialNumber;

    @Column(name = "firmware_version", length = 160)
    private String firmwareVersion;

    @Column(name = "platform_version", length = 160)
    private String platformVersion;

    @Column(name = "server_version", length = 160)
    private String serverVersion;

    @Column(name = "os_name", length = 80)
    private String osName;

    @Column(length = 80)
    private String architecture;

    @Column(name = "sdk_versions_json", columnDefinition = "TEXT")
    private String sdkVersionsJson;

    @Column(name = "api_versions_json", columnDefinition = "TEXT")
    private String apiVersionsJson;

    @Column(name = "capability_hints_json", columnDefinition = "TEXT")
    private String capabilityHintsJson;

    @Column(length = 255)
    private String host;

    private Integer port;

    @Column(name = "base_url", length = 1000)
    private String baseUrl;

    @Column(nullable = false, length = 120)
    private String route;

    @Column(name = "route_status", nullable = false, length = 40)
    private String routeStatus;

    @Column(name = "route_kind", nullable = false, length = 40)
    private String routeKind;

    @Column(name = "implementation_status", nullable = false, length = 80)
    private String implementationStatus;

    @Column(name = "official_documentation_json", columnDefinition = "TEXT")
    private String officialDocumentationJson;

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "last_probe_status", length = 40)
    private String lastProbeStatus;

    @Column(name = "last_probe_message", length = 1000)
    private String lastProbeMessage;

    @Column(name = "last_probe_at")
    private Instant lastProbeAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BiometricDeviceIntegration() {
    }

    public BiometricDeviceIntegration(String biometricDeviceId, String hubDeviceId) {
        this.id = UUID.randomUUID().toString();
        this.biometricDeviceId = biometricDeviceId;
        this.hubDeviceId = hubDeviceId;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String cleanRequired(String value) {
        return value == null ? "" : value.strip();
    }

    public void updateConfiguration(
            String name,
            String vendor,
            String model,
            String serialNumber,
            String firmwareVersion,
            String platformVersion,
            String serverVersion,
            String osName,
            String architecture,
            String sdkVersionsJson,
            String apiVersionsJson,
            String capabilityHintsJson,
            String host,
            Integer port,
            String baseUrl,
            String route,
            String routeStatus,
            String routeKind,
            String implementationStatus,
            String officialDocumentationJson,
            String optionsJson) {
        this.name = cleanRequired(name);
        this.vendor = cleanRequired(vendor).toLowerCase();
        this.model = cleanRequired(model);
        this.serialNumber = clean(serialNumber);
        this.firmwareVersion = clean(firmwareVersion);
        this.platformVersion = clean(platformVersion);
        this.serverVersion = clean(serverVersion);
        this.osName = clean(osName);
        this.architecture = clean(architecture);
        this.sdkVersionsJson = sdkVersionsJson;
        this.apiVersionsJson = apiVersionsJson;
        this.capabilityHintsJson = capabilityHintsJson;
        this.host = clean(host);
        this.port = port;
        this.baseUrl = clean(baseUrl);
        this.route = cleanRequired(route);
        this.routeStatus = cleanRequired(routeStatus);
        this.routeKind = cleanRequired(routeKind);
        this.implementationStatus = cleanRequired(implementationStatus);
        this.officialDocumentationJson = officialDocumentationJson;
        this.optionsJson = optionsJson;
    }

    public void probeResult(boolean ok, String message) {
        this.lastProbeStatus = ok ? "SUCCESS" : "FAILED";
        this.lastProbeMessage = message == null ? null : message.substring(0, Math.min(1000, message.length()));
        this.lastProbeAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getBiometricDeviceId() {
        return biometricDeviceId;
    }

    public String getHubDeviceId() {
        return hubDeviceId;
    }

    public String getName() {
        return name;
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getOsName() {
        return osName;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getSdkVersionsJson() {
        return sdkVersionsJson;
    }

    public String getApiVersionsJson() {
        return apiVersionsJson;
    }

    public String getCapabilityHintsJson() {
        return capabilityHintsJson;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getRoute() {
        return route;
    }

    public String getRouteStatus() {
        return routeStatus;
    }

    public String getRouteKind() {
        return routeKind;
    }

    public String getImplementationStatus() {
        return implementationStatus;
    }

    public String getOfficialDocumentationJson() {
        return officialDocumentationJson;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public String getLastProbeStatus() {
        return lastProbeStatus;
    }

    public String getLastProbeMessage() {
        return lastProbeMessage;
    }

    public Instant getLastProbeAt() {
        return lastProbeAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
