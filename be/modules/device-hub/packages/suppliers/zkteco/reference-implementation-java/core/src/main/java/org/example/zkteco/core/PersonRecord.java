package org.example.zkteco.core;

import java.util.Map;

public record PersonRecord(
        String externalId,
        String displayName,
        String cardNumber,
        String privilege,
        Map<String, String> attributes
) {
    public PersonRecord {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
