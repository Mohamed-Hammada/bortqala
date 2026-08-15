package com.bemo.shared.concurrency;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Captures the current thread's diagnostic context (SLF4J MDC + locale) so it can be restored
 * inside an asynchronous task that runs on another thread. This is what keeps correlation ids
 * attached to log lines emitted by worker threads.
 */
public final class AsyncContextSwitcher {

    public AsyncContextSwitcher() {
    }

    public static ContextSnapshot capture() {
        return new ContextSnapshot(
                MDC.getCopyOfContextMap(),
                LocaleContextHolder.getLocaleContext());
    }

    /** Wraps a {@link Runnable} so it runs with the context captured at decoration time. */
    public static Runnable contextRunnable(Runnable task) {
        return contextRunnable(capture(), task);
    }

    public static Runnable contextRunnable(ContextSnapshot snapshot, Runnable task) {
        return () -> {
            try (Scope scope = snapshot.enter()) {
                task.run();
            }
        };
    }

    /**
     * A frozen view of the caller thread's diagnostic context.
     */
    public record ContextSnapshot(Map<String, String> mdc, LocaleContext localeContext) {

        static ContextSnapshot capture() {
            return AsyncContextSwitcher.capture();
        }

        /** Restores the captured context on the current thread; restores the previous state on close. */
        public Scope enter() {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            LocaleContext previousLocale = LocaleContextHolder.getLocaleContext();
            if (mdc != null && !mdc.isEmpty()) {
                MDC.setContextMap(new HashMap<>(mdc));
            } else {
                MDC.clear();
            }
            if (localeContext != null) {
                LocaleContextHolder.setLocaleContext(localeContext);
            }
            return new Scope(previousMdc, previousLocale);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Map<String, String> previousMdc;
        private final LocaleContext previousLocale;
        private boolean closed;

        private Scope(Map<String, String> previousMdc, LocaleContext previousLocale) {
            this.previousMdc = previousMdc;
            this.previousLocale = previousLocale;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previousMdc != null && !previousMdc.isEmpty()) {
                MDC.setContextMap(new HashMap<>(previousMdc));
            } else {
                MDC.clear();
            }
            if (previousLocale != null) {
                LocaleContextHolder.setLocaleContext(previousLocale);
            } else {
                LocaleContextHolder.resetLocaleContext();
            }
        }
    }
}
