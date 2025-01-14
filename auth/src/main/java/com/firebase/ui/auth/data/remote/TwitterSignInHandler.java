package com.firebase.ui.auth.data.remote;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.firebase.ui.auth.*;
import com.firebase.ui.auth.IdentityProviderResponse;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.ui.HelperActivityBase;
import com.firebase.ui.auth.util.data.ProviderAvailability;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import androidx.annotation.*;

public class TwitterSignInHandler extends ProviderSignInBase<Void> {
    static {
        if (ProviderAvailability.IS_TWITTER_AVAILABLE) {
            Context context = AuthUI.getApplicationContext();
            Twitter.initialize(new TwitterConfig.Builder(context)
                    .twitterAuthConfig(new TwitterAuthConfig(
                            context.getString(R.string.twitter_consumer_key),
                            context.getString(R.string.twitter_consumer_secret)))
                    .build());
        }
    }

    private final TwitterAuthClient mClient;
    private final TwitterSessionResult mCallback = new TwitterSessionResult();

    public TwitterSignInHandler(Application application) {
        super(application);
        mClient = new TwitterAuthClient();
    }

    public static void initializeTwitter() {
        // This method is intentionally empty, but calling it forces the static {} block of this
        // class to be executed (if it wasn't already).
        //
        // Even though it's currently safe to initialize Twitter more than once, this protects
        // against a future behavior change and gives a small efficiency gain.
    }

    private static IdentityProviderResponse createIdpResponse(
            TwitterSession session, String email, String name, Uri photoUri) {
        return new IdentityProviderResponse.Builder(
                new User.Builder(TwitterAuthProvider.PROVIDER_ID, email)
                        .setName(name)
                        .setPhotoUri(photoUri)
                        .build())
                .setToken(session.getAuthToken().token)
                .setSecret(session.getAuthToken().secret)
                .build();
    }

    @Override
    public void startSignIn(@NonNull HelperActivityBase activity) {
        mClient.authorize(activity, mCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mClient.onActivityResult(requestCode, resultCode, data);
    }

    private class TwitterSessionResult extends Callback<TwitterSession> {
        @Override
        public void success(final Result<TwitterSession> sessionResult) {
            setResult(Resource.<IdentityProviderResponse>forLoading());
            TwitterCore.getInstance()
                    .getApiClient()
                    .getAccountService()
                    .verifyCredentials(false, false, true)
                    .enqueue(new Callback<com.twitter.sdk.android.core.models.User>() {
                        @Override
                        public void success(Result<com.twitter.sdk.android.core.models.User> result) {
                            com.twitter.sdk.android.core.models.User user = result.data;
                            setResult(Resource.forSuccess(createIdpResponse(
                                    sessionResult.data,
                                    user.email,
                                    user.name,
                                    Uri.parse(user.profileImageUrlHttps))));
                        }

                        @Override
                        public void failure(TwitterException e) {
                            setResult(Resource.<IdentityProviderResponse>forFailure(new FirebaseUiException(
                                    ErrorCodes.PROVIDER_ERROR, e)));
                        }
                    });
        }

        @Override
        public void failure(TwitterException e) {
            setResult(Resource.<IdentityProviderResponse>forFailure(new FirebaseUiException(
                    ErrorCodes.PROVIDER_ERROR, e)));
        }
    }
}
