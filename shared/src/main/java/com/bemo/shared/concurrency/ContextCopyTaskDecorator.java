package com.bemo.shared.concurrency;

import org.springframework.core.task.TaskDecorator;

/**
 * Spring {@link TaskDecorator} that propagates MDC/locale context to executor threads.
 * Assign to any {@code ThreadPoolTaskExecutor} via {@code setTaskDecorator(...)}.
 */
public class ContextCopyTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return AsyncContextSwitcher.contextRunnable(runnable);
    }
}
