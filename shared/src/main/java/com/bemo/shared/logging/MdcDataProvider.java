package com.bemo.shared.logging;

import org.slf4j.MDC;

/**
 * Typed accessors for the MDC keys the tracing filter populates. Keeps key spellings in one
 * place so callers never depend on raw MDC strings.
 */
public final class MdcDataProvider {

    public static final String REQUEST_ID = "requestId";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String EXTERNAL_ID = "externalId";
    public static final String USER_ID = "userId";
    public static final String ROLES = "roles";
    public static final String TENANT_ID = "tenantId";
    public static final String BRANCH_ID = "branchId";
    public static final String CUSTOMER_ID = "customerId";

    private MdcDataProvider() {
    }

    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    public static String getTransactionId() {
        return MDC.get(TRANSACTION_ID);
    }

    public static String getExternalId() {
        return MDC.get(EXTERNAL_ID);
    }

    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    public static String getRoles() {
        return MDC.get(ROLES);
    }

    public static String getTenantId() {
        return MDC.get(TENANT_ID);
    }

    public static String getBranchId() {
        return MDC.get(BRANCH_ID);
    }

    public static String getCustomerId() {
        return MDC.get(CUSTOMER_ID);
    }
}
