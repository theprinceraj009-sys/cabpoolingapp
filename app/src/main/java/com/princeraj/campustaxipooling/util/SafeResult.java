package com.princeraj.campustaxipooling.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Enhanced result wrapper for all repository operations.
 * Replaces {@link Resource} with comprehensive error handling, retry metadata,
 * and offline indicators.
 *
 * Standard states: Loading, Success, Error, Cached (offline)
 *
 * Example usage:
 *   SafeResult<List<Ride>> result = repository.getRides();
 *   if (result.isSuccess()) {
 *       handleSuccess(result.getOrNull());
 *   } else if (result.isError()) {
 *       showRetryDialog(result.getException(), result.canRetry());
 *   }
 */
public class SafeResult<T> {

    public enum Status {
        LOADING,    // Async operation in progress
        SUCCESS,    // Data available and valid
        ERROR,      // Operation failed with exception
        CACHED      // Data from local cache (offline)
    }

    @NonNull
    private final Status status;

    @Nullable
    private final T data;

    @Nullable
    private final Exception exception;

    @Nullable
    private final String userMessage;

    @NonNull
    private final String errorCode;

    private final boolean isOffline;
    private final boolean canRetry;
    private final int retryCount;

    private SafeResult(
            @NonNull Status status,
            @Nullable T data,
            @Nullable Exception exception,
            @Nullable String userMessage,
            @NonNull String errorCode,
            boolean isOffline,
            boolean canRetry,
            int retryCount) {
        this.status = status;
        this.data = data;
        this.exception = exception;
        this.userMessage = userMessage;
        this.errorCode = errorCode;
        this.isOffline = isOffline;
        this.canRetry = canRetry;
        this.retryCount = retryCount;
    }

    // ── Factory Methods ──────────────────────────────────────────────────────

    /**
     * Creates a loading state (typically shown as progress bar).
     */
    public static <T> SafeResult<T> loading() {
        return new SafeResult<>(Status.LOADING, null, null, null, "", false, false, 0);
    }

    /**
     * Creates a loading state with cached data (for smooth UX during refresh).
     */
    public static <T> SafeResult<T> loading(@NonNull T cachedData) {
        return new SafeResult<>(Status.LOADING, cachedData, null, null, "", false, false, 0);
    }

    /**
     * Creates a success state with data.
     */
    public static <T> SafeResult<T> success(@NonNull T data) {
        return new SafeResult<>(Status.SUCCESS, data, null, null, "", false, false, 0);
    }

    /**
     * Creates an error state from a Firebase exception.
     * Automatically determines if the error is retryable and if we're offline.
     */
    public static <T> SafeResult<T> error(
            @NonNull Exception exception,
            @Nullable String userMessage) {
        String errorCode = inferErrorCode(exception);
        boolean canRetry = isRetryableError(errorCode);
        boolean isOffline = isOfflineError(exception);

        return new SafeResult<>(
                Status.ERROR,
                null,
                exception,
                userMessage != null ? userMessage : getDefaultErrorMessage(errorCode),
                errorCode,
                isOffline,
                canRetry,
                0
        );
    }

    /**
     * Creates an error state with a simple message.
     */
    public static <T> SafeResult<T> error(@Nullable String userMessage) {
        return new SafeResult<>(Status.ERROR, null, null, userMessage, "GENERIC_ERROR", false, false, 0);
    }

    /**
     * Creates an error state with cached fallback data.
     * Used when offline and we have stale data to show.
     */
    public static <T> SafeResult<T> errorWithCache(
            @NonNull Exception exception,
            @NonNull T cachedData,
            @Nullable String userMessage) {
        String errorCode = inferErrorCode(exception);
        boolean canRetry = isRetryableError(errorCode);

        return new SafeResult<>(
                Status.ERROR,
                cachedData,
                exception,
                userMessage != null ? userMessage : getDefaultErrorMessage(errorCode),
                errorCode,
                true,
                canRetry,
                0
        );
    }

    /**
     * Creates a cached (offline) state with locally stored data.
     */
    public static <T> SafeResult<T> cached(@NonNull T data) {
        return new SafeResult<>(Status.CACHED, data, null, null, "OFFLINE_CACHE", true, false, 0);
    }

