package com.firebase.ui.auth;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.*;

/**
 * Error codes for failed sign-in attempts.
 */
public final class ErrorCodes {
    /**
     * An unknown error has occurred.
     */
    public static final int UNKNOWN_ERROR = 0;
    /**
     * Sign in failed due to lack of network connection.
     */
    public static final int NO_NETWORK = 1;
    /**
     * A required update to Play Services was cancelled by the user.
     */
    public static final int PLAY_SERVICES_UPDATE_CANCELLED = 2;
    /**
     * A sign-in operation couldn't be completed due to a developer error.
     */
    public static final int DEVELOPER_ERROR = 3;
    /**
     * An external sign-in provider error occurred.
     */
    public static final int PROVIDER_ERROR = 4;
    /**
     * Signing in with a different email in the WelcomeBackIdp flow
     */
    public static final int EMAIL_MISMATCH_ERROR = 6;
    /**
     *  Attempting to auth with account that is currently disabled in the Firebase console.
     */
    public static final int ERROR_USER_DISABLED = 12;

    private ErrorCodes() {
        throw new AssertionError("No instance for you!");
    }

    @NonNull
    
    public static String toFriendlyMessage(@Code int code) {
        switch (code) {
            case UNKNOWN_ERROR:
                return "Unknown error";
            case NO_NETWORK:
                return "No internet connection";
            case PLAY_SERVICES_UPDATE_CANCELLED:
                return "Play Services update cancelled";
            case DEVELOPER_ERROR:
                return "Developer error";
            case PROVIDER_ERROR:
                return "Provider error";
            case EMAIL_MISMATCH_ERROR:
                return "You are are attempting to sign in a different email than previously " +
                        "provided";
            case ERROR_USER_DISABLED:
                return "The user account has been disabled by an administrator.";
            default:
                throw new IllegalArgumentException("Unknown code: " + code);
        }
    }

    /**
     * Valid codes that can be returned from {@link FirebaseUiException#getErrorCode()}.
     */
    @IntDef({
            UNKNOWN_ERROR,
            NO_NETWORK,
            PLAY_SERVICES_UPDATE_CANCELLED,
            DEVELOPER_ERROR,
            PROVIDER_ERROR,
            EMAIL_MISMATCH_ERROR,
            ERROR_USER_DISABLED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Code {
    }
}
