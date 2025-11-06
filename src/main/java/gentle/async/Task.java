package gentle.async;

import gentle.Result;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

import static gentle.Result.err;
import static gentle.Result.ok;

/**
 * Represents a single asynchronous task running inside a {@link Scope}.
 *
 * <p>Provides methods to cancel the task, check status, and retrieve results
 * in a type-safe {@link Result} wrapper.
 *
 * @param <T> type of the task's result
 */
@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
public class Task<T> {

    /** The underlying future representing the task computation. */
    private final Future<T> future;

    /**
     * Cancels the task.
     */
    public void cancel() {
        future.cancel(true);
    }

    /**
     * Returns {@code true} if the task was cancelled.
     *
     * @return whether the task is cancelled
     */
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * Returns {@code true} if the task is done (completed or cancelled).
     *
     * @return whether the task is done
     */
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Waits for the task to complete and returns a {@link Result} containing either the value
     * or an {@link AsyncError}.
     *
     * @return the result of the task
     */
    public final Result<T, AsyncError> await() {
        try {
            return ok(future.get());
        } catch (CancellationException | ExecutionException exception) {
            return err(new AsyncError(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return err(new AsyncError(exception));
        }
    }

    /**
     * Transforms the result of this task using the provided function if successful.
     *
     * @param f   mapping function
     * @param <U> result type of the mapping
     * @return a {@link Result} with the transformed value or the original error
     */
    public final <U> Result<U, AsyncError> map(@NonNull Function<? super T, ? extends U> f) {
        return await().map(f);
    }

    /**
     * Transforms the result of this task using a function returning a {@link Result}.
     *
     * @param f   mapping function
     * @param <U> result type of the mapping
     * @return the {@link Result} returned by the mapping function or the original error
     */
    public final <U> Result<U, AsyncError> flatMap(@NonNull Function<? super T, ? extends Result<U, AsyncError>> f) {
        return await().flatMap(f);
    }
}
