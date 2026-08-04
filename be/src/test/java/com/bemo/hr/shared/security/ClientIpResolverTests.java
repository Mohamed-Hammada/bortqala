package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTests {

    private ClientIpResolver resolver(String... proxies) {
        return new ClientIpResolver(java.util.List.of(proxies));
    }

    @Test
    void ignoresForwardedHeaderFromUntrustedRemoteAddress() {
        ClientIpResolver resolver = resolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.42.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("10.42.0.10");
    }

    @Test
    void returnsRemoteAddressWhenNoForwardedHeaderPresent() {
        ClientIpResolver resolver = resolver("10.42.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.42.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("10.42.0.5");
    }

    @Test
    void trustsForwardedHeaderFromConfiguredProxy() {
        ClientIpResolver resolver = resolver("10.42.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.42.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void takesFirstEntryOfCommaSeparatedForwardedHeader() {
        ClientIpResolver resolver = resolver("10.42.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.42.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.42.0.5, 198.51.100.9");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void trustsRemoteAddressInsideConfiguredCidr() {
        ClientIpResolver resolver = resolver("10.42.0.0/24");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.42.0.77");
        request.addHeader("X-Forwarded-For", "192.0.2.44");

        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.44");
    }

    @Test
    void ignoresForwardedHeaderFromAddressOutsideConfiguredCidr() {
        ClientIpResolver resolver = resolver("10.42.0.0/24");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.42.1.77");
        request.addHeader("X-Forwarded-For", "192.0.2.44");

        assertThat(resolver.resolve(request)).isEqualTo("10.42.1.77");
    }

    @Test
    void rejectsWildcardTrustSoForwardedHeaderIsNeverHonored() {
        ClientIpResolver resolver = resolver("0.0.0.0/0", "::/0");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("X-Forwarded-For", "203.0.113.99");

        assertThat(resolver.resolve(request)).isEqualTo("8.8.8.8");
    }

    @Test
    void cidrMatchingHandlesInvalidInputsSafely() {
        assertThat(ClientIpResolver.IpCidr.matches("10.42.0.5", "10.42.0.0/24")).isTrue();
        assertThat(ClientIpResolver.IpCidr.matches("10.42.1.5", "10.42.0.0/24")).isFalse();
        assertThat(ClientIpResolver.IpCidr.matches("10.42.0.5", "10.42.0.0/33")).isFalse();
        assertThat(ClientIpResolver.IpCidr.matches("999.1.1.1", "10.42.0.0/24")).isFalse();
        assertThat(ClientIpResolver.IpCidr.matches("10.42.0.5", "10.42.0.0/not-a-number")).isFalse();
    }
}
