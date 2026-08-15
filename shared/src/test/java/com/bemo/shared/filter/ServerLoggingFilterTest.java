package com.bemo.shared.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ServerLoggingFilterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(ServerLoggingFilter.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void logsRequestAndResponseLines() throws Exception {
        ServerLoggingFilter filter = new ServerLoggingFilter(true, List.of("Authorization"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/orders");
        request.setContentType("application/json");
        request.setContent("{\"email\":\"a@b.com\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Api-Key", "supersecret");
        request.addHeader("Authorization", "Bearer abc");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        List<ILoggingEvent> events = appender.list;
        assertTrue(events.size() >= 2, "expected request + response log lines");
        String requestLine = events.get(0).getFormattedMessage();
        assertTrue(requestLine.contains("\"type\":\"request\""));
        assertTrue(requestLine.contains("\"method\":\"POST\""));
        assertTrue(requestLine.contains("\"uri\":\"/api/v1/orders\""));
        assertTrue(requestLine.contains("\"body\":\"{\\\"email\\\":\\\"a@b.com\\\"}\""));
        assertTrue(requestLine.contains("X-Api-Key\":\"********\""), "secret header must be masked");
        assertTrue(!requestLine.contains("Authorization"), "excluded header must not be logged");
        String responseLine = events.get(1).getFormattedMessage();
        assertTrue(responseLine.contains("\"type\":\"response\""));
        assertTrue(responseLine.contains("\"status\":\"200\""));
        assertTrue(responseLine.contains("\"durationInMillis\""));
    }

    @Test
    void disabledFilterEmitsNothing() throws Exception {
        ServerLoggingFilter filter = new ServerLoggingFilter(false, List.of());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/orders");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals(0, appender.list.size());
    }

    @Test
    void multipartBodiesAreNotReplayedButRequestIsStillLogged() throws Exception {
        ServerLoggingFilter filter = new ServerLoggingFilter(true, List.of());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/upload");
        request.setContentType("multipart/form-data");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        List<ILoggingEvent> events = appender.list;
        assertTrue(events.size() >= 1);
        assertTrue(events.get(0).getFormattedMessage().contains("\"type\":\"request\""));
        assertTrue(events.get(0).getFormattedMessage().contains("\"uri\":\"/api/v1/upload\""));
    }
}
