package com.bemo.hr.shared.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Configuration
public class EpochMillisDateConfiguration {
    @Bean
    JsonMapperBuilderCustomizer epochMillisJsonCustomizer(
            @Value("${hr.company-zone:Africa/Cairo}") String companyZone) {
        ZoneId zoneId = ZoneId.of(companyZone);
        var module = new SimpleModule("hr-epoch-millis-dates");
        module.addSerializer(Instant.class, new InstantEpochSerializer());
        module.addDeserializer(Instant.class, new InstantEpochDeserializer());
        module.addSerializer(LocalDate.class, new LocalDateEpochSerializer(zoneId));
        module.addDeserializer(LocalDate.class, new LocalDateEpochDeserializer(zoneId));
        return builder -> builder.addModule(module);
    }

    private static final class InstantEpochSerializer extends StdScalarSerializer<Instant> {
        private InstantEpochSerializer() { super(Instant.class); }

        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializationContext context)
                throws JacksonException {
            generator.writeNumber(value.toEpochMilli());
        }
    }

    private static final class InstantEpochDeserializer extends StdScalarDeserializer<Instant> {
        private InstantEpochDeserializer() { super(Instant.class); }

        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            if (!parser.isExpectedNumberIntToken()) {
                return context.reportInputMismatch(Instant.class, "Date-time values must be epoch milliseconds.");
            }
            return Instant.ofEpochMilli(parser.getLongValue());
        }
    }

    private static final class LocalDateEpochSerializer extends StdScalarSerializer<LocalDate> {
        private final ZoneId zoneId;
        private LocalDateEpochSerializer(ZoneId zoneId) { super(LocalDate.class); this.zoneId = zoneId; }

        @Override
        public void serialize(LocalDate value, JsonGenerator generator, SerializationContext context)
                throws JacksonException {
            generator.writeNumber(value.atStartOfDay(zoneId).toInstant().toEpochMilli());
        }
    }

    private static final class LocalDateEpochDeserializer extends StdScalarDeserializer<LocalDate> {
        private final ZoneId zoneId;
        private LocalDateEpochDeserializer(ZoneId zoneId) { super(LocalDate.class); this.zoneId = zoneId; }

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            if (!parser.isExpectedNumberIntToken()) {
                return context.reportInputMismatch(LocalDate.class, "Date values must be epoch milliseconds.");
            }
            return Instant.ofEpochMilli(parser.getLongValue()).atZone(zoneId).toLocalDate();
        }
    }
}
