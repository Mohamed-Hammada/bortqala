package com.bemo.hr.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientIpResolver {

    private static final List<String> FORBIDDEN_TRUSTS = List.of("0.0.0.0/0", "::/0");

    private final List<String> trustedProxies;

    public ClientIpResolver(@Value("${hr.security.trusted-proxies:}") List<String> trustedProxies) {
        this.trustedProxies = trustedProxies == null
                ? List.of()
                : trustedProxies.stream()
                        .filter(candidate -> candidate != null && !candidate.isBlank())
                        .filter(candidate -> !FORBIDDEN_TRUSTS.contains(candidate.trim()))
                        .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) return false;
        for (String candidate : trustedProxies) {
            if (candidate.indexOf('/') > 0) {
                if (IpCidr.matches(ip, candidate)) return true;
            } else if (candidate.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    static final class IpCidr {
        private IpCidr() {
        }

        static boolean matches(String ip, String cidr) {
            int slash = cidr.indexOf('/');
            String network = cidr.substring(0, slash);
            int prefix;
            try {
                prefix = Integer.parseInt(cidr.substring(slash + 1));
            } catch (NumberFormatException exception) {
                return false;
            }
            long address = toLong(ip);
            long base = toLong(network);
            if (address < 0 || base < 0 || prefix < 0 || prefix > 32) return false;
            long mask = prefix == 0 ? 0 : ~0L << (32 - prefix);
            return (address & mask) == (base & mask);
        }

        private static long toLong(String ip) {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return -1;
            try {
                long value = 0;
                for (String part : parts) {
                    int octet = Integer.parseInt(part);
                    if (octet < 0 || octet > 255) return -1;
                    value = (value << 8) | octet;
                }
                return value;
            } catch (NumberFormatException exception) {
                return -1;
            }
        }
    }
}
