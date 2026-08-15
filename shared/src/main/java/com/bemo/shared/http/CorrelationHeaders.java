package com.bemo.shared.http;

/**
 * Canonical correlation/tenant header names propagated across the HTTP boundary.
 * Apps should configure CORS/allowed-headers to echo these back to clients.
 */
public final class CorrelationHeaders {

    public static final String CORRELATION_ID = "web-correlation-id";
    public static final String TRANSACTION_ID = "transaction-id";
    public static final String EXTERNAL_ID = "x-external-id";
    public static final String USER_ID = "x-user-id";
    public static final String TENANT_ID = "x-tenant-id";
    public static final String BRANCH_ID = "x-branch-id";
    public static final String CUSTOMER_ID = "x-customer-id";
    public static final String REQUEST_SOURCE = "x-request-source";

    private CorrelationHeaders() {
    }
}
