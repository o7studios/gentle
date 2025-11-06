package gentle.async;

import gentle.Result;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A structured concurrency scope for managing asynchronous tasks.
 * <p>
 * The {@code Scope} allows creating and managing multiple asynchronous {@link Task tasks} in a single
 * logical scope. All tasks are automatically cancelled when the scope is closed.
 *
 * <p>Typical usage:
 * <pre>{@code
 * try (Scope scope = Scope.open()) {
 *     Task<String> t1 = scope.async(() -> "Hello");
 *     Task<Integer> t2 = scope.async(() -> 42);
 *
 *     System.out.println(t1.await());
 *     System.out.println(t2.await());
 * }
 * }</pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Scope implements AutoCloseable {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Utils.cores);

    /** ExecutorService used for running tasks in this scope. */
    private final ExecutorService executor;

    /** List of tasks created in this scope. */
    private final List<Task<?>> tasks = new ArrayList<>();

    /** Indicates whether this scope has been closed. */
    private volatile boolean closed = false;

    /**
     * Opens a new scope with a cached thread pool.
     *
     * @return a new {@code Scope}
     */
    public static Scope open() {
        return new Scope(Executors.newCachedThreadPool());
    }

    /**
     * Opens a new scope using a custom {@link ExecutorService}.
     *
     * @param executor the executor to run tasks
     * @return a new {@code Scope}
     */
    public static Scope open(@NonNull ExecutorService executor) {
        return new Scope(executor);
    }

    /**
     * Submits a new asynchronous {@link Task} to this scope.
     * <p>
     * The task is automatically cancelled when the scope is closed.
     *
     * @param supplier the task to execute
     * @param <T>      the type of the task result
     * @return a {@link Task} representing the asynchronous computation
     * @throws IllegalStateException if the scope is already closed
     */
    public synchronized <T> Task<T> async(@NonNull Callable<T> supplier) {
        if (closed) throw new IllegalStateException("Scope already closed");
        Task<T> task = new Task<>(executor.submit(supplier));
        tasks.add(task);
        return task;
    }

    /**
     * Submits a new asynchronous {@link Task} to this scope that will start after a specified delay.
     * <p>
     * The task is scheduled using a shared {@link ScheduledExecutorService}. When the delay elapses,
     * the {@code supplier} is executed and the result is completed in the returned {@link Task}.
     * The task is automatically cancelled if the scope is closed before it executes.
     *
     * <p>Example usage:
     * <pre>{@code
     * try (Scope scope = Scope.open()) {
     *     Task<String> delayedTask = scope.delayed(Duration.ofSeconds(2), () -> "Hello after 2s");
     *     System.out.println(delayedTask.await());
     * }
     * }</pre>
     *
     * @param delay    the delay after which the task should execute
     * @param supplier the task to execute after the delay
     * @param <T>      the type of the task result
     * @return a {@link Task} representing the delayed asynchronous computation
     * @throws IllegalStateException if the scope has already been closed
     */
    public synchronized  <T> Task<T> delayed(@NonNull Duration delay, @NonNull Callable<T> supplier) {
        if (closed) throw new IllegalStateException("Scope already closed");

        CompletableFuture<T> future = new CompletableFuture<>();
        ScheduledFuture<?> scheduled = scheduler.schedule(() -> {
            try {
                future.complete(supplier.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);

        Task<T> task = new Task<>(future) {
            @Override
            public void cancel() {
                scheduled.cancel(true);
                super.cancel();
            }
        };

        tasks.add(task);
        return task;
    }

    /**
     * Returns the number of tasks currently tracked by this scope.
     *
     * @return number of tasks
     */
    public synchronized int size() {
        return tasks.size();
    }

    /**
     * Closes the scope.
     * <p>
     * Cancels all tasks and shuts down the executor. If multiple tasks throw exceptions during
     * cancellation, the first exception is rethrown and subsequent exceptions are suppressed.
     *
     * @throws Exception if any task cancellation or shutdown fails
     */
    @Override
    public void close() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        synchronized (this) {
            closed = true;
            for (var t : tasks) {
                try {
                    t.cancel();
                } catch (Throwable ex) {
                    errors.add(ex);
                }
            }
            executor.shutdownNow();
        }
        if (errors.isEmpty()) return;
        var primary = errors.getFirst();
        for (int i = 1; i < errors.size(); i++) {
            primary.addSuppressed(errors.get(i));
        }
        if (primary instanceof Exception e) throw e;
        if (primary instanceof Error err) throw err;
        throw new RuntimeException(primary);
    }

    @Override
    public String toString() {
        return "Scope[size=" + size() + ", closed=" + closed + "]";
    }
}
