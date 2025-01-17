package com.firebase.ui.auth.data.model;

import android.content.Intent;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;


public class IntentRequiredException extends FirebaseUiException {
    private final Intent mIntent;
    private final int mRequestCode;

    public IntentRequiredException(@NonNull Intent intent, int requestCode) {
        super(ErrorCodes.UNKNOWN_ERROR);
        mIntent = intent;
        mRequestCode = requestCode;
    }

    @NonNull
    public Intent getIntent() {
        return mIntent;
    }

    public int getRequestCode() {
        return mRequestCode;
    }
}
