package co.phoenixlab.discord.util;

import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TryingScheduledExecutor implements ScheduledExecutorService {

    private final ScheduledExecutorService service;
    private final Logger logger;

    public TryingScheduledExecutor(ScheduledExecutorService service, Logger logger) {
        this.service = service;
        this.logger = logger;
    }

    private Runnable wrap(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.warn("Exception while running runnable. Job will be canceled!", e);
                throw new RuntimeException(e);
            }
        };
    }

    private <V> Callable<V> wrap(Callable<V> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                logger.warn("Exception while running callable. Job will be canceled!", e);
                throw new RuntimeException(e);
            }
        };
    }


    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return service.schedule(wrap(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return service.schedule(wrap(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return service.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return service.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        logger.info("Service is shutting down", new Exception());
        service.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        logger.info("Service is shutting down NOW", new Exception());
        return service.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return service.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return service.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return service.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return service.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return service.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return service.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return service.invokeAll(
            tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList())
        );
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return service.invokeAll(
            tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList()),
            timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return service.invokeAny(
            tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList())
        );
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return service.invokeAny(
            tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList()),
            timeout, unit
        );
    }

    @Override
    public void execute(Runnable command) {
        service.execute(wrap(command));
    }
}
