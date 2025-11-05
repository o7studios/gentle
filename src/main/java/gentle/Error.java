package gentle;

import lombok.NonNull;

/**
 * Represents a structured error with a numeric {@code code} and a human-readable {@code message}.
 * <p>
 * This interface provides convenient methods for formatting the error message and converting
 * it into a {@link RuntimeException}. Implementations should be immutable and thread-safe.
 *
 * <p>Example usage:
 * <pre>{@code
 * public enum MyError implements Error {
 *     INVALID_INPUT(1001, "Invalid input: %s"),
 *     NOT_FOUND(1002, "Resource not found: %s");
 *
 *     private final int code;
 *     private final String message;
 *
 *     MyError(int code, String message) {
 *         this.code = code;
 *         this.message = message;
 *     }
 *
 *     @Override
 *     public int code() { return code; }
 *
 *     @Override
 *     public String message() { return message; }
 * }
 *
 * throw MyError.INVALID_INPUT.asException("username");
 * }</pre>
 */
public interface Error {

    /**
     * Default error format used by {@link #formatted(Object...)}.
     * <p>
     * The format is: "[code] message".
     */
    String ERROR_FORMAT = "[%s] %s";

    /**
     * Returns the numeric code of the error.
     *
     * @return the error code
     */
    int code();

    /**
     * Returns the human-readable message of the error.
     *
     * @return the error message, never {@code null}
     */
    @NonNull
    String message();

    /**
     * Returns a formatted string representation of the error.
     * <p>
     * The {@code message} can contain format specifiers, which will be replaced
     * by the given {@code formatArgs}. The resulting string is formatted using
     * {@link String#format(String, Object...)} with the default {@link #ERROR_FORMAT}.
     *
     * @param formatArgs the arguments to format the message with
     * @return the formatted error string
     * @throws NullPointerException if {@code formatArgs} is null
     */
    default String formatted(@NonNull Object... formatArgs) {
        return String.format(ERROR_FORMAT, code(), message().formatted(formatArgs));
    }

    /**
     * Converts this error into a {@link RuntimeException} using the formatted error message.
     * <p>
     * This is a convenience method to throw the error as an unchecked exception.
     *
     * @param formatArgs optional arguments to format the message
     * @return a new {@link RuntimeException} with the formatted error message
     * @throws NullPointerException if {@code formatArgs} is null
     */
    default RuntimeException asException(@NonNull Object... formatArgs) {
        return new RuntimeException(formatted(formatArgs));
    }
}
