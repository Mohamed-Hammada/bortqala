package com.bemo.shared.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import com.bemo.shared.logging.MdcDataProvider;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcFilterTest {

    @Test
    void propagatesCorrelationHeadersIntoMdc() throws Exception {
        MdcFilter filter = new MdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("transaction-id", "txn-1");
        request.addHeader("x-tenant-id", "tenant-a");
        request.addHeader("x-user-id", "user-1");

        AtomicReference<String> tenantSeen = new AtomicReference<>();
        AtomicReference<String> userSeen = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            tenantSeen.set(MDC.get(MdcDataProvider.TENANT_ID));
            userSeen.set(MDC.get(MdcDataProvider.USER_ID));
        });

        assertEquals("tenant-a", tenantSeen.get());
        assertEquals("user-1", userSeen.get());
    }

    @Test
    void clearsMdcAfterTheRequest() throws Exception {
        MdcFilter filter = new MdcFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-tenant-id", "tenant-a");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChainAdapter());

        assertNull(MDC.get(MdcDataProvider.TENANT_ID));
        assertNull(MDC.get(MdcDataProvider.USER_ID));
    }

    private static final class MockFilterChainAdapter implements jakarta.servlet.FilterChain {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
        }
    }
}
