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
package com.firebase.ui.auth.data.model;

import android.content.Intent;
import android.os.*;
import android.text.TextUtils;

import com.firebase.ui.auth.AuthUI.IdentityProviderConfig;
import com.firebase.ui.auth.util.*;

import java.util.*;

import androidx.annotation.*;

/**
 * Encapsulates the core parameters and data captured during the authentication flow, in a
 * serializable manner, in order to pass data between activities.
 */

public class FlowParameters implements Parcelable {

    public static final Creator<FlowParameters> CREATOR = new Creator<FlowParameters>() {
        @Override
        public FlowParameters createFromParcel(Parcel in) {
            String appName = in.readString();
            List<IdentityProviderConfig> providerInfo = in.createTypedArrayList(IdentityProviderConfig.CREATOR);
            String termsOfServiceUrl = in.readString();
            String privacyPolicyUrl = in.readString();
            boolean enableCredentials = in.readInt() != 0;

            return new FlowParameters(
                    appName,
                    providerInfo,
                    termsOfServiceUrl,
                    privacyPolicyUrl,
                    enableCredentials);
        }

        @Override
        public FlowParameters[] newArray(int size) {
            return new FlowParameters[size];
        }
    };

    @NonNull
    public final String appName;

    @NonNull
    public final List<IdentityProviderConfig> providers;

    @Nullable
    public final String termsOfServiceUrl;

    @Nullable
    public final String privacyPolicyUrl;

    public final boolean enableCredentials;

    public FlowParameters(
            @NonNull String appName,
            @NonNull List<IdentityProviderConfig> providers,
            @Nullable String termsOfServiceUrl,
            @Nullable String privacyPolicyUrl,
            boolean enableCredentials
    ) {
        this.appName = Preconditions.checkNotNull(appName, "appName cannot be null");
        this.providers = Collections.unmodifiableList(
                Preconditions.checkNotNull(providers, "providers cannot be null"));
        this.termsOfServiceUrl = termsOfServiceUrl;
        this.privacyPolicyUrl = privacyPolicyUrl;
        this.enableCredentials = enableCredentials;
    }

    /**
     * Extract FlowParameters from an Intent.
     */
    public static FlowParameters fromIntent(Intent intent) {
        return intent.getParcelableExtra(ExtraConstants.FLOW_PARAMS);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appName);
        dest.writeTypedList(providers);
        dest.writeString(termsOfServiceUrl);
        dest.writeString(privacyPolicyUrl);
        dest.writeInt(enableCredentials ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isTermsOfServiceUrlProvided() {
        return !TextUtils.isEmpty(termsOfServiceUrl);
    }

    public boolean isPrivacyPolicyUrlProvided() {
        return !TextUtils.isEmpty(privacyPolicyUrl);
    }
}
