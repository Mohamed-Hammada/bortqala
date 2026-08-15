package com.bemo.shared.concurrency.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.bemo.shared.concurrency.AsyncContextSwitcher;

/**
 * Scheduled thread pool whose tasks snapshot the scheduling thread's MDC context at submit
 * time and restore it while the task runs. Ensures scheduled jobs (e.g. sync jobs) keep the
 * correlation id that triggered them.
 */
public class ContextCopyingScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    public ContextCopyingScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return new ContextCopyingScheduledFutureTask<>(task, AsyncContextSwitcher.capture());
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return new ContextCopyingScheduledFutureTask<>(task, AsyncContextSwitcher.capture());
    }

    private static final class ContextCopyingScheduledFutureTask<V> implements RunnableScheduledFuture<V> {

        private final RunnableScheduledFuture<V> delegate;
        private final AsyncContextSwitcher.ContextSnapshot context;

        private ContextCopyingScheduledFutureTask(RunnableScheduledFuture<V> delegate,
                                                  AsyncContextSwitcher.ContextSnapshot context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public boolean isPeriodic() {
            return delegate.isPeriodic();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return delegate.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed other) {
            return delegate.compareTo(other);
        }

        @Override
        public void run() {
            try (var ignored = context.enter()) {
                delegate.run();
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }
    }
}
