package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigurationSourceTests {
    @Test
    void honorsTheConfiguredCorsAllowlist() {
        var configuration = new SecurityConfig().corsConfigurationSource(
                List.of("https://erp.example.com", "https://erp.example.co"));

        var request = new MockHttpServletRequest("OPTIONS", "/api/v1/users/me");
        var cors = configuration.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns())
                .containsExactlyInAnyOrder("https://erp.example.com", "https://erp.example.co");
        assertThat(cors.checkOrigin("https://erp.example.com")).isEqualTo("https://erp.example.com");
        assertThat(cors.checkOrigin("http://localhost:4200")).isNull();
        assertThat(cors.checkOrigin("https://attacker.example.org")).isNull();
    }
}
