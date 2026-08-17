package com.bemo.hr.shared.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EpochMillisDateConfigurationTests {
    private final JsonMapper jsonMapper;

    @Autowired
    EpochMillisDateConfigurationTests(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Test
    void serializesAndReadsAllApiDatesAsEpochMilliseconds() throws Exception {
        var value = new DateContract(LocalDate.of(2026, 7, 24), Instant.parse("2026-07-24T12:30:00Z"));

        String json = jsonMapper.writeValueAsString(value);
        var restored = jsonMapper.readValue(json, DateContract.class);

        assertThat(json).doesNotContain("2026-07-24").contains("1784896200000");
        assertThat(restored).isEqualTo(value);
    }

    private record DateContract(LocalDate workDate, Instant occurredAt) {
    }
}
