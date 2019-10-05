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

package com.firebase.uidemo.auth;

import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.firebase.ui.auth.*;
import com.firebase.uidemo.R;
import com.firebase.uidemo.util.ConfigurationUtils;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.tasks.*;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.*;

import java.util.*;

import androidx.annotation.*;
import androidx.appcompat.app.*;
import butterknife.*;

public class AuthUiActivity extends AppCompatActivity {
    private static final String TAG = "AuthUiActivity";

    private static final String GOOGLE_TOS_URL = "https://www.google.com/policies/terms/";
    private static final String FIREBASE_TOS_URL = "https://firebase.google.com/terms/";
    private static final String GOOGLE_PRIVACY_POLICY_URL = "https://www.google" +
            ".com/policies/privacy/";
    private static final String FIREBASE_PRIVACY_POLICY_URL = "https://firebase.google" +
            ".com/terms/analytics/#7_privacy";

    private static final int RC_SIGN_IN = 100;

    @BindView(R.id.root) View mRootView;

    @BindView(R.id.google_provider) CheckBox mUseGoogleProvider;
    @BindView(R.id.facebook_provider) CheckBox mUseFacebookProvider;
    @BindView(R.id.twitter_provider) CheckBox mUseTwitterProvider;
    @BindView(R.id.email_provider) CheckBox mUseEmailProvider;

    @BindView(R.id.default_theme) RadioButton mDefaultTheme;
    @BindView(R.id.green_theme) RadioButton mGreenTheme;
    @BindView(R.id.purple_theme) RadioButton mPurpleTheme;
    @BindView(R.id.dark_theme) RadioButton mDarkTheme;

    @BindView(R.id.google_tos_privacy) RadioButton mUseGoogleTosPp;
    @BindView(R.id.firebase_tos_privacy) RadioButton mUseFirebaseTosPp;

    @BindView(R.id.google_scopes_header) TextView mGoogleScopesHeader;
    @BindView(R.id.google_scope_drive_file) CheckBox mGoogleScopeDriveFile;
    @BindView(R.id.google_scope_youtube_data) CheckBox mGoogleScopeYoutubeData;

    @BindView(R.id.facebook_permissions_header) TextView mFacebookPermissionsHeader;
    @BindView(R.id.facebook_permission_friends) CheckBox mFacebookPermissionFriends;
    @BindView(R.id.facebook_permission_photos) CheckBox mFacebookPermissionPhotos;

