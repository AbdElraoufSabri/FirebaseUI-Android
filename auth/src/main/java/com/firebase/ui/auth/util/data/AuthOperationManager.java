package com.firebase.ui.auth.util.data;

import com.firebase.ui.auth.data.model.FlowParameters;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.*;

/**
 * Utilities to help with Anonymous user upgrade.
 */
public class AuthOperationManager {

    private static String firebaseAppName = "FUIScratchApp";

    private static AuthOperationManager mAuthManager;

    @VisibleForTesting
    public FirebaseAuth mScratchAuth;

    private AuthOperationManager() {}

    public static synchronized AuthOperationManager getInstance() {
        if (mAuthManager == null) {
            mAuthManager = new AuthOperationManager();
        }
        return mAuthManager;
    }

    private FirebaseApp getScratchApp(FirebaseApp defaultApp) {
        try {
            return FirebaseApp.getInstance(firebaseAppName);
        } catch (IllegalStateException e) {
            return FirebaseApp.initializeApp(defaultApp.getApplicationContext(),
                    defaultApp.getOptions(), firebaseAppName);
        }
    }

    private FirebaseAuth getScratchAuth(FlowParameters flowParameters) {
        // Use a different FirebaseApp so that the anonymous user state is not lost in our
        // original FirebaseAuth instance.
        if (mScratchAuth == null) {
            FirebaseApp app = FirebaseApp.getInstance(flowParameters.appName);
            mScratchAuth = FirebaseAuth.getInstance(getScratchApp(app));
        }
        return mScratchAuth;
    }

    public Task<AuthResult> createOrLinkUserWithEmailAndPassword(@NonNull FirebaseAuth auth,
                                                                 @NonNull String email,
                                                                 @NonNull String password) {
             return auth.createUserWithEmailAndPassword(email, password);

    }

    public Task<AuthResult> signInAndLinkWithCredential(@NonNull FirebaseAuth auth,
                                                        @NonNull AuthCredential credential) {
            return auth.signInWithCredential(credential);
    }


    @NonNull
    public Task<AuthResult> validateCredential(AuthCredential credential,
                                               FlowParameters flowParameters) {
        return getScratchAuth(flowParameters).signInWithCredential(credential);
    }

    public Task<AuthResult> safeLink(final AuthCredential credential,
                                     final AuthCredential credentialToLink,
                                     final FlowParameters flowParameters) {
        return getScratchAuth(flowParameters)
                .signInWithCredential(credential)
                .continueWithTask(new Continuation<AuthResult, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<AuthResult> task) throws Exception {
                        if (task.isSuccessful()) {
                            return task.getResult().getUser().linkWithCredential(credentialToLink);
                        }
                        return task;
                    }
                });
    }
}