    /**
     * Creates an error with custom error code and user message.
     */
    public static <T> SafeResult<T> error(
            @NonNull String errorCode,
            @Nullable T fallbackData,
            @Nullable String userMessage,
            boolean canRetry) {
        return new SafeResult<>(
                Status.ERROR,
                fallbackData,
                null,
                userMessage != null ? userMessage : getDefaultErrorMessage(errorCode),
                errorCode,
                false,
                canRetry,
                0
        );
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    @NonNull
    public Status getStatus() {
        return status;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isCached() {
        return status == Status.CACHED;
    }

    /**
     * Returns data if available (success or cached), null otherwise.
     */
    @Nullable
    public T getOrNull() {
        return data;
    }

    /**
     * Alias for getOrNull() - provides better readability in UI observers.
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Returns data or throws exception if error.
     */
    @NonNull
    public T getOrThrow() throws Exception {
        if (exception != null) throw exception;
        if (data == null) throw new IllegalStateException("No data available");
        return data;
    }

    /**
     * Returns data or default fallback.
     */
    @NonNull
    public T getOrDefault(@NonNull T fallback) {
        return data != null ? data : fallback;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    @Nullable
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Alias for getUserMessage() - shorthand for the most common use case.
     */
    @Nullable
    public String getMessage() {
        return userMessage;
    }

    @NonNull
    public String getErrorCode() {
        return errorCode;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public boolean canRetry() {
        return canRetry;
    }

    public int getRetryCount() {
        return retryCount;
    }

    // ── Error Code Inference ─────────────────────────────────────────────────

    /**
     * Infers error code from Firebase exception type.
     */
    private static String inferErrorCode(@NonNull Exception exception) {
        String message = exception.getMessage();
        if (message == null) message = exception.getClass().getSimpleName();

        if (message.contains("PERMISSION_DENIED")) {
            return "PERMISSION_DENIED";
        } else if (message.contains("NOT_FOUND")) {
            return "NOT_FOUND";
        } else if (message.contains("ALREADY_EXISTS")) {
            return "ALREADY_EXISTS";
        } else if (message.contains("DEADLINE_EXCEEDED")) {
            return "TIMEOUT";
        } else if (message.contains("UNAVAILABLE")) {
            return "SERVICE_UNAVAILABLE";
        } else if (message.contains("UNAUTHENTICATED")) {
            return "UNAUTHENTICATED";
        } else if (message.contains("INVALID_ARGUMENT")) {
            return "INVALID_ARGUMENT";
        } else if (message.contains("Network")) {
            return "NETWORK_ERROR";
        } else if (message.contains("Quota")) {
            return "QUOTA_EXCEEDED";
        } else {
            return "UNKNOWN_ERROR";
        }
    }

    /**
     * Determines if error is retryable (transient).
     */
    private static boolean isRetryableError(@NonNull String errorCode) {
        return errorCode.equals("TIMEOUT")
                || errorCode.equals("SERVICE_UNAVAILABLE")
                || errorCode.equals("NETWORK_ERROR")
                || errorCode.equals("DEADLINE_EXCEEDED");
    }

    /**
     * Detects offline state from exception.
     */
    private static boolean isOfflineError(@NonNull Exception exception) {
        String message = exception.getMessage();
        return message != null && (
                message.contains("Network") ||
                        message.contains("Disconnected") ||
                        message.contains("UNAVAILABLE")
        );
    }

    /**
     * Provides user-friendly error messages based on error code.
     */
    private static String getDefaultErrorMessage(@NonNull String errorCode) {
        switch (errorCode) {
            case "PERMISSION_DENIED":
                return "You don't have permission to access this. Contact admin if this seems wrong.";
            case "NOT_FOUND":
                return "The resource you're looking for doesn't exist.";
            case "ALREADY_EXISTS":
                return "This resource already exists.";
            case "TIMEOUT":
                return "The operation took too long. Please try again.";
            case "SERVICE_UNAVAILABLE":
                return "The service is temporarily unavailable. We're working to fix it.";
            case "UNAUTHENTICATED":
                return "You need to log in to perform this action.";
            case "NETWORK_ERROR":
                return "Network connection failed. Please check your internet.";
            case "QUOTA_EXCEEDED":
                return "You've exceeded your request quota. Please try again later.";
            case "INVALID_ARGUMENT":
                return "Invalid input. Please check your data.";
            case "OFFLINE_CACHE":
                return "Showing cached data (you're offline).";
            default:
                return "An unexpected error occurred. Please try again.";
        }
    }

    // ── Utility Methods ──────────────────────────────────────────────────────

    /**
     * Maps the data in this result to a new type.
     * Preserves status and error information.
     */
    public <R> SafeResult<R> map(@NonNull Mapper<T, R> mapper) {
        try {
            if (isSuccess() || isCached()) {
                return new SafeResult<>(
                        status,
                        mapper.map(data),
                        null,
                        userMessage,
                        errorCode,
                        isOffline,
                        false,
                        0
                );
            } else {
                return new SafeResult<>(status, null, exception, userMessage, errorCode, isOffline, canRetry, retryCount);
            }
        } catch (Exception e) {
            return SafeResult.error(e, "Error mapping data");
        }
    }

    /**
     * Executes a callback based on result status.
     */
    public void observe(
            @Nullable Runnable onLoading,
            @Nullable Observer<T> onSuccess,
            @Nullable Observer<Exception> onError) {
        if (isLoading() && onLoading != null) {
            onLoading.run();
        } else if (isSuccess() && onSuccess != null) {
            onSuccess.accept(data);
        } else if (isError() && onError != null) {
            onError.accept(exception);
        }
    }

    // ── Functional Interfaces ────────────────────────────────────────────────

    @FunctionalInterface
    public interface Mapper<T, R> {
        R map(T value) throws Exception;
    }

    @FunctionalInterface
    public interface Observer<T> {
        void accept(T value);
    }
}

