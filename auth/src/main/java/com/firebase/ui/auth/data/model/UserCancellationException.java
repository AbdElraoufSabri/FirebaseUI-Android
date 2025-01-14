package com.firebase.ui.auth.data.model;


import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

import androidx.annotation.RestrictTo;

public class UserCancellationException extends FirebaseUiException {
    public UserCancellationException() {
        super(ErrorCodes.UNKNOWN_ERROR);
    }
}
