package gentle;

import lombok.NonNull;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static gentle.Result.*;

/**
 * A container object which may or may not contain a non-null value.
 * <p>
 * {@code Optional} is a functional, type-safe alternative to using {@code null} to indicate
 * the absence of a value. It can either hold a value of type {@code T} (represented by {@link Some})
 * or be empty (represented by {@link None}).
 *
 * <p>Typical usage:
 * <pre>{@code
 * Optional<String> name = Optional.of("Alice");
 * String value = name.orElse("Unknown");
 * name.ifPresent(System.out::println);
 * }</pre>
 *
 * @param <T> the type of the contained value
 */
public sealed interface Optional<T> permits Optional.Some, Optional.None {

    /**
     * Converts this {@code Optional} to a {@link java.util.Optional}.
     *
     * @return a {@code java.util.Optional} containing the value if present, otherwise empty
     */
    default java.util.Optional<T> asOptional() {
        return switch (this) {
            case None<T> _ -> java.util.Optional.empty();
            case Some<T> v -> java.util.Optional.of(v.value);
        };
    }

    /**
     * Returns the contained value if present.
     *
     * @return the contained value
     * @throws NoSuchElementException if no value is present
     */
    default T get() throws NoSuchElementException {
        return switch (this) {
            case None<T> _ -> throw new NoSuchElementException("No value present.");
            case Some<T> v -> v.value;
        };
    }

    /**
     * Returns {@code true} if a value is present.
     *
     * @return {@code true} if the value is present, otherwise {@code false}
     */
    default boolean isPresent() {
        return this instanceof Some<T>;
    }

    /**
     * Returns {@code true} if a value is present.
     *
     * @return {@code true} if the value is present, otherwise {@code false}
     */
    default boolean isSome() {
        return this instanceof Some<T>;
    }

    /**
     * Returns {@code true} if no value is present.
     *
     * @return {@code true} if the value is absent, otherwise {@code false}
     */
    default boolean isEmpty() {
        return this instanceof None<T>;
    }

    /**
     * Returns {@code true} if no value is present.
     *
     * @return {@code true} if the value is absent, otherwise {@code false}
     */
    default boolean isNone() {
        return this instanceof None<T>;
    }

    /**
     * Executes the given action if a value is present.
     *
     * @param action the action to perform if a value is present
     */
    default void ifPresent(@NonNull Consumer<? super T> action) {
        if (this instanceof Some<T>(T value)) {
            action.accept(value);
        }
    }

    /**
     * Executes one of the two actions depending on whether a value is present.
     *
     * @param action      the action to perform if a value is present
     * @param emptyAction the action to perform if no value is present
     */
    default void ifPresentOrElse(@NonNull Consumer<? super T> action, @NonNull Runnable emptyAction) {
        switch (this) {
            case None<T> _ -> emptyAction.run();
            case Some<T> v -> action.accept(v.value);
        }
    }

    /**
     * Returns this {@code Optional} if it contains a value matching the predicate,
     * otherwise returns empty.
     *
     * @param predicate the predicate to apply to the value, if present
     * @return this {@code Optional} if value matches, otherwise {@link None}
     */
    default Optional<T> filter(Predicate<? super T> predicate) {
        return switch (this) {
            case None<T> v -> v;
            case Some<T> v -> predicate.test(v.value) ? this : empty();
        };
    }

    /**
     * Transforms the contained value using the given mapping function if present.
     *
     * @param f   the function to apply to the value
     * @param <U> the type of the new value
     * @return a new {@code Optional} containing the transformed value, or empty if absent
     */
    default <U> Optional<U> map(@NonNull Function<? super T, ? extends U> f) {
        return switch (this) {
            case None<T> _ -> empty();
            case Some<T> v -> maybe(f.apply(v.value));
        };
    }

    /**
     * Transforms the contained value using a function returning an {@code Optional}.
     *
     * @param f   the mapping function
     * @param <U> the type of the value in the resulting {@code Optional}
     * @return the {@code Optional} returned by the function or empty if absent
     */
    default <U> Optional<U> flatMap(@NonNull Function<? super T, ? extends Optional<U>> f) {
        return switch (this) {
            case None<T> _ -> empty();
            case Some<T> v -> f.apply(v.value);
        };
    }

    /**
     * Returns this {@code Optional} if present, otherwise returns an {@code Optional}
     * supplied by the given supplier.
     *
     * @param supplier the supplier of an {@code Optional} to return if empty
     * @return this {@code Optional} if value is present, otherwise the supplied {@code Optional}
     */
    @SuppressWarnings("unchecked")
    default Optional<T> or(@NonNull Supplier<? extends Optional<? extends T>> supplier) {
        return switch (this) {
            case None<T> _ -> (Optional<T>) supplier.get();
            case Some<T> v -> v;
        };
    }

