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

package com.firebase.ui.auth.ui.idp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.*;
import com.firebase.ui.auth.IdentityProviderResponse;
import com.firebase.ui.auth.R;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.data.remote.FacebookSignInHandler;
import com.firebase.ui.auth.data.remote.GoogleSignInHandler;
import com.firebase.ui.auth.data.remote.TwitterSignInHandler;
import com.firebase.ui.auth.ui.AppCompatBase;
import com.firebase.ui.auth.util.ExtraConstants;
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils;
import com.firebase.ui.auth.util.data.ProviderUtils;
import com.firebase.ui.auth.viewmodel.ProviderSignInBase;
import com.firebase.ui.auth.viewmodel.ResourceObserver;
import com.firebase.ui.auth.viewmodel.idp.LinkingSocialProviderResponseHandler;
import com.google.firebase.auth.*;

import androidx.annotation.*;
import androidx.lifecycle.*;

public class WelcomeBackIdpPrompt extends AppCompatBase {
    private ProviderSignInBase<?> mProvider;

    private Button mDoneButton;
    private ProgressBar mProgressBar;

    public static Intent createIntent(
            Context context, FlowParameters flowParams, User existingUser) {
        return createIntent(context, flowParams, existingUser, null);
    }

    public static Intent createIntent(
            Context context,
            FlowParameters flowParams,
            User existingUser,
            @Nullable IdentityProviderResponse requestedUserResponse) {

        return createBaseIntent(context, WelcomeBackIdpPrompt.class, flowParams)
                .putExtra(ExtraConstants.IDP_RESPONSE, requestedUserResponse)
                .putExtra(ExtraConstants.USER, existingUser);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fui_welcome_back_idp_prompt_layout);

        mDoneButton = findViewById(R.id.welcome_back_idp_button);
        mProgressBar = findViewById(R.id.top_progress_bar);

        User user = User.getUser(getIntent());

        IdentityProviderResponse requestedUserResponse = IdentityProviderResponse.fromResultIntent(getIntent());
        ViewModelProvider supplier = ViewModelProviders.of(this);

        final LinkingSocialProviderResponseHandler handler =
                supplier.get(LinkingSocialProviderResponseHandler.class);

        handler.init(getFlowParams());
        if (requestedUserResponse != null) {
            AuthCredential credential = ProviderUtils.getAuthCredential(requestedUserResponse);
            handler.setRequestedSignInCredentialForEmail(credential, user.getEmail());
        }

        String providerId = user.getProviderId();
        AuthUI.IdentityProviderConfig config =
                ProviderUtils.getConfigFromIdps(getFlowParams().providers, providerId);
        if (config == null) {
            finish(RESULT_CANCELED, IdentityProviderResponse.getErrorIntent(new FirebaseUiException(
                    ErrorCodes.DEVELOPER_ERROR,
                    "Firebase login unsuccessful."
                            + " Account linking failed due to provider not enabled by application: "
                            + providerId)));
            return;
        }

        @StringRes int providerName;
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                GoogleSignInHandler google = supplier.get(GoogleSignInHandler.class);
                google.init(new GoogleSignInHandler.Params(config, user.getEmail()));
                mProvider = google;

                providerName = R.string.fui_idp_name_google;
                break;
            case FacebookAuthProvider.PROVIDER_ID:
                FacebookSignInHandler facebook = supplier.get(FacebookSignInHandler.class);
                facebook.init(config);
                mProvider = facebook;

                providerName = R.string.fui_idp_name_facebook;
                break;
            case TwitterAuthProvider.PROVIDER_ID:
                TwitterSignInHandler twitter = supplier.get(TwitterSignInHandler.class);
                twitter.init(null);
                mProvider = twitter;

                providerName = R.string.fui_idp_name_twitter;
                break;
            default:
                throw new IllegalStateException("Invalid provider id: " + providerId);
        }

        mProvider.getOperation().observe(this, new ResourceObserver<IdentityProviderResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdentityProviderResponse response) {
                handler.startSignIn(response);
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                handler.startSignIn(IdentityProviderResponse.from(e));
            }
        });

        ((TextView) findViewById(R.id.welcome_back_idp_prompt)).setText(getString(
                R.string.fui_welcome_back_idp_prompt,
                user.getEmail(),
                getString(providerName)));

        mDoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mProvider.startSignIn(WelcomeBackIdpPrompt.this);
            }
        });

        handler.getOperation().observe(this, new ResourceObserver<IdentityProviderResponse>(this) {
            @Override
            protected void onSuccess(@NonNull IdentityProviderResponse response) {
                finish(RESULT_OK, response.toIntent());
            }

            @Override
            protected void onFailure(@NonNull Exception e) {
                    finish(RESULT_CANCELED, IdentityProviderResponse.getErrorIntent(e));
            }
        });

        TextView footerText = findViewById(R.id.email_footer_tos_and_pp_text);
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(this, getFlowParams(), footerText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mProvider.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void showProgress(int message) {
        mDoneButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mDoneButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
