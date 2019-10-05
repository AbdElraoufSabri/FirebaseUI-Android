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

import android.content.Intent;
import android.os.*;
import android.text.TextUtils;

import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.auth.*;

import java.io.*;

import androidx.annotation.*;

/**
 * A container that encapsulates the result of authenticating with an Identity Provider.
 */
public class IdentityProviderResponse implements Parcelable {
    public static final Creator<IdentityProviderResponse> CREATOR = new Creator<IdentityProviderResponse>() {
        @Override
        public IdentityProviderResponse createFromParcel(Parcel in) {
            return new IdentityProviderResponse(
                    in.readParcelable(User.class.getClassLoader()),
                    in.readString(), // token
                    in.readString(), // secret
                    in.readInt() == 1,
                    (FirebaseUiException) in.readSerializable(),
                    in.readParcelable(AuthCredential.class.getClassLoader())
            );
        }

        @Override
        public IdentityProviderResponse[] newArray(int size) {
            return new IdentityProviderResponse[size];
        }
    };

    private final User mUser;
    private final AuthCredential mPendingCredential;

    private final String mToken;
    private final String mSecret;
    private final boolean mIsNewUser;

    private final FirebaseUiException mException;

    private IdentityProviderResponse(@NonNull FirebaseUiException e) {
        this(null, null, null, false, e, null);
    }

    private IdentityProviderResponse(
            @NonNull User user,
            @Nullable String token,
            @Nullable String secret,
            boolean isNewUser) {
        this(user, token, secret, isNewUser, null, null);
    }

    private IdentityProviderResponse(AuthCredential credential, FirebaseUiException e) {
        this(null, null, null, false, e, credential);
    }

    private IdentityProviderResponse(
            User user,
            String token,
            String secret,
            boolean isNewUser,
            FirebaseUiException e,
            AuthCredential credential) {
        mUser = user;
        mToken = token;
        mSecret = secret;
        mIsNewUser = isNewUser;
        mException = e;
        mPendingCredential = credential;
    }

    /**
     * Extract the {@link IdentityProviderResponse} from the flow's result intent.
     *
     * @param resultIntent The intent which {@code onActivityResult} was called with.
     * @return The IdentityProviderResponse containing the token(s) from signing in with the Idp
     */
    @Nullable
    public static IdentityProviderResponse fromResultIntent(@Nullable Intent resultIntent) {
        if (resultIntent != null) {
            return resultIntent.getParcelableExtra(ExtraConstants.IDP_RESPONSE);
        } else {
            return null;
        }
    }

