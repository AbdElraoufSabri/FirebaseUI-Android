package com.firebase.uidemo.util;


import android.content.Context;
import androidx.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.firebase.uidemo.R;
import com.google.firebase.auth.ActionCodeSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ConfigurationUtils {

    private ConfigurationUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static boolean isGoogleMisconfigured(@NonNull Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.default_web_client_id));
    }

    public static boolean isFacebookMisconfigured(@NonNull Context context) {
        return AuthUI.UNCONFIGURED_CONFIG_VALUE.equals(
                context.getString(R.string.facebook_application_id));
    }

    public static boolean isTwitterMisconfigured(@NonNull Context context) {
        List<String> twitterConfigs = Arrays.asList(
                context.getString(R.string.twitter_consumer_key),
                context.getString(R.string.twitter_consumer_secret)
        );

        return twitterConfigs.contains(AuthUI.UNCONFIGURED_CONFIG_VALUE);
    }

    @NonNull
    public static List<AuthUI.IdentityProviderConfig> getConfiguredProviders(@NonNull Context context) {
        List<AuthUI.IdentityProviderConfig> providers = new ArrayList<>();

        if (!isGoogleMisconfigured(context)) {
            providers.add(new AuthUI.IdentityProviderConfig.GoogleBuilder().build());
        }

        if (!isFacebookMisconfigured(context)) {
            providers.add(new AuthUI.IdentityProviderConfig.FacebookBuilder().build());
        }

        if (!isTwitterMisconfigured(context)) {
            providers.add(new AuthUI.IdentityProviderConfig.TwitterBuilder().build());
        }

        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setAndroidPackageName("com.firebase.uidemo", true, null)
                .setHandleCodeInApp(true)
                .setUrl("https://google.com")
                .build();

        providers.add(new AuthUI.IdentityProviderConfig.EmailBuilder()
                .setAllowNewAccounts(true)
                .setActionCodeSettings(actionCodeSettings)
                .build());


        return providers;
    }
}
