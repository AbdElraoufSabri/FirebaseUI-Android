package com.firebase.ui.auth;


import androidx.annotation.*;

/**
 * Base class for all FirebaseUI exceptions.
 */
public class FirebaseUiException extends Exception {
    private final int mErrorCode;

    
    public FirebaseUiException(@ErrorCodes.Code int code) {
        this(code, ErrorCodes.toFriendlyMessage(code));
    }

    
    public FirebaseUiException(@ErrorCodes.Code int code, @NonNull String message) {
        super(message);
        mErrorCode = code;
    }

    
    public FirebaseUiException(@ErrorCodes.Code int code, @NonNull Throwable cause) {
        this(code, ErrorCodes.toFriendlyMessage(code), cause);
    }

    
    public FirebaseUiException(@ErrorCodes.Code int code,
                               @NonNull String message,
                               @NonNull Throwable cause) {
        super(message, cause);
        mErrorCode = code;
    }

    /**
     * @return error code associated with this exception
     * @see com.firebase.ui.auth.ErrorCodes
     */
    @ErrorCodes.Code
    public final int getErrorCode() {
        return mErrorCode;
    }
}
