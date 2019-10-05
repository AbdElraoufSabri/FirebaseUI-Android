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

import com.firebase.ui.auth.AuthUI.IdentityProviderConfig;
import com.firebase.ui.auth.AuthUI.SignInIntentBuilder;
import com.firebase.ui.auth.data.model.FlowParameters;
import com.firebase.ui.auth.testhelpers.TestConstants;
import com.firebase.ui.auth.testhelpers.TestHelper;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.auth.EmailAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AuthUITest {
    private static final String URL = "url";
    private AuthUI mAuthUi;

    @Before
    public void setUp() {
        TestHelper.initialize();
        mAuthUi = AuthUI.getInstance(TestHelper.MOCK_APP);
    }

    @Test
    public void testCreateStartIntent_shouldHaveEmailAsDefaultProvider() {
        FlowParameters flowParameters = mAuthUi
                .createSignInIntentBuilder()
                .build()
                .getParcelableExtra(ExtraConstants.FLOW_PARAMS);
        assertEquals(1, flowParameters.providers.size());
        assertEquals(EmailAuthProvider.PROVIDER_ID,
                flowParameters.providers.get(0).getProviderId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStartIntent_shouldOnlyAllowOneInstanceOfAnIdp() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setAvailableProviders(Arrays.asList(
                new IdentityProviderConfig.EmailBuilder().build(),
                new IdentityProviderConfig.EmailBuilder().build()));
    }

    @Test
    public void testCreatingStartIntent() {
        FlowParameters flowParameters = mAuthUi
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                        new IdentityProviderConfig.EmailBuilder().build(),
                        new AuthUI.IdentityProviderConfig.GoogleBuilder().build(),
                        new IdentityProviderConfig.FacebookBuilder().build(),
                        new IdentityProviderConfig.TwitterBuilder().build()))
                .setTosAndPrivacyPolicyUrls(TestConstants.TOS_URL, TestConstants.PRIVACY_URL)
                .build()
                .getParcelableExtra(ExtraConstants.FLOW_PARAMS);

        assertEquals(4, flowParameters.providers.size());
        assertEquals(TestHelper.MOCK_APP.getName(), flowParameters.appName);
        assertEquals(TestConstants.TOS_URL, flowParameters.termsOfServiceUrl);
        assertEquals(TestConstants.PRIVACY_URL, flowParameters.privacyPolicyUrl);
    }

    @Test(expected = NullPointerException.class)
    public void testCreatingStartIntent_withNullTos_expectEnforcesNonNullTosUrl() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setTosAndPrivacyPolicyUrls(null, TestConstants.PRIVACY_URL);
    }

    @Test(expected = NullPointerException.class)
    public void testCreatingStartIntent_withNullPp_expectEnforcesNonNullPpUrl() {
        SignInIntentBuilder startIntent = mAuthUi.createSignInIntentBuilder();
        startIntent.setTosAndPrivacyPolicyUrls(TestConstants.TOS_URL, TestConstants.PRIVACY_URL);
    }
}
