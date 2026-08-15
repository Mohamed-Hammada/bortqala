package com.bemo.shared.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.bemo.shared.concurrency.util.ContextCopyingScheduledThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Factory for executor instances with context-propagation and consistent, named threads.
 * Daemon threads are used for fire-and-forget pools so they never block JVM shutdown.
 */
public final class ExecutorsFactory {

    private ExecutorsFactory() {
    }

    public static ThreadPoolTaskExecutor newThreadPoolTaskExecutor(String name, int core, int max, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(name + "-");
        executor.setTaskDecorator(new ContextCopyTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    public static ExecutorService newExecutorService(String name, int poolSize) {
        return Executors.newFixedThreadPool(poolSize, daemonThreadFactory(name));
    }

    /**
     * Scheduled pool whose tasks inherit the MDC context of the thread that scheduled them.
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPool(String name, int poolSize) {
        return new ContextCopyingScheduledThreadPoolExecutor(poolSize, daemonThreadFactory(name));
    }

    public static ThreadFactory daemonThreadFactory(String name) {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
