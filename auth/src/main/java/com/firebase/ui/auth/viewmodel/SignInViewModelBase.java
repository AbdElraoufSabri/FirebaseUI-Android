package com.firebase.ui.auth.viewmodel;

import android.app.Application;

import com.firebase.ui.auth.IdentityProviderResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.google.firebase.auth.AuthResult;

import androidx.annotation.*;

public abstract class SignInViewModelBase extends AuthViewModelBase<IdentityProviderResponse> {

    protected SignInViewModelBase(Application application) {
        super(application);
    }

    @Override
    protected void setResult(Resource<IdentityProviderResponse> output) {
        super.setResult(output);
    }

    protected void handleSuccess(@NonNull IdentityProviderResponse response, @NonNull AuthResult result) {
        setResult(Resource.forSuccess(response.withResult(result)));
    }
}
