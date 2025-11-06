package gentle;

import lombok.NonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Represents the outcome of an operation that can either succeed with a value of type {@code T}
 * or fail with an error of type {@code E}.
 * <p>
 * This is inspired by the Result type from functional programming languages. It allows handling
 * success and failure in a type-safe way without using exceptions for control flow.
 *
 * <p>Typical usage:
 * <pre>{@code
 * Result<Integer, MyError> divide(int a, int b) {
 *     if (b == 0) return Result.err(MyError.DIVIDE_BY_ZERO);
 *     return Result.ok(a / b);
 * }
 *
 * Result<Integer, MyError> r = divide(10, 2);
 * int value = r.orElse(0);  // returns 5
 * }</pre>
 *
 * @param <T> the type of the success value
 * @param <E> the type of the error, must implement {@link Error}
 */
public sealed interface Result<T, E extends Error> permits Result.Ok, Result.Err {

    /**
     * Converts this {@code Result} to an {@link Optional}.
     * <p>
     * Returns an {@link Optional} containing the value if this {@code Result} is {@link Ok},
     * or {@link Optional#empty()} if it is {@link Err}.
     *
     * @return an {@code Optional} representing the success value
     */
    default Optional<T> asOptional() {
        return switch (this) {
            case Result.Err<T, E> _ -> Optional.empty();
            case Result.Ok<T, E> v -> Optional.of(v.value);
        };
    }

    /**
     * Returns whether this {@code Result} represents an error.
     * <p>
     * Equivalent to checking {@code instanceOf Err}.
     *
     * @return {@code true} if this is an {@link Err}, otherwise {@code false}
     */
    default boolean isError() {
        return this instanceof Err<T,E>;
    }

    /**
     * Returns whether this {@code Result} represents a successful value.
     * <p>
     * Equivalent to checking {@code instanceOf Ok}.
     *
     * @return {@code true} if this is an {@link Ok}, otherwise {@code false}
     */
    default boolean isOk() {
        return this instanceof Ok<T,E>;
    }

    /**
     * Transforms the success value using the given function, leaving errors unchanged.
     *
     * @param f   the function to apply to the success value
     * @param <U> the type of the new success value
     * @return a new {@code Result} containing the transformed value, or the original error
     */
    default <U> Result<U, E> map(@NonNull Function<? super T, ? extends U> f) {
        return switch (this) {
            case Result.Err<T, E> v -> new Err<>(v.error);
            case Result.Ok<T, E> v -> new Ok<>(f.apply(v.value));
        };
    }

    /**
     * Transforms the success value using a function that returns a {@code Result}.
     * <p>
     * Similar to {@link #map(Function)}, but the mapping function itself can fail.
     *
     * @param f   the function to apply to the success value
     * @param <U> the type of the new success value
     * @return the resulting {@code Result} from the function, or the original error
     */
    default <U> Result<U, E> flatMap(@NonNull Function<? super T, ? extends Result<U, E>> f) {
        return switch (this) {
            case Result.Err<T, E> v -> new Err<>(v.error);
            case Result.Ok<T, E> v -> f.apply(v.value);
        };
    }

    /**
     * Transforms the error value using the given function, leaving successes unchanged.
     *
     * @param f   the function to apply to the error
     * @param <F> the type of the new error
     * @return a new {@code Result} with the transformed error or the original success
     */
    default <F extends Error> Result<T, F> mapError(@NonNull Function<? super E, ? extends F> f) {
       return switch (this) {
           case Result.Err<T, E> v -> new Err<>(f.apply(v.error));
           case Result.Ok<T, E> v -> new Ok<>(v.value);
       };
    }

    /**
     * Applies one of the two given functions to this {@code Result}, depending on whether
     * it is a success ({@link Ok}) or an error ({@link Err}).
     *
     * @param onOk  function to apply if this is {@link Ok}
     * @param onErr function to apply if this is {@link Err}
     * @param <R>   the return type of the function
     * @return the result of applying the appropriate function
     */
    default <R> R fold(@NonNull Function<? super T, ? extends R> onOk,
                       @NonNull Function<? super E, ? extends R> onErr) {
        return switch (this) {
            case Result.Err<T, E> v -> onErr.apply(v.error);
            case Result.Ok<T, E> v -> onOk.apply(v.value);
        };
    }

