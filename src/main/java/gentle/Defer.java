package gentle;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A scoped defer stack, similar to the <code>defer</code> statement in Go.
 * <p>
 * <code>Defer</code> allows registering cleanup actions that are executed
 * automatically when the scope is exited (via {@link #close()}), typically by
 * using a {@code try}-with-resources block.
 *
 * <p>Deferred actions are executed in <strong>reverse order</strong> of registration,
 * ensuring proper cleanup behavior (LIFO).
 *
 * <p>Example:
 * <pre>{@code
 * try (Defer d = Defer.create()) {
 *     d.defer(() -> System.out.println("cleanup 1"));
 *     d.defer(() -> System.out.println("cleanup 2"));
 *     System.out.println("doing work");
 * }
 *
 * // Output:
 * // doing work
 * // cleanup 2
 * // cleanup 1
 * }</pre>
 *
 * <p><strong>Error Handling:</strong><br>
 * If deferred actions throw exceptions, they are caught and suppressed silently.
 * This ensures that all cleanup actions are attempted.
 */
@NoArgsConstructor (access = AccessLevel.PRIVATE)
public final class Defer implements AutoCloseable {
    /**
     * The stack of deferred actions.
     * Executed in reverse order when {@link #close()} is called.
     */
    private final List<Runnable> stack = new ArrayList<>();

    /**
     * Creates a new, empty defer stack.
     * <p>
     * Intended to be used with {@code try}-with-resources:
     * <pre>{@code
     * try (Defer d = Defer.create()) {
     *     ...
     * }
     * }</pre>
     *
     * @return a new {@link Defer} instance
     */
    public static Defer create() {
        return new Defer();
    }

    /**
     * Registers a new action to be executed when this {@code Defer} is closed.
     * The action will be executed after all previously registered actions.
     *
     * @param r the action to defer
     */
    public void defer(@NonNull Runnable r) {
        stack.add(r);
    }

    /**
     * Executes all deferred actions in reverse order.
     * <p>
     * Any exceptions thrown by deferred actions are caught and ignored, allowing
     * all actions to run regardless of failures.
     */
    @Override
    public void close() {
        for (int i = stack.size() - 1; i >= 0; i--) {
            try {
                stack.get(i).run();
            } catch (Exception _) {}
        }
    }
}
