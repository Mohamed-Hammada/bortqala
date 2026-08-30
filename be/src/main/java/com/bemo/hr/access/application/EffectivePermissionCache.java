package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessPolicyApi.UserEffectivePermissionsResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request-scoped in-memory cache for user effective permissions during SpEL evaluation.
 */
@Component
public class EffectivePermissionCache {

    private static final String REQUEST_ATTR_KEY = "bemo.effective_permissions.cache";

    public UserEffectivePermissionsResponse get(String username) {
        Map<String, UserEffectivePermissionsResponse> cache = getRequestCache(false);
        if (cache == null || username == null) {
            return null;
        }
        return cache.get(username);
    }

    public void put(String username, UserEffectivePermissionsResponse response) {
        if (username == null || response == null) {
            return;
        }
        Map<String, UserEffectivePermissionsResponse> cache = getRequestCache(true);
        if (cache != null) {
            cache.put(username, response);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, UserEffectivePermissionsResponse> getRequestCache(boolean create) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Map<String, UserEffectivePermissionsResponse> cache =
                (Map<String, UserEffectivePermissionsResponse>) attributes.getAttribute(REQUEST_ATTR_KEY, RequestAttributes.SCOPE_REQUEST);
        if (cache == null && create) {
            cache = new ConcurrentHashMap<>();
            attributes.setAttribute(REQUEST_ATTR_KEY, cache, RequestAttributes.SCOPE_REQUEST);
        }
        return cache;
    }
}
