package gentle;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.lang.Error;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A scoped stack of deferred cleanup actions, similar to Go's {@code defer}.
 * <p>
 * {@code Defer} allows you to register cleanup callbacks during execution,
 * which are automatically invoked when the scope ends. Deferred actions
 * run in <strong>LIFO</strong> (last-in-first-out) order.
 *
 * <p>Typical usage:
 * <pre>{@code
 * try (Defer d = Defer.create()) {
 *     d.defer(() -> System.out.println("cleanup A"));
 *     d.defer(() -> System.out.println("cleanup B"));
 *     System.out.println("work");
 * }
 *
 * // Output:
 * // work
 * // cleanup B
 * // cleanup A
 * }</pre>
 *
 * <p><strong>Exception Handling:</strong><br>
 * If multiple deferred actions throw exceptions:
 * <ul>
 *   <li>The first thrown exception is rethrown</li>
 *   <li>Additional exceptions are attached using {@link Throwable#addSuppressed(Throwable)}</li>
 * </ul>
 *
 * <p>This makes {@code Defer} safe for resource cleanup, multi-step teardown,
 * and transactional compensation.
 */
@NoArgsConstructor (access = AccessLevel.PRIVATE)
public final class Defer implements AutoCloseable {

    @FunctionalInterface
    private interface Cleanup {
        void run() throws Exception;
    }

    /**
     * Internal LIFO stack of cleanup actions.
     */
    private final List<Cleanup> stack = new ArrayList<>();

    /**
     * Creates a new, initially empty {@code Defer} stack.
     *
     * @return a new {@code Defer} instance
     */
    public static Defer create() {
        return new Defer();
    }

    /**
     * Executes a scoped block with automatic cleanup.
     * <p>
     * Equivalent to:
     * <pre>{@code
     * try (Defer d = Defer.create()) {
     *     body.accept(d);
     * }
     * }</pre>
     *
     * @param body the scoped operation to execute
     * @throws Exception if either the body or cleanup throws
     */
    public static void scope(@NonNull Consumer<Defer> body) throws Exception {
        try (Defer d = create()) {
            body.accept(d);
        }
    }

    /**
     * Registers an arbitrary action to be executed when this {@code Defer} is closed.
     * <p>
     * The action is executed after all previously registered actions.
     *
     * @param r the action to defer
     * @return this {@code Defer}, enabling chained calls
     */
    public Defer defer(@NonNull Runnable r) {
        synchronized (stack) {
            stack.add(r::run);
        }
        return this;
    }

    /**
     * Registers a closeable resource which will be closed when this {@code Defer} is closed.
     *
     * @param c the resource to close
     * @return this {@code Defer}, enabling chained calls
     */
    public Defer defer(@NonNull AutoCloseable c) {
        synchronized (stack) {
            stack.add(c::close);
        }
        return this;
    }

    /**
     * Registers a {@link CompletableFuture} to be awaited on scope exit.
     * Its result value is ignored; errors propagate into cleanup.
     *
     * @param future the future to complete before leaving the scope
     * @return this {@code Defer}, enabling chained calls
     */
    public Defer defer(@NonNull CompletableFuture<?> future) {
        synchronized (stack) {
            stack.add(future::get);
        }
        return this;
    }

    /**
     * Returns the number of deferred cleanup actions currently stored.
     *
     * @return number of registered deferred actions
     */
    public int size() {
        synchronized (stack) {
            return stack.size();
        }
    }

    /**
     * Executes deferred actions in <strong>reverse</strong> order.
     * <p>
     * Errors follow a predictable structured-error model:
     * <ul>
     *   <li>If no errors occur, nothing is thrown.</li>
     *   <li>If one error occurs, it is thrown.</li>
     *   <li>If multiple errors occur, the first is thrown and the rest are suppressed.</li>
     * </ul>
     *
     * @throws Exception the primary exception thrown during cleanup
     */
    @Override
    public void close() throws Exception {
        Throwable primary = null;

        synchronized (stack) {
            for (int i = stack.size() - 1; i >= 0; i--) {
                try {
                    stack.get(i).run();
                } catch (Throwable t) {
                    if (primary == null) {
                        primary = t;
                        continue;
                    }
                    primary.addSuppressed(t);
                }
            }
        }
        if (primary instanceof Exception e) throw e;
        if (primary instanceof Error err) throw err;
        if (primary == null) return;
        throw new RuntimeException(primary);
    }

    @Override
    public String toString() {
        return "Defer[size=" + size() + "]";
    }
}
