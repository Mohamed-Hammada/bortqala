package com.bemo.hr.finance.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FrankfurterExchangeRateClient {

    private final RestClient restClient;

    public FrankfurterExchangeRateClient(
            @Value("${hr.exchange-rate.frankfurter-base-url:https://api.frankfurter.dev}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Set<String> supportedCurrencies() {
        try {
            CurrencyRow[] rows = restClient.get()
                    .uri("/v2/currencies")
                    .retrieve()
                    .body(CurrencyRow[].class);
            if (rows == null) return Collections.emptySet();
            return Arrays.stream(rows)
                    .map(CurrencyRow::isoCode)
                    .filter(code -> code != null && !code.isBlank())
                    .map(code -> code.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (RestClientException ex) {
            throw new FrankfurterClientException("Unable to load Frankfurter supported currencies", ex);
        }
    }

    public List<RateRow> latestRates(String base, List<String> quotes) {
        if (quotes == null || quotes.isEmpty()) return List.of();

        String normalizedBase = base.toUpperCase(Locale.ROOT);
        String normalizedQuotes = quotes.stream()
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(","));

        try {
            RateRow[] rows = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/rates")
                            .queryParam("base", normalizedBase)
                            .queryParam("quotes", normalizedQuotes)
                            .build())
                    .retrieve()
                    .body(RateRow[].class);
            return rows == null ? List.of() : List.of(rows);
        } catch (RestClientException ex) {
            throw new FrankfurterClientException("Unable to load Frankfurter exchange rates", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrencyRow(@JsonProperty("iso_code") String isoCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RateRow(LocalDate date, String base, String quote, BigDecimal rate) {}

    public static final class FrankfurterClientException extends RuntimeException {
        public FrankfurterClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
