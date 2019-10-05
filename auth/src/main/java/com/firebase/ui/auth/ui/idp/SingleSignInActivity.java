package com.firebase.ui.auth.ui.idp;

import android.content.*;
import android.os.Bundle;

import com.firebase.ui.auth.*;
import com.firebase.ui.auth.data.model.*;
import com.firebase.ui.auth.data.remote.*;
import com.firebase.ui.auth.ui.InvisibleActivityBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.*;
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler;
import com.google.firebase.auth.*;

import androidx.annotation.*;
import androidx.lifecycle.*;

public class SingleSignInActivity extends InvisibleActivityBase {
    private SocialProviderResponseHandler mHandler;
    private ProviderSignInBase<?> mProvider;

    public static Intent createIntent(Context context, FlowParameters flowParams, User user) {
        return createBaseIntent(context, SingleSignInActivity.class, flowParams)
                .putExtra(ExtraConstants.USER, user);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = User.getUser(getIntent());
        String provider = user.getProviderId();

        AuthUI.IdentityProviderConfig providerConfig =
                ProviderUtils.getConfigFromIdps(getFlowParams().providers, provider);
        if (providerConfig == null) {
            finish(RESULT_CANCELED, IdentityProviderResponse.getErrorIntent(new FirebaseUiException(
                    ErrorCodes.DEVELOPER_ERROR,
                    "Provider not enabled: " + provider)));
            return;
        }

        ViewModelProvider supplier = ViewModelProviders.of(this);

        mHandler = supplier.get(SocialProviderResponseHandler.class);
        mHandler.init(getFlowParams());

        switch (provider) {
            case GoogleAuthProvider.PROVIDER_ID:
                GoogleSignInHandler google = supplier.get(GoogleSignInHandler.class);
                google.init(new GoogleSignInHandler.Params(providerConfig, user.getEmail()));
                mProvider = google;
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                FacebookSignInHandler facebook = supplier.get(FacebookSignInHandler.class);
                facebook.init(providerConfig);
                mProvider = facebook;
                break;
            case TwitterAuthProvider.PROVIDER_ID:
                TwitterSignInHandler twitter = supplier.get(TwitterSignInHandler.class);
                twitter.init(null);
                mProvider = twitter;
                break;
            default:
                throw new IllegalStateException("Invalid provider id: " + provider);
        }

        mProvider.getOperation().observe(this, new ResourceObserver<IdentityProviderResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdentityProviderResponse response) {
                mHandler.startSignIn(response);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                mHandler.startSignIn(IdentityProviderResponse.from(e));
            }
        });

        mHandler.getOperation().observe(this, new ResourceObserver<IdentityProviderResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdentityProviderResponse response) {
                startSaveCredentials(mHandler.getCurrentUser(), response, null);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                finish(RESULT_CANCELED, IdentityProviderResponse.getErrorIntent(e));
            }
        });

        if (mHandler.getOperation().getValue() == null) {
            mProvider.startSignIn(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mHandler.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }
}
