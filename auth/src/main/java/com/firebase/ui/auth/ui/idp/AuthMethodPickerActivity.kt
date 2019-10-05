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

package com.firebase.ui.auth.ui.idp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdentityProviderConfig
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdentityProviderResponse
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.data.model.UserCancellationException
import com.firebase.ui.auth.data.remote.FacebookSignInHandler
import com.firebase.ui.auth.data.remote.GoogleSignInHandler
import com.firebase.ui.auth.data.remote.TwitterSignInHandler
import com.firebase.ui.auth.ui.AppCompatBase
import com.firebase.ui.auth.ui.HelperActivityBase
import com.firebase.ui.auth.ui.email.AuthenticationButtonsListener
import com.firebase.ui.auth.ui.email.RecoverPasswordActivity
import com.firebase.ui.auth.ui.email.SignInFragment
import com.firebase.ui.auth.ui.email.SignUpFragment
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.viewmodel.ProviderSignInBase
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.email.EmailSignInHandler
import com.firebase.ui.auth.viewmodel.idp.LinkingSocialProviderResponseHandler
import com.firebase.ui.auth.viewmodel.idp.SocialProviderResponseHandler
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.TwitterAuthProvider
import kotlinx.android.synthetic.main.fui_auth_method_picker_layout.*
import java.util.ArrayList

/**
 * Presents the list of authentication options for this app to the user.
 */
class AuthMethodPickerActivity : AppCompatBase(), SignInFragment.CheckEmailListener, AuthenticationButtonsListener {

    private lateinit var mIdpUser: User
    private lateinit var mSocialHandler: SocialProviderResponseHandler
    private lateinit var mSocialLinkingHandler: LinkingSocialProviderResponseHandler
    private lateinit var mEmailHandler: EmailSignInHandler

    private lateinit var mSocialProviders: MutableList<ProviderSignInBase<*>>
    private lateinit var mSignInFragment: SignInFragment
    private lateinit var mSignUpFragment: SignUpFragment

    private lateinit var mSupplier: ViewModelProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSupplier = ViewModelProviders.of(this)

        mSocialHandler = mSupplier.get(SocialProviderResponseHandler::class.java)
        mSocialHandler.init(flowParams)

        mSocialLinkingHandler = mSupplier.get(LinkingSocialProviderResponseHandler::class.java)
        mSocialLinkingHandler.init(flowParams)

        mEmailHandler = mSupplier.get(EmailSignInHandler::class.java)

        mSignInFragment = SignInFragment()
        mSignUpFragment = SignUpFragment()

        switchFragment(mSignInFragment, R.id.auth_fragment, SignInFragment.TAG)

        mSocialProviders = ArrayList()

        setContentView(R.layout.fui_auth_method_picker_layout)

        populateIdpList(flowParams.providers)

