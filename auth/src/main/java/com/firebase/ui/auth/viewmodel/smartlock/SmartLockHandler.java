package com.firebase.ui.auth.viewmodel.smartlock;

import android.app.*;
import android.util.Log;

import com.firebase.ui.auth.*;
import com.firebase.ui.auth.data.model.*;
import com.firebase.ui.auth.util.*;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.*;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;

import androidx.annotation.*;

/**
 * ViewModel for initiating saves to the Credentials API (SmartLock).
 */
public class SmartLockHandler extends AuthViewModelBase<IdentityProviderResponse> {
    private static final String TAG = "SmartLockViewModel";

    private IdentityProviderResponse mResponse;

    public SmartLockHandler(Application application) {
        super(application);
    }

    public void setResponse(@NonNull IdentityProviderResponse response) {
        mResponse = response;
    }

    /**
     * Forward the result of a resolution from the Activity to the ViewModel.
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == RequestCodes.CRED_SAVE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Resource.forSuccess(mResponse));
            } else {
                Log.e(TAG, "SAVE: Canceled by user.");
                FirebaseUiException exception = new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR, "Save canceled by user.");
                setResult(Resource.forFailure(exception));
            }
        }
    }

    /** @see #saveCredentials(Credential) */
    @RestrictTo(RestrictTo.Scope.TESTS)
    public void saveCredentials(FirebaseUser firebaseUser, @Nullable String password, @Nullable String accountType) {
        saveCredentials(CredentialUtils.buildCredential(firebaseUser, password, accountType));
    }

    /** Initialize saving a credential. */
    public void saveCredentials(@Nullable Credential credential) {
        if (credentialsDisabled()) {
            setResult(Resource.forSuccess(mResponse));
            return;
        }

        setResult(Resource.forLoading());

        if (credential == null) {
            setResult(Resource.forFailure(new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR, "Failed to build credential.")));
            return;
        }

        deleteUnusedCredentials();

        getCredentialsClient().save(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            setResult(Resource.forSuccess(mResponse));
                        } else if (task.getException() instanceof ResolvableApiException) {
                            ResolvableApiException rae = (ResolvableApiException) task.getException();
                            setResult(Resource.forFailure(new PendingIntentRequiredException(rae.getResolution(), RequestCodes.CRED_SAVE)));
                        } else {
                            Log.w(TAG, "Non-resolvable exception: " + task.getException());
                            setResult(Resource.forFailure(new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR,
                                    "Error when saving credential.",
                                    task.getException()))
                            );
                        }
                    }
                });
    }

    private boolean credentialsDisabled() {
        return !getArguments().enableCredentials;
    }

    private void deleteUnusedCredentials() {
        if (mResponse.getProviderType().equals(GoogleAuthProvider.PROVIDER_ID)) {
            // Since Google accounts upgrade email ones, we don't want to end up
            // with duplicate credentials so delete the email ones.
            String type = ProviderUtils.providerIdToAccountType(GoogleAuthProvider.PROVIDER_ID);
            Credential credential = CredentialUtils.buildCredentialOrThrow(getCurrentUser(), "pass", type);
            GoogleApiUtils.getCredentialsClient(getApplication()).delete(credential);
        }
    }
}
