package gentle;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Represents a lazily-initialized value.
 * <p>
 * A {@code Lazy} wraps a {@link Supplier} that is only executed once — the first
 * time {@link #get()} is called. The computed value is cached and returned
 * for all subsequent calls.
 *
 * <p>This is useful for:
 * <ul>
 *     <li>Expensive computations that should be deferred until needed</li>
 *     <li>Implementing on-demand initialization</li>
 *     <li>Avoiding unnecessary work in object construction</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * Lazy<Config> config = Lazy.of(() -> loadConfigFromDisk());
 *
 * // The configuration will only be loaded here, the first time get() is called:
 * Config c = config.get();
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong><br>
 * This implementation is thread-safe. The supplier may be executed more than once
 * in race conditions, but the stored value is guaranteed to be the final one set.
 *
 * @param <T> the type of the lazily-computed value
 */
@RequiredArgsConstructor (access = AccessLevel.PRIVATE)
public final class Lazy<T> {

    /**
     * Internal marker object indicating that no value has been computed yet.
     */
    private static final Object UNSET = new Object();

    @NonNull
    private final Supplier<T> supplier;

    /**
     * Stores either {@link #UNSET} or the computed value.
     */
    private final AtomicReference<Object> ref = new AtomicReference<>(UNSET);

    /**
     * Creates a new lazy value backed by the given supplier.
     * <p>
     * The supplier is <em>not</em> executed until {@link #get()} is called.
     *
     * @param supplier the computation to defer
     * @param <T>      the type of the eventual value
     * @return a new {@code Lazy} instance
     */
    public static <T> Lazy<T> of(@NonNull Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    /**
     * Returns the computed value, computing it on first access.
     * <p>
     * If multiple threads call {@link #get()} simultaneously, the supplier
     * may be invoked multiple times, but the first stored result is the
     * one consistently returned afterward.
     *
     * @return the value, computing it lazily if needed
     */
    @SuppressWarnings("unchecked")
    public T get() {
        var v = ref.get();
        if (v != UNSET) return (T) v;
        T computed = supplier.get();
        ref.compareAndSet(UNSET, computed);
        return computed;
    }
}