        mSocialHandler.operation.observe(this, object : ResourceObserver<IdentityProviderResponse>(
                this, R.string.fui_progress_dialog_signing_in) {
            override fun onSuccess(response: IdentityProviderResponse) {
                startSaveCredentials(mSocialHandler.currentUser, response, null)
            }

            override fun onFailure(e: Exception) {
                if (e !is UserCancellationException) {
                    val text = if (e is FirebaseUiException)
                        e.message
                    else
                        getString(R.string.fui_error_unknown)
                    Toast.makeText(this@AuthMethodPickerActivity,
                            text,
                            Toast.LENGTH_SHORT).show()
                }
            }
        })

    }

    /*
    //
    //    private void setUpPpAndToS() {
    //        TextView termsText = findViewById(R.id.main_tos_and_pp);
    //
    //        PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicyText(this,
    //                getFlowParams(),
    //                termsText);
    //    }
     */

    private fun populateIdpList(providerConfigs: List<IdentityProviderConfig>) {

        mSocialProviders = ArrayList()

        providerConfigs.forEach { idpConfig ->
            when (idpConfig.providerId) {

                GoogleAuthProvider.PROVIDER_ID ->
                    handleSocialProviderSignInOperation(idpConfig, googleSignInButton)

                FacebookAuthProvider.PROVIDER_ID ->
                    handleSocialProviderSignInOperation(idpConfig, facebookSignInButton)

                TwitterAuthProvider.PROVIDER_ID ->
                    handleSocialProviderSignInOperation(idpConfig, twitterSignInButton)

                EmailAuthProvider.PROVIDER_ID ->
                    handleEmailSignInOperation()

                else -> throw IllegalStateException("Unknown provider: ${idpConfig.providerId}")
            }
        }
    }

    @StringRes
    private fun getErrorMessage(exception: Exception): Int {
        return if (exception is FirebaseAuthInvalidCredentialsException) {
            R.string.fui_error_invalid_password
        } else R.string.fui_error_unknown

    }

    private fun handleEmailSignInOperation() {

        mEmailHandler.operation.observe(this, object : ResourceObserver<IdentityProviderResponse>(this, R.string.fui_progress_dialog_signing_in) {
            override fun onSuccess(response: IdentityProviderResponse) {
                startSaveCredentials(mEmailHandler.currentUser, response, mEmailHandler.pendingPassword)
            }

            override fun onFailure(e: Exception) {
                snackbarShow("Email login : error\n" + e.message)
            }

        })
    }

    private fun snackbarShow(message: String) {
        Snackbar.make(auth_fragment, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("ok") {}
                .apply { view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 10 }
                .show()
    }

    private fun handleSocialProviderSignInOperation(identityProviderConfig: IdentityProviderConfig, view: View) {
        val providerId = identityProviderConfig.providerId
        val provider: ProviderSignInBase<*>

        when (providerId) {
            GoogleAuthProvider.PROVIDER_ID -> {
                val google = mSupplier.get(GoogleSignInHandler::class.java)
                google.init(GoogleSignInHandler.Params(identityProviderConfig))
                provider = google
            }
            FacebookAuthProvider.PROVIDER_ID -> {
                val facebook = mSupplier.get(FacebookSignInHandler::class.java)
                facebook.init(identityProviderConfig)
                provider = facebook
            }
            TwitterAuthProvider.PROVIDER_ID -> {
                val twitter = mSupplier.get(TwitterSignInHandler::class.java)
                twitter.init(null)
                provider = twitter
            }
            else -> throw IllegalStateException("Unknown provider: $providerId")
        }
        mSocialProviders.add(provider)

        provider.operation.observe(this, object : ResourceObserver<IdentityProviderResponse>(this) {
            override fun onSuccess(response: IdentityProviderResponse) {
                handleResponse(response)
            }

            override fun onFailure(e: Exception) {
                handleResponse(IdentityProviderResponse.from(e))
            }

            private fun handleResponse(response: IdentityProviderResponse) {
                if (!response.isSuccessful) {
                    // We have no idea what provider this error stemmed from so just forward
                    // this along to the handler.
                    mSocialHandler.startSignIn(response)
                } else if (AuthUI.SOCIAL_PROVIDERS.contains(providerId)) {
                    // Don't use the response's provider since it can be different than the one
                    // that launched the sign-in attempt. Ex: the email flow is started, but
                    // ends up turning into a Google sign-in because that account already
                    // existed. In the previous example, an extra sign-in would incorrectly
                    // started.
                    mSocialHandler.startSignIn(response)
                }
            }
        })

        view.setOnClickListener(View.OnClickListener {
            if (isOffline) {
                Toast.makeText(this@AuthMethodPickerActivity,
                        getString(R.string.fui_no_internet),
                        Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            provider.startSignIn(this@AuthMethodPickerActivity)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mSocialHandler.onActivityResult(requestCode, resultCode, data)

        for (provider in mSocialProviders) {
            provider.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showProgress(message: Int) {
        top_progress_bar.visibility = View.VISIBLE
        disableElement(R.id.googleSignInButton)
        disableElement(R.id.facebookSignInButton)
        disableElement(R.id.twitterSignInButton)
        disableElement(R.id.auth_fragment)
    }

    override fun hideProgress() {
        top_progress_bar.visibility = View.INVISIBLE
        enableElement(R.id.googleSignInButton)
        enableElement(R.id.facebookSignInButton)
        enableElement(R.id.twitterSignInButton)
        enableElement(R.id.auth_fragment)
    }

    private fun disableElement(box: Int) {
        val element = findViewById<View>(box)
        element.isEnabled = false
        element.alpha = 0.75f
    }

    private fun enableElement(box: Int) {
        val element = findViewById<View>(box)
        element.isEnabled = true
        element.alpha = 1.0f
    }

    override fun onExistingEmailUser(user: User, password: String) {
        if (isOffline) {
            Toast.makeText(this,
                    getString(R.string.fui_no_internet),
                    Toast.LENGTH_SHORT).show()
            return
        }

        val response = IdentityProviderResponse.Builder(user).build()
        mEmailHandler.init(flowParams)

        val authCredential = ProviderUtils.getAuthCredential(response)
        mEmailHandler.startSignIn(response.email!!, password, response, authCredential)
    }

    override fun onExistingIdpUser(user: User) {
        mIdpUser = user;

        // Existing social user, direct them to sign in using their chosen provider.
        if (isOffline) {
            Toast.makeText(this,
                    getString(R.string.fui_no_internet),
                    Toast.LENGTH_SHORT).show()
            return
        }

        val providerId = user.providerId
        val config = ProviderUtils.getConfigFromIdps(flowParams.providers, providerId)
        val provider: ProviderSignInBase<*>

        @StringRes val providerName: Int
        when (providerId) {
            GoogleAuthProvider.PROVIDER_ID -> {
                val google = mSupplier.get(GoogleSignInHandler::class.java)
                google.init(GoogleSignInHandler.Params(config, user.email))
                provider = google

                providerName = R.string.fui_idp_name_google
            }
            FacebookAuthProvider.PROVIDER_ID -> {
                val facebook = mSupplier.get(FacebookSignInHandler::class.java)
                facebook.init(config)
                provider = facebook

                providerName = R.string.fui_idp_name_facebook
            }
            TwitterAuthProvider.PROVIDER_ID -> {
                val twitter = mSupplier.get(TwitterSignInHandler::class.java)
                twitter.init(null)
                provider = twitter

                providerName = R.string.fui_idp_name_twitter
            }
            else -> throw IllegalStateException("Invalid provider id: $providerId")
        }

        provider.operation.observe(this, object : ResourceObserver<IdentityProviderResponse>(this) {
            override fun onSuccess(response: IdentityProviderResponse) {
                mSocialLinkingHandler.startSignIn(response)
            }

            override fun onFailure(e: Exception) {
                mSocialLinkingHandler.startSignIn(IdentityProviderResponse.from(e))
            }
        })

        mSocialLinkingHandler.operation.observe(this, object : ResourceObserver<IdentityProviderResponse>(this) {
            override fun onSuccess(response: IdentityProviderResponse) {
                snackbarShow("Idp : $providerId Success")
            }

            override fun onFailure(e: java.lang.Exception) {
                snackbarShow("Idp signin : $providerId cancelled")
            }
        })
    }

    override fun onNewUser(user: User) {
        // TODO switch to sign up fragment
        // New user, direct them to create an account with email/password
        // if account creation is enabled in SignInIntentBuilder
        mSignUpFragment = SignUpFragment.newInstance(user)
        switchToSignUp()
    }

    override fun switchToSignIn() {
        switchFragment(mSignInFragment, R.id.auth_fragment, SignInFragment.TAG)
    }

    override fun switchToSignUp() {
        switchFragment(mSignUpFragment, R.id.auth_fragment, SignInFragment.TAG)
    }

    override fun forgotPasswordClicked() {

        startActivity(RecoverPasswordActivity.createIntent(
                this,
                flowParams))
    }

    override fun onDeveloperFailure(e: Exception) {
        finishOnDeveloperError(e)
    }

    private fun finishOnDeveloperError(e: Exception) {
        finish(Activity.RESULT_CANCELED, IdentityProviderResponse.getErrorIntent(FirebaseUiException(ErrorCodes.DEVELOPER_ERROR, e.message!!)))
    }

    companion object {

        fun createIntent(context: Context, flowParams: FlowParameters): Intent {
            return HelperActivityBase.createBaseIntent(context, AuthMethodPickerActivity::class.java, flowParams)
        }

        fun createIntent(context: Context, flowParams: FlowParameters, email: String): Intent {
            return createIntent(context, flowParams)
                    .putExtra(ExtraConstants.EMAIL, email);
        }
    }
}
