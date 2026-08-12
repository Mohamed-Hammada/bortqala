package com.bemo.hr.shared.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system/about")
public class SystemAboutController {

    @Value("${spring.application.name:BEMO ERP}")
    private String applicationName;

    public record SystemAboutResponse(
            String productName,
            String applicationName,
            String version,
            String buildNumber,
            String gitCommit,
            String buildTime,
            String apiVersion,
            String environment,
            boolean supportEnabled
    ) {}

    @GetMapping
    public SystemAboutResponse getSystemAbout() {
        return new SystemAboutResponse(
                "BEMO ERP",
                applicationName,
                "1.8.7",
                "20260812.1",
                "9fb98f80",
                Instant.now().toString(),
                "v1",
                "PRODUCTION",
                true
        );
    }
}
