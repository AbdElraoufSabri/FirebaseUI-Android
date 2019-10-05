package com.firebase.ui.auth.viewmodel;

import android.app.Application;

import com.firebase.ui.auth.data.model.*;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;

import androidx.annotation.*;

public abstract class AuthViewModelBase<T> extends OperableViewModel<FlowParameters, Resource<T>> {
    private CredentialsClient mCredentialsClient;
    private FirebaseAuth mAuth;

    protected AuthViewModelBase(Application application) {
        super(application);
    }

    @Override
    protected void onCreate() {
        FirebaseApp app = FirebaseApp.getInstance(getArguments().appName);
        mAuth = FirebaseAuth.getInstance(app);
        mCredentialsClient = GoogleApiUtils.getCredentialsClient(getApplication());
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    protected FirebaseAuth getAuth() {
        return mAuth;
    }


    protected CredentialsClient getCredentialsClient() {
        return mCredentialsClient;
    }

    @VisibleForTesting
    public void initializeForTesting(FlowParameters parameters,
                                     FirebaseAuth auth,
                                     CredentialsClient client) {
        setArguments(parameters);
        mAuth = auth;
        mCredentialsClient = client;
    }
}
