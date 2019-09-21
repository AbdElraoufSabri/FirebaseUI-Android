/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.login.LoginManager;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.util.CredentialUtils;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.GoogleApiUtils;
import com.firebase.ui.auth.util.Preconditions;
import com.firebase.ui.auth.util.data.PhoneNumberUtils;
import com.firebase.ui.auth.util.data.ProviderAvailability;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.twitter.sdk.android.core.TwitterCore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.*;

/**
 * The entry point to the AuthUI authentication flow, and related utility methods. If your
 * application uses the default {@link FirebaseApp} instance, an AuthUI instance can be retrieved
 * simply by calling {@link AuthUI#getInstance()}. If an alternative app instance is in use, call
 * {@link AuthUI#getInstance(FirebaseApp)} instead, passing the appropriate app instance.
 * <p>
 * <p>
 * See the
 * <a href="https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#table-of-contents">README</a>
 * for examples on how to get started with FirebaseUI Auth.
 */
public final class AuthUI {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String TAG = "AuthUI";

    /**
     * The set of authentication providers supported in Firebase Auth UI.
     */
    public static final Set<String> SUPPORTED_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    GoogleAuthProvider.PROVIDER_ID,
                    FacebookAuthProvider.PROVIDER_ID,
                    TwitterAuthProvider.PROVIDER_ID,
                    EmailAuthProvider.PROVIDER_ID
            )));

    /**
     * The set of social authentication providers supported in Firebase Auth UI.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Set<String> SOCIAL_PROVIDERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    GoogleAuthProvider.PROVIDER_ID,
                    FacebookAuthProvider.PROVIDER_ID,
                    TwitterAuthProvider.PROVIDER_ID)));

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String UNCONFIGURED_CONFIG_VALUE = "CHANGE-ME";

    private static final IdentityHashMap<FirebaseApp, AuthUI> INSTANCES = new IdentityHashMap<>();

    private static Context sApplicationContext;

    private final FirebaseApp mApp;
    private final FirebaseAuth mAuth;

    private AuthUI(FirebaseApp app) {
        mApp = app;
        mAuth = FirebaseAuth.getInstance(mApp);

        try {
            mAuth.setFirebaseUIVersion(BuildConfig.VERSION_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't set the FUI version.", e);
        }
        mAuth.useAppLanguage();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static Context getApplicationContext() {
        return sApplicationContext;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void setApplicationContext(@NonNull Context context) {
        sApplicationContext = Preconditions.checkNotNull(context, "App context cannot be null.")
                .getApplicationContext();
    }

    /**
     * Retrieves the {@link AuthUI} instance associated with the default app, as returned by {@code
     * FirebaseApp.getInstance()}.
     *
     * @throws IllegalStateException if the default app is not initialized.
     */
    @NonNull
    public static AuthUI getInstance() {
        return getInstance(FirebaseApp.getInstance());
    }

    /**
     * Retrieves the {@link AuthUI} instance associated the the specified app.
     */
    @NonNull
    public static AuthUI getInstance(@NonNull FirebaseApp app) {
        AuthUI authUi;
        synchronized (INSTANCES) {
            authUi = INSTANCES.get(app);
            if (authUi == null) {
                authUi = new AuthUI(app);
                INSTANCES.put(app, authUi);
            }
        }
        return authUi;
    }

    /**
     * Returns true if AuthUI can handle the intent.
     * <p>
     * AuthUI handle the intent when the embedded data is an email link. If it is, you can then
     * specify the link in {@link SignInIntentBuilder#setEmailLink(String)} before starting AuthUI
     * and it will be handled immediately.
     */
    public static boolean canHandleIntent(@NonNull Intent intent) {
        if (intent == null || intent.getData() == null) {
            return false;
        }
        String link = intent.getData().toString();
        return FirebaseAuth.getInstance().isSignInWithEmailLink(link);
    }

    /**
     * Default theme
     */
    @StyleRes
    public static int getDefaultTheme() {
        return R.style.FirebaseUI;
    }

    /**
     * Make a list of {@link Credential} from a FirebaseUser. Useful for deleting Credentials, not
     * for saving since we don't have access to the password.
     */
    private static List<Credential> getCredentialsFromFirebaseUser(@NonNull FirebaseUser user) {
        if (TextUtils.isEmpty(user.getEmail()) && TextUtils.isEmpty(user.getPhoneNumber())) {
            return Collections.emptyList();
        }

        List<Credential> credentials = new ArrayList<>();
        for (UserInfo userInfo : user.getProviderData()) {
            if (FirebaseAuthProvider.PROVIDER_ID.equals(userInfo.getProviderId())) {
                continue;
            }

            String type = ProviderUtils.providerIdToAccountType(userInfo.getProviderId());
            if (type == null) {
                // Since the account type is null, we've got an email credential. Adding a fake
                // password is the only way to tell Smart Lock that this is an email credential.
                credentials.add(CredentialUtils.buildCredentialOrThrow(user, "pass", null));
            } else {
                credentials.add(CredentialUtils.buildCredentialOrThrow(user, null, type));
            }
        }

        return credentials;
    }

    /**
     * Signs the user in without any UI if possible. If this operation fails, you can safely start a
     * UI-based sign-in flow knowing it is required.
     *
     * @param context requesting the user be signed in
     * @param configs to use for silent sign in. Only Google and email are currently supported, the
     *                rest will be ignored.
     * @return a task which indicates whether or not the user was successfully signed in.
     */
    @NonNull
    public Task<AuthResult> silentSignIn(@NonNull Context context,
                                         @NonNull List<IdpConfig> configs) {
        if (mAuth.getCurrentUser() != null) {
            throw new IllegalArgumentException("User already signed in!");
        }

        final Context appContext = context.getApplicationContext();
        final IdpConfig google =
                ProviderUtils.getConfigFromIdps(configs, GoogleAuthProvider.PROVIDER_ID);
        final IdpConfig email =
                ProviderUtils.getConfigFromIdps(configs, EmailAuthProvider.PROVIDER_ID);

        if (google == null && email == null) {
            throw new IllegalArgumentException("No supported providers were supplied. " +
                    "Add either Google or email support.");
        }

        final GoogleSignInOptions googleOptions;
        if (google == null) {
            googleOptions = null;
        } else {
            GoogleSignInAccount last = GoogleSignIn.getLastSignedInAccount(appContext);
            if (last != null && last.getIdToken() != null) {
                return mAuth.signInWithCredential(GoogleAuthProvider.getCredential(
                        last.getIdToken(), null));
            }

            googleOptions = google.getParams()
                    .getParcelable(ExtraConstants.GOOGLE_SIGN_IN_OPTIONS);
        }

        return GoogleApiUtils.getCredentialsClient(context)
                .request(new CredentialRequest.Builder()
                        // We can support both email and Google at the same time here because they
                        // are mutually exclusive. If a user signs in with Google, their email
                        // account will automatically be upgraded (a.k.a. replaced) with the Google
                        // one, meaning Smart Lock won't have to show the picker UI.
                        .setPasswordLoginSupported(email != null)
                        .setAccountTypes(google == null ? null :
                                ProviderUtils.providerIdToAccountType(GoogleAuthProvider
                                        .PROVIDER_ID))
                        .build())
                .continueWithTask(new Continuation<CredentialRequestResponse, Task<AuthResult>>() {
                    @Override
                    public Task<AuthResult> then(@NonNull Task<CredentialRequestResponse> task) {
                        Credential credential = task.getResult().getCredential();
                        String email = credential.getId();
                        String password = credential.getPassword();

                        if (TextUtils.isEmpty(password)) {
                            return GoogleSignIn.getClient(appContext,
                                    new GoogleSignInOptions.Builder(googleOptions)
                                            .setAccountName(email)
                                            .build())
                                    .silentSignIn()
                                    .continueWithTask(new Continuation<GoogleSignInAccount,
                                            Task<AuthResult>>() {
                                        @Override
                                        public Task<AuthResult> then(
                                                @NonNull Task<GoogleSignInAccount> task) {
                                            AuthCredential authCredential = GoogleAuthProvider
                                                    .getCredential(
                                                            task.getResult().getIdToken(), null);
                                            return mAuth.signInWithCredential(authCredential);
                                        }
                                    });
                        } else {
                            return mAuth.signInWithEmailAndPassword(email, password);
                        }
                    }
                });
    }

    /**
     * Signs the current user out, if one is signed in.
     *
     * @param context the context requesting the user be signed out
     * @return A task which, upon completion, signals that the user has been signed out ({@link
     * Task#isSuccessful()}, or that the sign-out attempt failed unexpectedly !{@link
     * Task#isSuccessful()}).
     */
    @NonNull
    public Task<Void> signOut(@NonNull Context context) {
        Task<Void> maybeDisableAutoSignIn = GoogleApiUtils.getCredentialsClient(context)
                .disableAutoSignIn()
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(@NonNull Task<Void> task) {
                        // We want to ignore a specific exception, since it's not a good reason
                        // to fail (see Issue 1156).
                        Exception e = task.getException();
                        if (e instanceof ApiException
                                && ((ApiException) e).getStatusCode() == CommonStatusCodes
                                .CANCELED) {
                            Log.w(TAG, "Could not disable auto-sign in, maybe there are no " +
                                    "SmartLock accounts available?", e);
                            return null;
                        }

                        return task.getResult();
                    }
                });

        return Tasks.whenAll(
                signOutIdps(context),
                maybeDisableAutoSignIn
        ).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(@NonNull Task<Void> task) {
                task.getResult(); // Propagate exceptions
                mAuth.signOut();
                return null;
            }
        });
    }

    /**
     * Delete the use from FirebaseAuth and delete any associated credentials from the Credentials
     * API. Returns a {@link Task} that succeeds if the Firebase Auth user deletion succeeds and
     * fails if the Firebase Auth deletion fails. Credentials deletion failures are handled
     * silently.
     *
     * @param context the calling {@link Context}.
     */
    @NonNull
    public Task<Void> delete(@NonNull Context context) {
        final FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new FirebaseAuthInvalidUserException(
                    String.valueOf(CommonStatusCodes.SIGN_IN_REQUIRED),
                    "No currently signed in user."));
        }

        final List<Credential> credentials = getCredentialsFromFirebaseUser(currentUser);
        final CredentialsClient client = GoogleApiUtils.getCredentialsClient(context);

        // Ensure the order in which tasks are executed properly destructures the user.
        return signOutIdps(context).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) {
                task.getResult(); // Propagate exception if there was one

                List<Task<?>> credentialTasks = new ArrayList<>();
                for (Credential credential : credentials) {
                    credentialTasks.add(client.delete(credential));
                }
                return Tasks.whenAll(credentialTasks)
                        .continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(@NonNull Task<Void> task) {
                                Exception e = task.getException();
                                Throwable t = e == null ? null : e.getCause();
                                if (!(t instanceof ApiException)
                                        || ((ApiException) t).getStatusCode() !=
                                        CommonStatusCodes.CANCELED) {
                                    // Only propagate the exception if it isn't an invalid account
                                    // one. This can occur if we failed to save the credential or it
                                    // was deleted elsewhere. However, a lack of stored credential
                                    // doesn't mean fully deleting the user failed.
                                    return task.getResult();
                                }

                                return null;
                            }
                        });
            }
        }).continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(@NonNull Task<Void> task) {
                task.getResult(); // Propagate exception if there was one
                return currentUser.delete();
            }
        });
    }

    private Task<Void> signOutIdps(@NonNull Context context) {
        if (ProviderAvailability.IS_FACEBOOK_AVAILABLE) {
            LoginManager.getInstance().logOut();
        }
        if (ProviderAvailability.IS_TWITTER_AVAILABLE) {
            TwitterSignInHandler.initializeTwitter();
            TwitterCore.getInstance().getSessionManager().clearActiveSession();
        }
        return GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
    }

    /**
     * Starts the process of creating a sign in intent, with the mandatory application context
     * parameter.
     */
    @NonNull
    public SignInIntentBuilder createSignInIntentBuilder() {
        return new SignInIntentBuilder();
    }

    @StringDef({
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID,
            TwitterAuthProvider.PROVIDER_ID,
            EmailAuthProvider.PROVIDER_ID
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportedProvider {
    }

    /**
     * Configuration for an identity provider.
     */
    public static final class IdpConfig implements Parcelable {
        public static final Creator<IdpConfig> CREATOR = new Creator<IdpConfig>() {
            @Override
            public IdpConfig createFromParcel(Parcel in) {
                return new IdpConfig(in);
            }

            @Override
            public IdpConfig[] newArray(int size) {
                return new IdpConfig[size];
            }
        };

        private final String mProviderId;
        private final Bundle mParams;

        private IdpConfig(
                @SupportedProvider @NonNull String providerId,
                @NonNull Bundle params) {
            mProviderId = providerId;
            mParams = new Bundle(params);
        }

        private IdpConfig(Parcel in) {
            mProviderId = in.readString();
            mParams = in.readBundle(getClass().getClassLoader());
        }

        @NonNull
        @SupportedProvider
        public String getProviderId() {
            return mProviderId;
        }

        /**
         * @return provider-specific options
         */
        @NonNull
        public Bundle getParams() {
            return new Bundle(mParams);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(mProviderId);
            parcel.writeBundle(mParams);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IdpConfig config = (IdpConfig) o;

            return mProviderId.equals(config.mProviderId);
        }

        @Override
        public final int hashCode() {
            return mProviderId.hashCode();
        }

        @Override
        public String toString() {
            return "IdpConfig{" +
                    "mProviderId='" + mProviderId + '\'' +
                    ", mParams=" + mParams +
                    '}';
        }

        /**
         * Base builder for all authentication providers.
         *
         * @see SignInIntentBuilder#setAvailableProviders(List)
         */
        public static class Builder {
            private final Bundle mParams = new Bundle();
            @SupportedProvider private String mProviderId;

            protected Builder(@SupportedProvider @NonNull String providerId) {
                if (!SUPPORTED_PROVIDERS.contains(providerId)) {
                    throw new IllegalArgumentException("Unknown provider: " + providerId);
                }
                mProviderId = providerId;
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @NonNull
            protected final Bundle getParams() {
                return mParams;
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            protected void setProviderId(@NonNull String providerId) {
                mProviderId = providerId;
            }

            @CallSuper
            @NonNull
            public IdpConfig build() {
                return new IdpConfig(mProviderId, mParams);
            }
        }

        /**
         * {@link IdpConfig} builder for the email provider.
         */
        public static final class EmailBuilder extends Builder {
            public EmailBuilder() {
                super(EmailAuthProvider.PROVIDER_ID);
            }

            /**
             * Enables or disables creating new accounts in the email sign in flows.
             * <p>
             * Account creation is enabled by default.
             */
            @NonNull
            public EmailBuilder setAllowNewAccounts(boolean allow) {
                getParams().putBoolean(ExtraConstants.ALLOW_NEW_EMAILS, allow);
                return this;
            }

            /**
             * Configures the requirement for the user to enter first and last name in the email
             * sign up flow.
             * <p>
             * Name is required by default.
             */
            @NonNull
            public EmailBuilder setRequireName(boolean requireName) {
                getParams().putBoolean(ExtraConstants.REQUIRE_NAME, requireName);
                return this;
            }


            /**
             * Sets the {@link ActionCodeSettings} object to be used for email link sign in.
             * <p>
             * {@link ActionCodeSettings#canHandleCodeInApp()} must be set to true, and a valid
             * continueUrl must be passed via {@link ActionCodeSettings.Builder#setUrl(String)}.
             * This URL must be whitelisted in the Firebase Console.
             *
             * @throws IllegalStateException if canHandleCodeInApp is set to false
             * @throws NullPointerException if ActionCodeSettings is null
             */
            @NonNull
            public EmailBuilder setActionCodeSettings(ActionCodeSettings actionCodeSettings) {
                getParams().putParcelable(ExtraConstants.ACTION_CODE_SETTINGS, actionCodeSettings);
                return this;
            }

            @Override
            public IdpConfig build() {
                return super.build();
            }
        }

        /**
         * {@link IdpConfig} builder for the Google provider.
         */
        public static final class GoogleBuilder extends Builder {
            public GoogleBuilder() {
                super(GoogleAuthProvider.PROVIDER_ID);
                Preconditions.checkConfigured(getApplicationContext(),
                        "Check your google-services plugin configuration, the" +
                                " default_web_client_id string wasn't populated.",
                        R.string.default_web_client_id);
            }

            /**
             * Set the scopes that your app will request when using Google sign-in. See all <a
             * href="https://developers.google.com/identity/protocols/googlescopes">available
             * scopes</a>.
             *
             * @param scopes additional scopes to be requested
             */
            @NonNull
            public GoogleBuilder setScopes(@NonNull List<String> scopes) {
                GoogleSignInOptions.Builder builder =
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
                for (String scope : scopes) {
                    builder.requestScopes(new Scope(scope));
                }
                return setSignInOptions(builder.build());
            }

            /**
             * Set the {@link GoogleSignInOptions} to be used for Google sign-in. Standard
             * options like requesting the user's email will automatically be added.
             *
             * @param options sign-in options
             */
            @NonNull
            public GoogleBuilder setSignInOptions(@NonNull GoogleSignInOptions options) {
                Preconditions.checkUnset(getParams(),
                        "Cannot overwrite previously set sign-in options.",
                        ExtraConstants.GOOGLE_SIGN_IN_OPTIONS);

                GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(options);
                builder.requestEmail().requestIdToken(getApplicationContext()
                        .getString(R.string.default_web_client_id));
                getParams().putParcelable(
                        ExtraConstants.GOOGLE_SIGN_IN_OPTIONS, builder.build());

                return this;
            }

            @NonNull
            @Override
            public IdpConfig build() {
                if (!getParams().containsKey(ExtraConstants.GOOGLE_SIGN_IN_OPTIONS)) {
                    setScopes(Collections.<String>emptyList());
                }

                return super.build();
            }
        }

        /**
         * {@link IdpConfig} builder for the Facebook provider.
         */
        public static final class FacebookBuilder extends Builder {
            private static final String TAG = "FacebookBuilder";

            public FacebookBuilder() {
                super(FacebookAuthProvider.PROVIDER_ID);
                if (!ProviderAvailability.IS_FACEBOOK_AVAILABLE) {
                    throw new RuntimeException(
                            "Facebook provider cannot be configured " +
                                    "without dependency. Did you forget to add " +
                                    "'com.facebook.android:facebook-login:VERSION' dependency?");
                }
                Preconditions.checkConfigured(getApplicationContext(),
                        "Facebook provider unconfigured. Make sure to add a" +
                                " `facebook_application_id` string. See the docs for more info:" +
                                " https://github" +
                                ".com/firebase/FirebaseUI-Android/blob/master/auth/README" +
                                ".md#facebook",
                        R.string.facebook_application_id);
                if (getApplicationContext().getString(R.string.facebook_login_protocol_scheme)
                        .equals("fbYOUR_APP_ID")) {
                    Log.w(TAG, "Facebook provider unconfigured for Chrome Custom Tabs.");
                }
            }

            /**
             * Specifies the additional permissions that the application will request in the
             * Facebook Login SDK. Available permissions can be found <a
             * href="https://developers.facebook.com/docs/facebook-login/permissions">here</a>.
             */
            @NonNull
            public FacebookBuilder setPermissions(@NonNull List<String> permissions) {
                getParams().putStringArrayList(
                        ExtraConstants.FACEBOOK_PERMISSIONS, new ArrayList<>(permissions));
                return this;
            }
        }

        /**
         * {@link IdpConfig} builder for the Twitter provider.
         */
        public static final class TwitterBuilder extends Builder {
            public TwitterBuilder() {
                super(TwitterAuthProvider.PROVIDER_ID);
                if (!ProviderAvailability.IS_TWITTER_AVAILABLE) {
                    throw new RuntimeException(
                            "Twitter provider cannot be configured " +
                                    "without dependency. Did you forget to add " +
                                    "'com.twitter.sdk.android:twitter-core:VERSION' dependency?");
                }
                Preconditions.checkConfigured(getApplicationContext(),
                        "Twitter provider unconfigured. Make sure to add your key and secret." +
                                " See the docs for more info:" +
                                " https://github" +
                                ".com/firebase/FirebaseUI-Android/blob/master/auth/README" +
                                ".md#twitter",
                        R.string.twitter_consumer_key,
                        R.string.twitter_consumer_secret);
            }
        }

    }

    /**
     * Base builder for both {@link SignInIntentBuilder}.
     */
    @SuppressWarnings(value = "unchecked")
    private abstract class AuthIntentBuilder<T extends AuthIntentBuilder> {
        final List<IdpConfig> mProviders = new ArrayList<>();

        String mTosUrl;
        String mPrivacyPolicyUrl;

        boolean mAlwaysShowProviderChoice = false;
        boolean mEnableCredentials = true;
        boolean mEnableHints = true;

        @NonNull
        public T setTosAndPrivacyPolicyUrls(@NonNull String tosUrl,
                                            @NonNull String privacyPolicyUrl) {
            Preconditions.checkNotNull(tosUrl, "tosUrl cannot be null");
            Preconditions.checkNotNull(privacyPolicyUrl, "privacyPolicyUrl cannot be null");
            mTosUrl = tosUrl;
            mPrivacyPolicyUrl = privacyPolicyUrl;
            return (T) this;
        }

        /**
         * Specified the set of supported authentication providers. At least one provider must
         * be specified. There may only be one instance of each provider. Anonymous provider cannot
         * be the only provider specified.
         * <p>
         * <p>If no providers are explicitly specified by calling this method, then the email
         * provider is the default supported provider.
         *
         * @param idpConfigs a list of {@link IdpConfig}s, where each {@link IdpConfig} contains the
         *                   configuration parameters for the IDP.
         * @see IdpConfig
         *
         * @throws IllegalStateException if anonymous provider is the only specified provider.
         */
        @NonNull
        public T setAvailableProviders(@NonNull List<IdpConfig> idpConfigs) {
            Preconditions.checkNotNull(idpConfigs, "idpConfigs cannot be null");

            mProviders.clear();

            for (IdpConfig config : idpConfigs) {
                if (mProviders.contains(config)) {
                    throw new IllegalArgumentException("Each provider can only be set once. "
                            + config.getProviderId()
                            + " was set twice.");
                } else {
                    mProviders.add(config);
                }
            }

            return (T) this;
        }

        /**
         * Enables or disables the use of Smart Lock for Passwords in the sign in flow. To
         * (en)disable hint selector and credential selector independently use {@link
         * #setIsSmartLockEnabled(boolean, boolean)}
         * <p>
         * <p>SmartLock is enabled by default.
         *
         * @param enabled enables smartlock's credential selector and hint selector
         */
        @NonNull
        public T setIsSmartLockEnabled(boolean enabled) {
            return setIsSmartLockEnabled(enabled, enabled);
        }

        /**
         * Enables or disables the use of Smart Lock for Passwords credential selector and hint
         * selector.
         * <p>
         * <p>Both selectors are enabled by default.
         *
         * @param enableCredentials enables credential selector before signup
         * @param enableHints       enable hint selector in respective signup screens
         */
        @NonNull
        public T setIsSmartLockEnabled(boolean enableCredentials, boolean enableHints) {
            mEnableCredentials = enableCredentials;
            mEnableHints = enableHints;
            return (T) this;
        }

        /**
         * Forces the sign-in method choice screen to always show, even if there is only
         * a single provider configured.
         * <p>
         * <p>This is false by default.
         *
         * @param alwaysShow if true, force the sign-in choice screen to show.
         */
        @NonNull
        public T setAlwaysShowSignInMethodScreen(boolean alwaysShow) {
            mAlwaysShowProviderChoice = alwaysShow;
            return (T) this;
        }

        @CallSuper
        @NonNull
        public Intent build() {
            if (mProviders.isEmpty()) {
                mProviders.add(new IdpConfig.EmailBuilder().build());
            }

            return KickoffActivity.createIntent(mApp.getApplicationContext(), getFlowParams());
        }

        protected abstract FlowParameters getFlowParams();
    }

    /**
     * Builder for the intent to start the user authentication flow.
     */
    public final class SignInIntentBuilder extends AuthIntentBuilder<SignInIntentBuilder> {

        private String mEmailLink;
        private boolean mEnableAnonymousUpgrade;

        private SignInIntentBuilder() {
            super();
        }

        /**
         * Specifies the email link to be used for sign in. When set, a sign in attempt will be
         * made immediately.
         */
        @NonNull
        public SignInIntentBuilder setEmailLink(@NonNull final String emailLink) {
            mEmailLink = emailLink;
            return this;
        }


        @Override
        protected FlowParameters getFlowParams() {
            return new FlowParameters(
                    mApp.getName(),
                    mProviders,
                    mTosUrl,
                    mPrivacyPolicyUrl,
                    mEnableCredentials,
                    mEnableHints,
                    mAlwaysShowProviderChoice);
        }
    }
}
