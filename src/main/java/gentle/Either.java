package gentle;

import lombok.NonNull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a value that can be one of two possible types: a {@code Left} or a {@code Right}.
 * <p>
 * This is commonly used to model computations that may return either a "success" or an "alternative"
 * result without implying failure semantics (unlike {@link gentle.Result}). Typical usage patterns
 * include:
 * <ul>
 *     <li>Representing two possible branches of logic</li>
 *     <li>Returning one of two distinct types without using inheritance</li>
 *     <li>Expressing conversions or parsing where either a parsed value or a fallback is meaningful</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * Either<String, Integer> parseInt(String s) {
 *     try {
 *         return Either.right(Integer.parseInt(s));
 *     } catch (NumberFormatException e) {
 *         return Either.left("Not a number: " + s);
 *     }
 * }
 *
 * int value = parseInt("10").mapRight(n -> n * 2).fold(
 *     err -> 0,
 *     ok -> ok
 * );
 * }</pre>
 *
 * @param <L> the type of the {@code Left} value, typically considered the "alternative" branch
 * @param <R> the type of the {@code Right} value, typically considered the "primary" branch
 */
public sealed interface Either<L, R> permits Either.Left, Either.Right {

    /**
     * @return {@code true} if this is a {@link Left}, otherwise {@code false}
     */
    default boolean isLeft() {
        return this instanceof Left<L, R>;
    }

    /**
     * @return {@code true} if this is a {@link Right}, otherwise {@code false}
     */
    default boolean isRight() {
        return this instanceof Right<L, R>;
    }

    /**
     * Transforms the {@code Left} value using the given function, leaving {@code Right} unchanged.
     *
     * @param f   the mapping function
     * @param <U> the new type of the {@code Left} value
     * @return a new {@code Either} with the mapped {@code Left}, or the same {@code Right}
     */
    default <U> Either<U, R> mapLeft(@NonNull Function<? super L, ? extends U> f) {
        return switch (this) {
            case Left<L, R> v -> left(f.apply(v.value));
            case Right<L, R> v -> right(v.value);
        };
    }

    /**
     * Transforms the {@code Right} value using the given function, leaving {@code Left} unchanged.
     *
     * @param f   the mapping function
     * @param <U> the new type of the {@code Right} value
     * @return a new {@code Either} with the mapped {@code Right}, or the same {@code Left}
     */
    default <U> Either<L, U> mapRight(@NonNull Function<? super R, ? extends U> f) {
        return switch (this) {
            case Left<L, R> v -> left(v.value);
            case Right<L, R> v -> right(f.apply(v.value));
        };
    }

    /**
     * Applies a function to the {@code Right} value that itself returns an {@code Either}.
     * <p>
     * This allows chaining of computations that may produce one of two outcomes.
     *
     * @param f   the function to apply
     * @param <U> the type of the new {@code Right} value
     * @return the resulting {@code Either} from the mapping function, or the same {@code Left}
     */
    default <U> Either<L, U> flatMap(@NonNull Function<? super R, ? extends Either<L, U>> f) {
        return switch (this) {
            case Left<L, R> v -> left(v.value);
            case Right<L, R> v -> f.apply(v.value);
        };
    }

    /**
     * Processes both possible values by applying one of two provided functions.
     *
     * @param onLeft  function applied if this is {@code Left}
     * @param onRight function applied if this is {@code Right}
     * @param <T>     the return type of the result
     * @return the result of applying the appropriate function
     */
    default <T> T fold(@NonNull Function<? super L, ? extends T> onLeft,
                       @NonNull Function<? super R, ? extends T> onRight) {
        return switch (this) {
            case Left<L, R> v -> onLeft.apply(v.value);
            case Right<L, R> v -> onRight.apply(v.value);
        };
    }

    /**
     * Executes an action if this is {@code Left}, leaving the value unchanged.
     *
     * @param action action to apply to the left value
     * @return this {@code Either}, for fluent chaining
     */
    default Either<L, R> peekLeft(@NonNull Consumer<? super L> action) {
        if (this instanceof Left<L, R>(L value)) action.accept(value);
        return this;
    }

    /**
     * Executes an action if this is {@code Right}, leaving the value unchanged.
     *
     * @param action action to apply to the right value
     * @return this {@code Either}, for fluent chaining
     */
    default Either<L, R> peekRight(@NonNull Consumer<? super R> action) {
        if (this instanceof Right<L, R>(R value)) action.accept(value);
        return this;
    }

    /**
     * Creates a {@code Left} value.
     */
    static <L, R> Either<L, R> left(@NonNull L value) {
        return new Left<>(value);
    }

    /**
     * Creates a {@code Right} value.
     */
    static <L, R> Either<L, R> right(@NonNull R value) {
        return new Right<>(value);
    }

    /**
     * Represents the {@code Left} variant of {@code Either}.
     */
    record Left<L, R>(@NonNull L value) implements Either<L, R> {}

    /**
     * Represents the {@code Right} variant of {@code Either}.
     */
    record Right<L, R>(@NonNull R value) implements Either<L, R> {}
}
