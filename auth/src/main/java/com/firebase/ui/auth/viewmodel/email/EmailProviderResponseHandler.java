package com.firebase.ui.auth.viewmodel.email;

import android.app.Application;

import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.*;
import com.firebase.ui.auth.data.remote.ProfileMerger;
import com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt;
import com.firebase.ui.auth.ui.idp.WelcomeBackIdpPrompt;
import com.firebase.ui.auth.util.data.*;
import com.firebase.ui.auth.viewmodel.*;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;

import androidx.annotation.*;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmailProviderResponseHandler extends SignInViewModelBase {
    private static final String TAG = "EmailProviderResponseHandler";

    public EmailProviderResponseHandler(Application application) {
        super(application);
    }

    public void startSignIn(@NonNull final IdpResponse response, @NonNull final String password) {
        if (!response.isSuccessful()) {
            setResult(Resource.<IdpResponse>forFailure(response.getError()));
            return;
        }
        if (!response.getProviderType().equals(EmailAuthProvider.PROVIDER_ID)) {
            throw new IllegalStateException(
                    "This handler can only be used with the email provider");
        }
        setResult(Resource.<IdpResponse>forLoading());

        final AuthOperationManager authOperationManager = AuthOperationManager.getInstance();
        final String email = response.getEmail();
        authOperationManager.createOrLinkUserWithEmailAndPassword(getAuth(), email, password)
                .continueWithTask(new ProfileMerger(response))
                .addOnFailureListener(new TaskFailureLogger(TAG, "Error creating user"))
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        handleSuccess(response, result);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            // Collision with existing user email without anonymous upgrade
                            // it should be very hard for the user to even get to this error
                            // due to CheckEmailFragment.
                            ProviderUtils.fetchTopProvider(getAuth(), getArguments(), email)
                                    .addOnSuccessListener(new StartWelcomeBackFlow(email))
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            setResult(Resource.<IdpResponse>forFailure(e));
                                        }
                                    });

                        } else {
                            setResult(Resource.<IdpResponse>forFailure(e));
                        }
                    }
                });
    }

    private class StartWelcomeBackFlow implements OnSuccessListener<String> {
        private final String mEmail;

        public StartWelcomeBackFlow(String email) {
            mEmail = email;
        }

        @Override
        public void onSuccess(String provider) {
            if (provider == null) {
                throw new IllegalStateException(
                        "User has no providers even though we got a collision.");
            }

            if (EmailAuthProvider.PROVIDER_ID.equalsIgnoreCase(provider)) {
                setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                        WelcomeBackPasswordPrompt.Companion.createIntent(
                                getApplication(),
                                getArguments(),
                                new IdpResponse.Builder(new User.Builder(
                                        EmailAuthProvider.PROVIDER_ID, mEmail).build()
                                ).build()),
                        RequestCodes.WELCOME_BACK_EMAIL_FLOW
                )));
            } else {
                setResult(Resource.<IdpResponse>forFailure(new IntentRequiredException(
                        WelcomeBackIdpPrompt.createIntent(
                                getApplication(),
                                getArguments(),
                                new User.Builder(provider, mEmail).build()),
                        RequestCodes.WELCOME_BACK_IDP_FLOW
                )));
            }
        }
    }
}
