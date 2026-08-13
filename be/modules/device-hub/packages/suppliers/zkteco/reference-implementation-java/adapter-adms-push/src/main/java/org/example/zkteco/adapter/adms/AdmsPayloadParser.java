package org.example.zkteco.adapter.adms;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdmsPayloadParser {
    private static final DateTimeFormatter[] FMTS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private AdmsPayloadParser() {}

    public static List<AdmsParsedRecord> parse(String table, String body) {
        if (body == null || body.isBlank()) return List.of();
        List<AdmsParsedRecord> out = new ArrayList<>();
        for (String line : body.replace("\r", "").split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Map<String, String> kv = parseKv(line);
            String kind = (table == null || table.isBlank() ? kv.getOrDefault("table", "ATTLOG") : table)
                    .toUpperCase(Locale.ROOT);
            if (!kv.isEmpty()) {
                String pin = first(kv, "PIN", "pin", "UserID", "userid");
                String dt = first(kv, "DateTime", "datetime", "TimeSecond", "time");
                out.add(new AdmsParsedRecord(kind, pin, parseTime(dt),
                        parseInt(first(kv, "Status", "status")),
                        parseInt(first(kv, "Verify", "verify")), kv, line));
            } else {
                String[] tokens = line.split("\\t");
                if (tokens.length < 2) tokens = line.split(",");
                Map<String, String> fields = new LinkedHashMap<>();
                for (int i = 0; i < tokens.length; i++) fields.put("c" + i, tokens[i]);
                out.add(new AdmsParsedRecord(kind,
                        tokens.length > 0 ? tokens[0] : null,
                        tokens.length > 1 ? parseTime(tokens[1]) : null,
                        tokens.length > 2 ? parseInt(tokens[2]) : null,
                        tokens.length > 3 ? parseInt(tokens[3]) : null,
                        fields, line));
            }
        }
        return List.copyOf(out);
    }

    static Map<String, String> parseKv(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String token : line.split("\\t")) {
            int i = token.indexOf('=');
            if (i > 0) map.put(token.substring(0, i).trim(), token.substring(i + 1).trim());
        }
        return map;
    }

    static LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter fmt : FMTS) {
            try { return LocalDateTime.parse(value.trim(), fmt); }
            catch (Exception ignored) {}
        }
        return null;
    }

    static Integer parseInt(String value) {
        try { return value == null ? null : Integer.valueOf(value.trim()); }
        catch (Exception ignored) { return null; }
    }

    static String first(Map<String, String> map, String... keys) {
        for (String key : keys) if (map.containsKey(key)) return map.get(key);
        return null;
    }
}
