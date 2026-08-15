package com.bemo.shared.http.support;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Wraps a servlet request, buffering the body once so downstream filters and controllers can
 * read it repeatedly (input streams and readers) without consuming it. Also snapshots the
 * header map for log-safe inspection.
 */
public class MultipleReadRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;
    private final Map<String, String> headers = new HashMap<>();

    public MultipleReadRequestWrapper(HttpServletRequest request) {
        super(request);
        this.body = readBody(request);
        copyHeaders(request);
    }

    private static byte[] readBody(HttpServletRequest request) {
        try (var in = request.getInputStream()) {
            return in == null ? new byte[0] : in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to buffer request body", e);
        }
    }

    private void copyHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return;
        }
        names.asIterator().forEachRemaining(name -> headers.put(name, request.getHeader(name)));
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public DelegatingServletInputStream getInputStream() {
        return new DelegatingServletInputStream(new ByteArrayInputStream(body));
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getHeadersSnapshot() {
        return headers;
    }
}
