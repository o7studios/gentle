package gentle.async;

import gentle.Error;
import lombok.NonNull;

public record AsyncError(@NonNull Throwable throwable) implements Error {

    /**
     * Returns a numeric code for this error.
     *
     * @return error code
     */
    @Override
    public int code() {
        return 1;
    }

    /**
     * Returns a human-readable message describing the error.
     *
     * @return error message
     */
    @Override
    public @NonNull String message() {
        return throwable.getMessage();
    }
}
