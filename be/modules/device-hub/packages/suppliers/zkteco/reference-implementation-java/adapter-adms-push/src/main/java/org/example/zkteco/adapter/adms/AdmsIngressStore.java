package org.example.zkteco.adapter.adms;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public final class AdmsIngressStore {
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentLinkedDeque<AdmsEnvelope> events = new ConcurrentLinkedDeque<>();
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private final int maximumEvents;

    public AdmsIngressStore(int maximumEvents) {
        if (maximumEvents < 1) {
            throw new IllegalArgumentException("maximumEvents must be positive");
        }
        this.maximumEvents = maximumEvents;
    }

    public AdmsEnvelope append(String serialNumber, String path, String method,
                               Map<String, String> query, String contentType, String body) {
        String safeSerial = serialNumber == null || serialNumber.isBlank() ? "unknown" : serialNumber;
        String safeBody = body == null ? "" : body;
        AdmsEnvelope envelope = new AdmsEnvelope(sequence.incrementAndGet(), safeSerial, path, method,
                query, contentType, safeBody, Instant.now());
        events.addFirst(envelope);
        lastSeen.put(safeSerial, envelope.receivedAt());
        while (events.size() > maximumEvents) {
            events.pollLast();
        }
        return envelope;
    }

    public List<AdmsEnvelope> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, maximumEvents));
        List<AdmsEnvelope> result = new ArrayList<>(safeLimit);
        for (AdmsEnvelope event : events) {
            if (result.size() >= safeLimit) break;
            result.add(event);
        }
        return List.copyOf(result);
    }

    public Map<String, Instant> lastSeen() {
        java.util.LinkedHashMap<String, Instant> ordered = new java.util.LinkedHashMap<>();
        lastSeen.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return java.util.Collections.unmodifiableMap(ordered);
    }
}
