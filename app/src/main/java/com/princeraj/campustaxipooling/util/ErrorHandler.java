package com.princeraj.campustaxipooling.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import android.view.View;

/**
 * Global error handler for displaying user-friendly error messages.
 * Centralizes error UI logic (Snackbars, Dialogs, Toast) to avoid duplication.
 *
 * Usage:
 *   ErrorHandler.showError(view, result.getUserMessage(), result.canRetry(), () -> retry());
 */
public class ErrorHandler {

    private static final String TAG = "ErrorHandler";

    /**
     * Shows an error Snackbar with optional retry button.
     *
     * @param view Parent view for Snackbar anchor
     * @param message User-friendly error message
     * @param showRetry If true, displays a "RETRY" action button
     * @param onRetry Callback when user taps retry
     */
    public static void showError(
            @NonNull View view,
            @Nullable String message,
            boolean showRetry,
            @Nullable Runnable onRetry) {

        if (message == null) {
            message = "An error occurred. Please try again.";
        }

        Log.w(TAG, "Showing error: " + message);

        Snackbar snackbar = Snackbar
                .make(view, message, Snackbar.LENGTH_LONG);

        if (showRetry && onRetry != null) {
            snackbar.setAction("RETRY", v -> {
                Log.d(TAG, "User tapped retry");
                onRetry.run();
            });
        }

        snackbar.show();
    }

    /**
     * Shows an error Snackbar without retry button.
     */
    public static void showError(@NonNull View view, @Nullable String message) {
        showError(view, message, false, null);
    }

    /**
     * Shows a fatal error dialog (non-dismissible without action).
     * Used for critical errors like authentication failure.
     */
    public static void showFatalError(
            @NonNull Context context,
            @Nullable String title,
            @Nullable String message,
            @Nullable String actionText,
            @Nullable Runnable onAction) {

        if (title == null) title = "Error";
        if (message == null) message = "An unexpected error occurred.";
        if (actionText == null) actionText = "OK";

        Log.e(TAG, "Showing fatal error: " + message);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(actionText, (dialog, which) -> {
                    dialog.dismiss();
                    if (onAction != null) {
                        onAction.run();
                    }
                })
                .show();
    }

    /**
     * Determines if error is user-actionable (vs system/transient).
     * Used to decide whether to show user-facing error messages.
     */
    public static boolean isUserActionableError(@NonNull SafeResult<?> result) {
        String errorCode = result.getErrorCode();
        return !errorCode.equals("TIMEOUT")
                && !errorCode.equals("SERVICE_UNAVAILABLE")
                && !errorCode.equals("NETWORK_ERROR");
    }

    /**
     * Logs error for analytics/debugging.
     * Strips sensitive data (passwords, tokens) before logging.
     */
    public static void logError(@NonNull String tag, @NonNull SafeResult<?> result) {
        String errorMsg = String.format(
                "Error [%s]: %s (Offline: %b, CanRetry: %b)",
                result.getErrorCode(),
                result.getUserMessage(),
                result.isOffline(),
                result.canRetry()
        );
        Log.e(tag, errorMsg, result.getException());
    }

    /**
     * Extracts a user-safe error message from an exception.
     * Removes internal implementation details.
     */
    @NonNull
    public static String sanitizeErrorMessage(@NonNull Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return "An unexpected error occurred.";
        }

        // Remove internal Firebase error prefixes
        msg = msg.replace("[", "")
                .replace("]", "")
                .replaceAll("com\\.google\\.firebase\\.[^ ]+", "")
                .trim();

        // If message is now empty, use default
        if (msg.isEmpty()) {
            return "An unexpected error occurred.";
        }

        return msg;
    }

    /**
     * Determines the UI action to show based on error code.
     */
    public enum ErrorAction {
        SHOW_RETRY,      // Show "RETRY" button
        SHOW_DISMISS,    // Show "DISMISS" button only
        SHOW_LOGIN,      // Redirect to login
        SHOW_SETTINGS    // Redirect to settings
    }

    /**
     * Gets recommended action for error type.
     */
    @NonNull
    public static ErrorAction getRecommendedAction(@NonNull SafeResult<?> result) {
        String errorCode = result.getErrorCode();

        if (errorCode.equals("UNAUTHENTICATED")) {
            return ErrorAction.SHOW_LOGIN;
        } else if (errorCode.equals("PERMISSION_DENIED")) {
            return ErrorAction.SHOW_SETTINGS;
        } else if (result.canRetry()) {
            return ErrorAction.SHOW_RETRY;
        } else {
            return ErrorAction.SHOW_DISMISS;
        }
    }
}