    @BindView(R.id.credential_selector_enabled) CheckBox mEnableCredentialSelector;
    @BindView(R.id.allow_new_email_accounts) CheckBox mAllowNewEmailAccounts;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, AuthUiActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_ui_layout);
        ButterKnife.bind(this);

        if (ConfigurationUtils.isGoogleMisconfigured(this)) {
            mUseGoogleProvider.setChecked(false);
            mUseGoogleProvider.setEnabled(false);
            mUseGoogleProvider.setText(R.string.google_label_missing_config);
            setGoogleScopesEnabled(false);
        } else {
            setGoogleScopesEnabled(mUseGoogleProvider.isChecked());
            mUseGoogleProvider.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    setGoogleScopesEnabled(checked);
                }
            });
        }

        if (ConfigurationUtils.isFacebookMisconfigured(this)) {
            mUseFacebookProvider.setChecked(false);
            mUseFacebookProvider.setEnabled(false);
            mUseFacebookProvider.setText(R.string.facebook_label_missing_config);
            setFacebookPermissionsEnabled(false);
        } else {
            setFacebookPermissionsEnabled(mUseFacebookProvider.isChecked());
            mUseFacebookProvider.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    setFacebookPermissionsEnabled(checked);
                }
            });
        }

        if (ConfigurationUtils.isTwitterMisconfigured(this)) {
            mUseTwitterProvider.setChecked(false);
            mUseTwitterProvider.setEnabled(false);
            mUseTwitterProvider.setText(R.string.twitter_label_missing_config);
        }

        mUseEmailProvider.setChecked(true);

        if (ConfigurationUtils.isGoogleMisconfigured(this)
                || ConfigurationUtils.isFacebookMisconfigured(this)
                || ConfigurationUtils.isTwitterMisconfigured(this)) {
            showSnackbar(R.string.configuration_required);
        }

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            mDarkTheme.setChecked(true);
        }
    }


    @OnClick(R.id.sign_in)
    public void signIn() {
        startActivityForResult(buildSignInIntent(/*link=*/null), RC_SIGN_IN);
    }

    @NonNull
    public Intent buildSignInIntent(@Nullable String link) {
        AuthUI.SignInIntentBuilder builder = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(getSelectedProviders())
                .setIsSmartLockEnabled(mEnableCredentialSelector.isChecked());

        if (getSelectedTosUrl() != null && getSelectedPrivacyPolicyUrl() != null) {
            builder.setTosAndPrivacyPolicyUrls(
                    getSelectedTosUrl(),
                    getSelectedPrivacyPolicyUrl());
        }

        return builder.build();
    }

    @OnClick(R.id.sign_in_silent)
    public void silentSignIn() {
        AuthUI.getInstance().silentSignIn(this, getSelectedProviders())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            startSignedInActivity(null);
                        } else {
                            showSnackbar(R.string.sign_in_failed);
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && getIntent().getExtras() == null) {
            startSignedInActivity(null);
            finish();
        }
    }

    private void handleSignInResponse(int resultCode, @Nullable Intent data) {
        IdentityProviderResponse response = IdentityProviderResponse.fromResultIntent(data);

        // Successfully signed in
        if (resultCode == RESULT_OK) {
            startSignedInActivity(response);
            finish();
        } else {
            // Sign in failed
            if (response == null) {
                // User pressed back button
                showSnackbar(R.string.sign_in_cancelled);
                return;
            }

            if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar(R.string.no_internet_connection);
                return;
            }

            if (response.getError().getErrorCode() == ErrorCodes.ERROR_USER_DISABLED) {
                showSnackbar(R.string.account_disabled);
                return;
            }

            showSnackbar(R.string.unknown_error);
            Log.e(TAG, "Sign-in error: ", response.getError());
        }
    }

    private void startSignedInActivity(@Nullable IdentityProviderResponse response) {
        startActivity(SignedInActivity.createIntent(this, response));
    }

    @OnClick({R.id.default_theme, R.id.purple_theme, R.id.green_theme, R.id.dark_theme})
    public void toggleDarkTheme() {
        int mode = mDarkTheme.isChecked() ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
        AppCompatDelegate.setDefaultNightMode(mode);
        getDelegate().setLocalNightMode(mode);
    }

    @StyleRes
    private int getSelectedTheme() {
        if (mGreenTheme.isChecked()) {
            return R.style.GreenTheme;
        }

        if (mPurpleTheme.isChecked()) {
            return R.style.PurpleTheme;
        }

        return com.firebase.ui.auth.R.style.FirebaseUI;
    }


    private List<AuthUI.IdentityProviderConfig> getSelectedProviders() {
        List<AuthUI.IdentityProviderConfig> selectedProviders = new ArrayList<>();

        if (mUseGoogleProvider.isChecked()) {
            selectedProviders.add(
                    new AuthUI.IdentityProviderConfig.GoogleBuilder().setScopes(getGoogleScopes()).build());
        }

        if (mUseFacebookProvider.isChecked()) {
            selectedProviders.add(new AuthUI.IdentityProviderConfig.FacebookBuilder()
                    .setPermissions(getFacebookPermissions())
                    .build());
        }

        if (mUseTwitterProvider.isChecked()) {
            selectedProviders.add(new AuthUI.IdentityProviderConfig.TwitterBuilder().build());
        }

        if (mUseEmailProvider.isChecked()) {
            selectedProviders.add(new AuthUI.IdentityProviderConfig.EmailBuilder()
                    .setAllowNewAccounts(mAllowNewEmailAccounts.isChecked())
                    .build());
        }

        return selectedProviders;
    }

    @Nullable
    private String getSelectedTosUrl() {
        if (mUseGoogleTosPp.isChecked()) {
            return GOOGLE_TOS_URL;
        }

        if (mUseFirebaseTosPp.isChecked()) {
            return FIREBASE_TOS_URL;
        }

        return null;
    }

    @Nullable
    private String getSelectedPrivacyPolicyUrl() {
        if (mUseGoogleTosPp.isChecked()) {
            return GOOGLE_PRIVACY_POLICY_URL;
        }

        if (mUseFirebaseTosPp.isChecked()) {
            return FIREBASE_PRIVACY_POLICY_URL;
        }

        return null;
    }

    private void setGoogleScopesEnabled(boolean enabled) {
        mGoogleScopesHeader.setEnabled(enabled);
        mGoogleScopeDriveFile.setEnabled(enabled);
        mGoogleScopeYoutubeData.setEnabled(enabled);
    }

    private void setFacebookPermissionsEnabled(boolean enabled) {
        mFacebookPermissionsHeader.setEnabled(enabled);
        mFacebookPermissionFriends.setEnabled(enabled);
        mFacebookPermissionPhotos.setEnabled(enabled);
    }

    private List<String> getGoogleScopes() {
        List<String> result = new ArrayList<>();
        if (mGoogleScopeYoutubeData.isChecked()) {
            result.add("https://www.googleapis.com/auth/youtube.readonly");
        }
        if (mGoogleScopeDriveFile.isChecked()) {
            result.add(Scopes.DRIVE_FILE);
        }
        return result;
    }

    private List<String> getFacebookPermissions() {
        List<String> result = new ArrayList<>();
        if (mFacebookPermissionFriends.isChecked()) {
            result.add("user_friends");
        }
        if (mFacebookPermissionPhotos.isChecked()) {
            result.add("user_photos");
        }
        return result;
    }

    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
