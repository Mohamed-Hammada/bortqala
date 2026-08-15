package com.bemo.shared.http;

/**
 * Micrometer metric names used by the framework's filters and aspects.
 */
public final class MetricTypes {

    public static final String API_REQUEST_COUNTER = "api_request_counter_total";
    public static final String API_REQUEST_DURATION = "api_request_duration_seconds";
    public static final String DB_QUERY_COUNTER = "db_query_counter_total";
    public static final String CACHE_HIT_COUNTER = "cache_hit_counter_total";
    public static final String CACHE_MISS_COUNTER = "cache_miss_counter_total";
    public static final String HTTP_CLIENT_REQUEST_COUNTER = "http_client_request_counter_total";
    public static final String HTTP_CLIENT_REQUEST_DURATION = "http_client_request_duration_seconds";

    private MetricTypes() {
    }
}
