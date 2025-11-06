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

@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
public final class Task<T> {
    private final Future<T> future;

    public void cancel() {
        future.cancel(true);
    }

    public boolean isCancelled() {
        return future.isCancelled();
    }

    public boolean isDone() {
        return future.isDone();
    }

    public Result<T, AsyncError> await() {
        try {
            return ok(future.get());
        } catch (CancellationException | ExecutionException exception) {
            return err(new AsyncError(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return err(new AsyncError(exception));
        }
    }

    public <U> Result<U, AsyncError> map(@NonNull Function<? super T, ? extends U> f) {
        return await().map(f);
    }

    public <U> Result<U, AsyncError> flatMap(@NonNull Function<? super T, ? extends Result<U, AsyncError>> f) {
        return await().flatMap(f);
    }
}
