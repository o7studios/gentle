package gentle;

import lombok.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Context provides a mechanism to carry request-scoped values, deadlines, and
 * cancellation signals across different threads or asynchronous tasks.
 *
 * <p>This is inspired by the Go `context.Context` pattern. Contexts are hierarchical:
 * a child context can inherit values and cancellation from a parent context.
 *
 * <p>Contexts are typically used to:
 * <ul>
 *     <li>Pass metadata or configuration to asynchronous tasks.</li>
 *     <li>Propagate cancellation signals across threads.</li>
 *     <li>Set deadlines for task execution.</li>
 * </ul>
 */
public final class Context {

    /** A shared scheduler for scheduling deadline-based cancellations. */
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Context parent;
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> cancelled = new CompletableFuture<>();
    private final Instant deadline;

    /**
     * Creates a new context with a parent and optional deadline.
     *
     * @param parent   the parent context
     * @param deadline the deadline at which this context should be automatically cancelled
     */
    public Context(@NonNull Context parent, Instant deadline) {
        this.parent = parent;
        this.deadline = deadline;
    }

    /**
     * Creates a root context without a parent or deadline.
     */
    public Context() {
        this.parent = null;
        this.deadline = null;
    }

    /**
     * Adds a key-value pair to the context.
     *
     * @param key   the key
     * @param value the value
     * @param <T>   type of the value
     * @return this context
     */
    public <T> Context withValue(@NonNull String key, @NonNull T value) {
        data.put(key, value);
        return this;
    }

    /**
     * Creates a new child context with the specified deadline.
     *
     * @param deadline the deadline
     * @return a new context with this as parent
     */
    public Context withDeadline(@NonNull Instant deadline) {
        return new Context(this, deadline);
    }

    /**
     * Creates a cancellable child context.
     * If the parent context is cancelled, the child will also be cancelled.
     *
     * @return a new cancellable child context
     */
    public Context withCancel() {
        var child = new Context(this, deadline);
        this.cancelled.whenComplete((_, _) -> child.cancel());
        return child;
    }

    /**
     * Creates a child context inheriting values and optionally cancellation.
     *
     * @return a new child context
     */
    public Context fork() {
        return new Context(this, deadline);
    }

    /**
     * Creates a child context inheriting values and optionally cancellation.
     *
     * @param inheritCancel if true, child will be cancelled when parent is cancelled
     * @return a new child context
     */
    public Context fork(boolean inheritCancel) {
        var child = new Context(this, deadline);
        if (inheritCancel && this.cancelled != null) {
            this.cancelled.whenComplete((_, _) -> child.cancel());
        }
        return child;
    }

    /**
     * Retrieves a value from the context by key, searching parent contexts if necessary.
     *
     * @param key the key to look up
     * @param <T> the expected type of the value
     * @return an {@link Optional} containing the value if found, otherwise empty
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NonNull String key) {
        var value = data.get(key);
        if (value != null) return Optional.maybe((T) value);
        if (parent != null) return parent.get(key);
        return Optional.empty();
    }

    /**
     * Retrieves a value from the context or throws if it is missing.
     *
     * @param key the key to look up
     * @param <T> the expected type of the value
     * @return the value
     * @throws IllegalStateException if the key is missing
     */
    @SuppressWarnings("unchecked")
    public <T> T require(@NonNull String key) {
        return (T) get(key).orElseThrow(() ->
                new IllegalStateException("Missing context key: " + key));
    }

    /**
     * Returns whether this context or any parent context has been cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled.isDone() || (parent != null && parent.isCancelled());
    }

    /**
     * Cancels this context and triggers cancellation for any children that inherit it.
     */
    public void cancel() {
        cancelled.complete(null);
    }

    /**
     * Schedules automatic cancellation when the deadline is reached.
     * Should be called after creating a context with a deadline.
     */
    public void scheduleDeadline() {
        if (deadline == null) return;
        long delay = Instant.now().until(deadline, ChronoUnit.MILLIS);
        if (delay <= 0) cancel();
        else scheduler.schedule(this::cancel, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns whether this context has passed its deadline.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return deadline != null && Instant.now().isAfter(deadline);
    }

    /**
     * Returns a {@link Result} representing an error if this context is cancelled or expired.
     *
     * @param errorSupplier the supplier to create the error
     * @param <T>           the type of the expected result
     * @param <E>           the type of the error
     * @return a {@link Optional} containing the error Result if cancelled/expired, otherwise empty
     */
    public <T, E extends Error> Optional<Result<T, E>> failIfCancelled(@NonNull Supplier<E> errorSupplier) {
        if (isCancelled() || isExpired()) return Optional.some(Result.err(errorSupplier.get()));
        return Optional.empty();
    }
}
