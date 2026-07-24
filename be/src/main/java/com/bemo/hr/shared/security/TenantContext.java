package com.bemo.hr.shared.security;

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_APP = new ThreadLocal<>();

    private TenantContext() { }

    public static void set(String appId) { CURRENT_APP.set(appId); }
    public static String currentOrSystem() { return CURRENT_APP.get() == null ? "SYSTEM" : CURRENT_APP.get(); }
    public static String require() {
        String appId = CURRENT_APP.get();
        if (appId == null) throw new IllegalStateException("No SaaS app is bound to the current request.");
        return appId;
    }
    public static void clear() { CURRENT_APP.remove(); }
}
