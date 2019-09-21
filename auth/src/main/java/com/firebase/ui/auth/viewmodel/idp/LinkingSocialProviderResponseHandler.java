package com.firebase.ui.auth.viewmodel.idp;

import android.app.Application;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.util.data.AuthOperationManager;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.SignInViewModelBase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;

import androidx.annotation.*;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LinkingSocialProviderResponseHandler extends SignInViewModelBase {
    private AuthCredential mRequestedSignInCredential;
    private String mEmail;
    public LinkingSocialProviderResponseHandler(Application application) {
        super(application);
    }

    public void setRequestedSignInCredentialForEmail(@Nullable AuthCredential credential,
                                                     @Nullable String email) {
        mRequestedSignInCredential = credential;
        mEmail = email;
    }

    public void startSignIn(@NonNull final IdpResponse response) {
        if (!response.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(response.getError()));
            return;
        }
        if (!AuthUI.SOCIAL_PROVIDERS.contains(response.getProviderType())) {
            throw new IllegalStateException(
                    "This handler cannot be used to link email or phone providers");
        }
        if (mEmail != null && !mEmail.equals(response.getEmail())) {
            setResult(Resource.<IdpResponse>forFailure(new FirebaseUiException
                    (ErrorCodes.EMAIL_MISMATCH_ERROR)));
            return;
        }

        setResult(Resource.<IdpResponse>forLoading());

        final AuthCredential credential = ProviderUtils.getAuthCredential(response);


            getAuth().signInWithCredential(credential)
                    .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                        @Override
                        public Task<AuthResult> then(@NonNull Task<AuthResult> task) {
                            final AuthResult result = task.getResult();
                            if (mRequestedSignInCredential == null) {
                                return Tasks.forResult(result);
                            } else {
                                return result.getUser()
                                        .linkWithCredential(mRequestedSignInCredential)
                                        .continueWith(new Continuation<AuthResult, AuthResult>() {
                                            @Override
                                            public AuthResult then(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    return task.getResult();
                                                } else {
                                                    // Since we've already signed in, it's too late
                                                    // to backtrack so we just ignore any errors.
                                                    return result;
                                                }
                                            }
                                        });
                            }
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                handleSuccess(response, task.getResult());
                            } else {
                                setResult(Resource.<IdpResponse>forFailure(task.getException()));
                            }
                        }
                    });

    }
}
