package com.firebase.ui.auth.viewmodel;

import android.app.Application;
import android.content.Intent;

import com.firebase.ui.auth.IdentityProviderResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.ui.HelperActivityBase;

import androidx.annotation.*;

/**
 * Handles retrieving a provider's login credentials, be that a token, secret, or both.
 */
public abstract class ProviderSignInBase<T> extends OperableViewModel<T, Resource<IdentityProviderResponse>> {
    protected ProviderSignInBase(Application application) {
        super(application);
    }

    /**
     * Start the login process for the IDP, e.g. show the Google sign-in activity.
     *
     * @param activity from which to start the login, DO NOT USE OUTSIDE OF THIS METHOD!!!
     */
    public abstract void startSignIn(@NonNull HelperActivityBase activity);


    public abstract void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
}
