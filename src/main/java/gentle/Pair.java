package gentle;

import lombok.NonNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A container object that holds exactly two non-null values, a {@code left} and a {@code right}.
 * <p>
 * This class is <b>value-based</b>; instances that are {@linkplain #equals(Object) equal} should
 * be considered interchangeable. It is immutable and thread-safe if the contained values themselves
 * are immutable or thread-safe.
 *
 * <p>Common use-cases include returning two values from a method, zipping streams, or pairing
 * results together.
 *
 * @param <L> the type of the left value
 * @param <R> the type of the right value
 */
public record Pair<L, R>(@NonNull L left, @NonNull R right) {

    /**
     * Returns a new {@code Pair} by applying the given function to the {@code left} value,
     * leaving the {@code right} value unchanged.
     *
     * @param f   the function to apply to the left value
     * @param <U> the type of the new left value
     * @return a new {@code Pair} with the transformed left value
     * @throws NullPointerException if the function result is null
     */
    public <U> Pair<U, R> mapLeft(@NonNull Function<? super L, ? extends U> f) {
        return new Pair<>(f.apply(left), right);
    }

    /**
     * Returns a new {@code Pair} by applying the given function to the {@code right} value,
     * leaving the {@code left} value unchanged.
     *
     * @param f   the function to apply to the right value
     * @param <U> the type of the new right value
     * @return a new {@code Pair} with the transformed right value
     * @throws NullPointerException if the function result is null
     */
    public <U> Pair<L, U> mapRight(@NonNull Function<? super R, ? extends U> f) {
        return new Pair<>(left, f.apply(right));
    }

    /**
     * Returns a new {@code Pair} by applying the given functions to both the {@code left} and
     * {@code right} values.
     *
     * @param left  the function to apply to the left value
     * @param right the function to apply to the right value
     * @param <U>           the type of the new left value
     * @param <V>           the type of the new right value
     * @return a new {@code Pair} with transformed values
     * @throws NullPointerException if any function result is null
     */
    public <U, V> Pair<U, V> map(@NonNull Function<? super L, ? extends U> left,
                                 @NonNull Function<? super R, ? extends V> right) {
        return new Pair<>(left.apply(this.left), right.apply(this.right));
    }

    /**
     * Returns a new {@code Pair} with {@code left} and {@code right} values swapped.
     *
     * @return a new {@code Pair} with left and right values swapped
     */
    public Pair<R, L> swap() {
        return new Pair<>(right, left);
    }

    /**
     * Returns a {@link Stream} containing this {@code Pair}.
     * <p>
     * Useful for flat-mapping or processing streams of pairs.
     *
     * @return a {@code Stream} with a single element: this pair
     */
    public Stream<Pair<L, R>> stream() {
        return Stream.of(this);
    }

    /**
     * Creates a new {@code Pair} with the given values.
     *
     * @param left  the left value, must be non-null
     * @param right the right value, must be non-null
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @return a new {@code Pair} instance
     * @throws NullPointerException if either value is null
     */
    public static <L, R> Pair<L, R> of(@NonNull L left, @NonNull R right) {
        return new Pair<>(left, right);
    }

    /**
     * Zips two streams into a single stream of {@code Pair} instances.
     * <p>
     * The resulting stream has length equal to the smaller of the two input streams.
     *
     * @param a   the first stream
     * @param b   the second stream
     * @param <A> the type of elements in the first stream
     * @param <B> the type of elements in the second stream
     * @return a stream of {@code Pair<A, B>}
     * @throws NullPointerException if either stream is null
     */
    public static <A, B> Stream<Pair<A, B>> zip(Stream<A> a, Stream<B> b) {
        var itA = a.iterator();
        var itB = b.iterator();
        Stream.Builder<Pair<A, B>> builder = Stream.builder();
        while (itA.hasNext() && itB.hasNext())
            builder.add(new Pair<>(itA.next(), itB.next()));
        return builder.build();
    }

    /**
     * Indicates whether some other object is "equal to" this {@code Pair}.
     * The other object is considered equal if:
     * <ul>
     *     <li>it is also a {@code Pair}, and
     *     <li>both left values are {@linkplain Objects#equals(Object, Object) equal}, and
     *     <li>both right values are {@linkplain Objects#equals(Object, Object) equal}.
     * </ul>
     *
     * @param obj an object to be tested for equality
     * @return {@code true} if the other object is "equal to" this pair, otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Pair<?, ?>(Object left1, Object right1) &&
                Objects.equals(this.left, left1) &&
                Objects.equals(this.right, right1);
    }

    /**
     * Returns a hash code value for this pair. The hash code is computed as
     * {@code 31 * left.hashCode() + right.hashCode()}.
     *
     * @return hash code value of this pair
     */
    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    /**
     * Returns a string representation of this pair. The format is
     * {@code Pair[left=<left>, right=<right>]}.
     *
     * @return a string representation of this pair
     */
    @Override
    public String toString() {
        return "Pair[left=" + left + ", right=" + right + "]";
    }
}