    @NonNull
    public static IdentityProviderResponse from(@NonNull Exception e) {
        if (e instanceof FirebaseUiException) {
            return new IdentityProviderResponse((FirebaseUiException) e);
        } else {
            FirebaseUiException wrapped = new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR, e);
            wrapped.setStackTrace(e.getStackTrace());
            return new IdentityProviderResponse(wrapped);
        }
    }

    @NonNull
    public static Intent getErrorIntent(@NonNull Exception e) {
        return from(e).toIntent();
    }

    @NonNull
    public IdentityProviderResponse withResult(AuthResult result) {
        return mutate().setNewUser(result.getAdditionalUserInfo().isNewUser()).build();
    }

    @NonNull
    public Intent toIntent() {
        return new Intent().putExtra(ExtraConstants.IDP_RESPONSE, this);
    }

    @NonNull
    public Builder mutate() {
        if (!isSuccessful()) {
            throw new IllegalStateException("Cannot mutate an unsuccessful response.");
        }
        return new Builder(this);
    }

    public boolean isSuccessful() {
        return mException == null;
    }

    public User getUser() {
        return mUser;
    }

    /**
     * Get the type of provider. e.g. {@link GoogleAuthProvider#PROVIDER_ID}
     */
    @NonNull
    @AuthUI.SupportedProvider
    public String getProviderType() {
        return mUser.getProviderId();
    }

    /**
     * Returns true if this user has just signed up, false otherwise.
     */
    public boolean isNewUser() {
        return mIsNewUser;
    }

    /**
     * Get the email used to sign in.
     */
    @Nullable
    public String getEmail() {
        return mUser.getEmail();
    }

    /**
     * Get the phone number used to sign in.
     */
    @Nullable
    public String getPhoneNumber() {
        return mUser.getPhoneNumber();
    }

    /**
     * Get the token received as a result of logging in with the specified IDP
     */
    @Nullable
    public String getIdpToken() {
        return mToken;
    }

    /**
     * Twitter only. Return the token secret received as a result of logging in with Twitter.
     */
    @Nullable
    public String getIdpSecret() {
        return mSecret;
    }

    /**
     * Get the error for a failed sign in.
     */
    @Nullable
    public FirebaseUiException getError() {
        return mException;
    }

    @Nullable
    public AuthCredential getCredentialForLinking() {
        return mPendingCredential;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mUser, flags);
        dest.writeString(mToken);
        dest.writeString(mSecret);
        dest.writeInt(mIsNewUser ? 1 : 0);

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new ByteArrayOutputStream());
            oos.writeObject(mException);

            // Success! The entire exception tree is serializable.
            dest.writeSerializable(mException);
        } catch (IOException e) {
            // Somewhere down the line, the exception is holding on to an object that isn't
            // serializable so default to some exception. It's the best we can do in this case.
            FirebaseUiException fake = new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR,
                    "Exception serialization error, forced wrapping. " +
                            "Original: " + mException +
                            ", original cause: " + mException.getCause());
            fake.setStackTrace(mException.getStackTrace());
            dest.writeSerializable(fake);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ignored) { }
            }
        }

        dest.writeParcelable(mPendingCredential, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentityProviderResponse response = (IdentityProviderResponse) o;

        return sameUser(response)
                && sameToken(response)
                && sameSecret(response)
                && sameIsNewUser(response)
                && sameException(response)
                && samePendingCredentials(response);
    }

    private boolean samePendingCredentials(IdentityProviderResponse response) {
        return mPendingCredential == null ? response.mPendingCredential == null :
                mPendingCredential.getProvider().equals(response.mPendingCredential.getProvider());
    }

    private boolean sameException(IdentityProviderResponse response) {
        return mException == null ? response.mException == null : mException.equals(response.mException);
    }

    private boolean sameIsNewUser(IdentityProviderResponse response) {
        return mIsNewUser == response.mIsNewUser;
    }

    private boolean sameSecret(IdentityProviderResponse response) {
        return mSecret == null ? response.mSecret == null : mSecret.equals(response.mSecret);
    }

    private boolean sameToken(IdentityProviderResponse response) {
        return mToken == null ? response.mToken == null : mToken.equals(response.mToken);
    }

    private boolean sameUser(IdentityProviderResponse response) {
        return mUser == null ? response.mUser == null : mUser.equals(response.mUser);
    }

    @Override
    public int hashCode() {
        int result = mUser == null ? 0 : mUser.hashCode();
        result = 31 * result + (mToken == null ? 0 : mToken.hashCode());
        result = 31 * result + (mSecret == null ? 0 : mSecret.hashCode());
        result = 31 * result + (mIsNewUser ? 1 : 0);
        result = 31 * result + (mException == null ? 0 : mException.hashCode());
        result = 31 * result + (mPendingCredential == null ? 0 : mPendingCredential.getProvider().hashCode());

        return result;
    }

    @Override
    public String toString() {
        return "IdentityProviderResponse{" +
                "mUser=" + mUser +
                ", mToken='" + mToken + '\'' +
                ", mSecret='" + mSecret + '\'' +
                ", mIsNewUser='" + mIsNewUser + '\'' +
                ", mException=" + mException +
                ", mPendingCredential=" + mPendingCredential +
                '}';
    }

    public static class Builder {
        private final User mUser;
        private final AuthCredential mPendingCredential;

        private String mToken;
        private String mSecret;
        private boolean mIsNewUser;

        public Builder(@NonNull User user) {
            mUser = user;
            mPendingCredential = null;
        }

        public Builder(@NonNull AuthCredential pendingCredential) {
            mUser = null;
            mPendingCredential = pendingCredential;
        }

        public Builder(@NonNull IdentityProviderResponse response) {
            mUser = response.mUser;
            mToken = response.mToken;
            mSecret = response.mSecret;
            mIsNewUser = response.mIsNewUser;
            mPendingCredential = response.mPendingCredential;
        }

        public Builder setNewUser(boolean newUser) {
            mIsNewUser = newUser;
            return this;
        }

        public Builder setToken(String token) {
            mToken = token;
            return this;
        }

        public Builder setSecret(String secret) {
            mSecret = secret;
            return this;
        }

        public IdentityProviderResponse build() {
            String providerId = mUser.getProviderId();
            if (!AuthUI.SUPPORTED_PROVIDERS.contains(providerId)) {
                throw new IllegalStateException("Unknown provider: " + providerId);
            }
            if (AuthUI.SOCIAL_PROVIDERS.contains(providerId) && TextUtils.isEmpty(mToken)) {
                throw new IllegalStateException("Token cannot be null when using a non-email provider.");
            }
            if (providerId.equals(TwitterAuthProvider.PROVIDER_ID) && TextUtils.isEmpty(mSecret)) {
                throw new IllegalStateException("Secret cannot be null when using the Twitter provider.");
            }
            return new IdentityProviderResponse(mUser, mToken, mSecret, mIsNewUser);
        }
    }
}