    /**
     * Executes the given action if a value is present and returns this {@code Optional}.
     *
     * @param action the action to perform if value is present
     * @return this {@code Optional}
     */
    default Optional<T> peekSome(@NonNull Consumer<? super T> action) {
        if (this instanceof Some<T>(T value)) action.accept(value);
        return this;
    }

    /**
     * Executes the given action if no value is present and returns this {@code Optional}.
     *
     * @param action the action to perform if value is absent
     * @return this {@code Optional}
     */
    default Optional<T> peekNone(@NonNull Runnable action) {
        if (this instanceof None<T>) action.run();
        return this;
    }

    /**
     * Returns a {@link Stream} containing the value if present, otherwise an empty stream.
     *
     * @return a stream containing the value or empty
     */
    default Stream<T> stream() {
        return switch (this) {
            case None<T> _ -> Stream.empty();
            case Some<T> v -> Stream.of(v.value);
        };
    }

    /**
     * Returns the value if present, otherwise returns the provided fallback.
     *
     * @param other the fallback value
     * @return the value if present, otherwise {@code other}
     */
    default T orElse(T other) {
        return switch (this) {
            case None<T> _ -> other;
            case Some<T> v -> v.value;
        };
    }

    /**
     * Returns the value if present, otherwise computes a fallback using the supplier.
     *
     * @param supplier the fallback supplier
     * @return the value if present, otherwise the supplied value
     */
    default T orElseGet(@NonNull Supplier<? extends T> supplier) {
        return switch (this) {
            case None<T> _ -> supplier.get();
            case Some<T> v -> v.value;
        };
    }

    /**
     * Returns the value if present, otherwise throws {@link NoSuchElementException}.
     *
     * @return the contained value
     * @throws NoSuchElementException if no value is present
     */
    default T orElseThrow() throws NoSuchElementException {
        return switch (this) {
            case None<T> _ -> throw new NoSuchElementException("No value present.");
            case Some<T> v -> v.value;
        };
    }

    /**
     * Returns the value if present, otherwise throws an exception supplied by the given supplier.
     *
     * @param <X>               the type of exception to throw
     * @param exceptionSupplier the supplier of the exception
     * @return the contained value
     * @throws X if no value is present
     */
    default <X extends Throwable> T orElseThrow(@NonNull Supplier<? extends X> exceptionSupplier) throws X {
        return switch (this) {
            case None<T> _ -> throw exceptionSupplier.get();
            case Some<T> v -> v.value;
        };
    }

    /**
     * Converts this {@code Optional} into a {@link Result}, using the provided
     * supplier to produce an error if no value is present.
     *
     * @param supplier the error supplier
     * @param <E>      the type of error
     * @return a {@code Result} containing the value or the error
     */
    default <E extends Error> Result<T, E> result(@NonNull Supplier<E> supplier) {
        return switch (this) {
            case None<T> _ -> err(supplier.get());
            case Some<T> v -> ok(v.value);
        };
    }

    /**
     * Returns an empty {@code Optional}.
     *
     * @param <T> type parameter
     * @return empty {@code Optional}
     */
    static <T> None<T> empty() {
        return new None<>();
    }

    /**
     * Alias for {@link #empty()}.
     *
     * @param <T> type parameter
     * @return empty {@code Optional}
     */
    static <T> None<T> none() {
        return new None<>();
    }

    /**
     * Returns an {@code Optional} containing the given non-null value.
     *
     * @param value the non-null value
     * @param <T>   type parameter
     * @return {@code Optional} containing the value
     */
    static <T> Some<T> of(@NonNull T value) {
        return new Some<>(value);
    }

    /**
     * Alias for {@link #of(Object)}.
     *
     * @param value the non-null value
     * @param <T>   type parameter
     * @return {@code Optional} containing the value
     */
    static <T> Some<T> some(@NonNull T value) {
        return new Some<>(value);
    }

    /**
     * Returns an {@code Optional} containing the value if non-null, otherwise empty.
     *
     * @param value the value
     * @param <T>   type parameter
     * @return {@code Optional} containing the value or empty
     */
    static <T> Optional<T> ofNullable(T value) {
        if (value == null) return empty();
        return some(value);
    }

    /**
     * Alias for {@link #ofNullable(Object)}.
     *
     * @param value the value
     * @param <T>   type parameter
     * @return {@code Optional} containing the value or empty
     */
    static <T> Optional<T> maybe(T value) {
        return ofNullable(value);
    }

    /** Represents a value present in the Optional. */
    record Some<T>(@NonNull T value) implements Optional<T> {}

    /** Represents the absence of a value in the Optional. */
    record None<T>() implements Optional<T> {}
}
