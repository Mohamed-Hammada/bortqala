package com.bemo.shared.concurrency.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Request wrapper that keeps attribute mutations in its own map, decoupling them from the
 * underlying request. Used by async flows where the original request may already have been
 * released back to the container.
 */
public class AttributesHolderServletRequest extends HttpServletRequestWrapper {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public AttributesHolderServletRequest(HttpServletRequest request) {
        super(request);
        Enumeration<String> names = request.getAttributeNames();
        if (names != null) {
            names.asIterator().forEachRemaining(name -> attributes.put(name, request.getAttribute(name)));
        }
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }
}
