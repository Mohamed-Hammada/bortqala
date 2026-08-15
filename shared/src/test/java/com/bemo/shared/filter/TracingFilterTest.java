package com.bemo.shared.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bemo.shared.logging.MdcDataProvider;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TracingFilterTest {

    @Test
    void assignsCorrelationIdAndEchoesItInTheResponse() throws Exception {
        TracingFilter filter = new TracingFilter(true, "web-correlation-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        java.util.concurrent.atomic.AtomicReference<String> mdcSeen = new java.util.concurrent.atomic.AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcSeen.set(MDC.get(MdcDataProvider.REQUEST_ID)));

        String correlationId = response.getHeader("web-correlation-id");
        assertNotNull(correlationId);
        assertTrue(!correlationId.isBlank());
        assertEquals(correlationId, mdcSeen.get());
    }

    @Test
    void honoursInboundCorrelationId() throws Exception {
        TracingFilter filter = new TracingFilter(true, "web-correlation-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("web-correlation-id", "inbound-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        java.util.concurrent.atomic.AtomicReference<String> mdcSeen = new java.util.concurrent.atomic.AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcSeen.set(MDC.get(MdcDataProvider.REQUEST_ID)));

        assertEquals("inbound-123", response.getHeader("web-correlation-id"));
        assertEquals("inbound-123", mdcSeen.get());
    }

    @Test
    void clearsMdcAfterTheRequest() throws Exception {
        TracingFilter filter = new TracingFilter(true, "web-correlation-id");
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(MDC.get(MdcDataProvider.REQUEST_ID));
    }

    @Test
    void disabledFilterSkipsTracing() throws Exception {
        TracingFilter filter = new TracingFilter(false, "web-correlation-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader("web-correlation-id"));
        assertNull(MDC.get(MdcDataProvider.REQUEST_ID));
    }
}
