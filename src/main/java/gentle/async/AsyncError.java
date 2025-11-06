package gentle.async;

import gentle.Error;
import lombok.NonNull;

public record AsyncError(@NonNull Throwable throwable) implements Error {
    @Override
    public int code() {
        return 1;
    }

    @Override
    public @NonNull String message() {
        return throwable.getMessage();
    }
}