    /**
     * Recovers from an error by providing a default success value.
     *
     * @param f function mapping the error to a success value
     * @return a new {@code Result} containing the recovered value, or the original success
     */
    default Result<T, E> recover(@NonNull Function<? super E, ? extends T> f) {
        return switch (this) {
            case Err<T, E> v -> ok(f.apply(v.error));
            case Ok<T, E> _ -> this;
        };
    }

    /**
     * Recovers from an error by providing a new {@code Result}.
     *
     * @param f function mapping the error to a new {@code Result}
     * @return the new {@code Result} or the original success
     */
    default Result<T, E> recoverWith(@NonNull Function<? super E, ? extends Result<T, E>> f) {
        return switch (this) {
            case Err<T, E> v -> f.apply(v.error);
            case Ok<T, E> _ -> this;
        };
    }

    /**
     * Executes the given action if this is {@link Ok}, leaving the result unchanged.
     *
     * @param action action to perform on the success value
     * @return this {@code Result} for chaining
     */
    default Result<T, E> peekOk(@NonNull Consumer<? super T> action) {
        if (this instanceof Ok<T,E>(T value)) action.accept(value);
        return this;
    }

    /**
     * Executes the given action if this is {@link Err}, leaving the result unchanged.
     *
     * @param action action to perform on the error value
     * @return this {@code Result} for chaining
     */
    default Result<T, E> peekErr(@NonNull Consumer<? super E> action) {
        if (this instanceof Err<T,E>(E error)) action.accept(error);
        return this;
    }

    /**
     * Returns a {@link Stream} containing the success value if present,
     * otherwise an empty stream.
     *
     * @return a stream of zero or one elements
     */
    default Stream<T> stream() {
        return switch (this) {
            case Err<T, E> _ -> Stream.empty();
            case Ok<T, E> v -> Stream.of(v.value);
        };
    }

    /**
     * Returns the success value if present, otherwise returns the fallback value.
     *
     * @param fallback the fallback value to return if this is {@link Err}
     * @return the success value or the fallback
     */
    default T orElse(T fallback) {
        return fold(v -> v, _ -> fallback);
    }

    /**
     * Returns the success value if present, otherwise computes a fallback using a supplier.
     *
     * @param supplier the supplier of a fallback value
     * @return the success value or the supplied fallback
     */
    default T orElseGet(@NonNull Supplier<? extends T> supplier) {
        return fold(v -> v, _ -> supplier.get());
    }

    /**
     * Returns the success value if present, otherwise {@code null}.
     *
     * @return the success value or {@code null} if this is {@link Err}
     */
    default T get() {
        return switch (this) {
            case Result.Ok<T, E> v -> v.value;
            case Result.Err<T, E> _ -> null;
        };
    }

    /**
     * Combines two {@code Result} values into one {@code Result} containing a {@link Pair}.
     * <p>
     * If either {@code Result} is an error, the first encountered error is returned.
     *
     * @param r1 first result
     * @param r2 second result
     * @param <A> type of first result value
     * @param <B> type of second result value
     * @param <E> type of error
     * @return a {@code Result} containing a {@link Pair} of the two values or an error
     */
    static <A, B, E extends Error> Result<Pair<A, B>, E> zip(Result<A, E> r1, Result<B, E> r2) {
        if (r1 instanceof Err<A, E>(E error)) return err(error);
        if (r2 instanceof Err<B, E>(E error)) return err(error);
        return ok(new Pair<>(r1.get(), r2.get()));
    }

    /**
     * Creates a successful {@code Result} containing the given value.
     *
     * @param value the success value, must be non-null
     * @param <T>   the type of the value
     * @param <E>   the error type
     * @return a new {@link Ok} containing the value
     */
    static <T, E extends Error> Ok<T, E> ok(@NonNull T value) {
        return new Ok<>(value);
    }

    /**
     * Creates an erroneous {@code Result} containing the given error.
     *
     * @param error the error value, must be non-null
     * @param <T>   the success type
     * @param <E>   the error type
     * @return a new {@link Err} containing the error
     */
    static <T, E extends Error> Err<T, E> err(@NonNull E error) {
        return new Err<>(error);
    }

    /**
     * Represents a successful {@code Result}.
     *
     * @param <T> the type of the success value
     * @param <E> the type of the error
     */
    record Ok<T, E extends Error>(@NonNull T value) implements Result<T, E> {}

    /**
     * Represents a failed {@code Result}.
     *
     * @param <T> the type of the success value
     * @param <E> the type of the error
     */
    record Err<T, E extends Error>(@NonNull E error) implements Result<T, E> {}
}
