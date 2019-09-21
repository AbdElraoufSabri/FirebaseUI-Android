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
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdpResponse
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
import com.firebase.ui.auth.ui.email.SignInFragment
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthMethodPickerActivity : AppCompatBase(), SignInFragment.CheckEmailListener, AuthenticationButtonsListener {

    private lateinit var mSocialHandler: SocialProviderResponseHandler
    private lateinit var mSocialLinkingHandler: LinkingSocialProviderResponseHandler
    private lateinit var mEmailHandler: EmailSignInHandler

    private lateinit var mSocialProviders: MutableList<ProviderSignInBase<*>>
    private lateinit var mSignInFragment: SignInFragment

    private lateinit var mSupplier: ViewModelProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSupplier = ViewModelProviders.of(this)

        mSocialHandler = mSupplier.get(SocialProviderResponseHandler::class.java)
        mSocialHandler.init(flowParams)

        mSocialLinkingHandler = mSupplier.get(LinkingSocialProviderResponseHandler::class.java)
        mSocialLinkingHandler.init(flowParams)

        mEmailHandler = mSupplier.get(EmailSignInHandler::class.java)
        mEmailHandler.init(flowParams)

        mSignInFragment = SignInFragment()
        switchFragment(mSignInFragment, R.id.auth_fragment, SignInFragment.TAG)

        mSocialProviders = ArrayList()

        setContentView(R.layout.fui_auth_method_picker_layout)

        populateIdpList(flowParams.providers)

        mSocialHandler.operation.observe(this, object : ResourceObserver<IdpResponse>(
                this, R.string.fui_progress_dialog_signing_in) {
            override fun onSuccess(response: IdpResponse) {
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

    private fun populateIdpList(providerConfigs: List<IdpConfig>) {

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

        mEmailHandler.operation.observe(this, object : ResourceObserver<IdpResponse>(this) {
            override fun onSuccess(response: IdpResponse) {
                handleResponse(response)
            }

            override fun onFailure(e: Exception) {
                handleResponse(IdpResponse.from(e))
            }

            private fun handleResponse(response: IdpResponse) {
                val responseStatus =
                        if (response.isSuccessful) "Successful"
//                            Activity.RESULT_OK
                        else
                            "Insuccessful"
//                            Activity.RESULT_CANCELED

                Snackbar.make(auth_fragment, "Email login : $responseStatus", Snackbar.LENGTH_INDEFINITE)
                        .setAction("ok") {}
                        .apply { view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 10 }
                        .show()
            }

        })
    }

    private fun handleSocialProviderSignInOperation(idpConfig: IdpConfig, view: View) {
        val providerId = idpConfig.providerId
        val provider: ProviderSignInBase<*>

        when (providerId) {
            GoogleAuthProvider.PROVIDER_ID -> {
                val google = mSupplier.get(GoogleSignInHandler::class.java)
                google.init(GoogleSignInHandler.Params(idpConfig))
                provider = google
            }
            FacebookAuthProvider.PROVIDER_ID -> {
                val facebook = mSupplier.get(FacebookSignInHandler::class.java)
                facebook.init(idpConfig)
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

        provider.operation.observe(this, object : ResourceObserver<IdpResponse>(this) {
            override fun onSuccess(response: IdpResponse) {
                handleResponse(response)
            }

            override fun onFailure(e: Exception) {
                handleResponse(IdpResponse.from(e))
            }

            private fun handleResponse(response: IdpResponse) {
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

        val response = IdpResponse.Builder(user).build()


    }

    override fun onExistingIdpUser(user: User) {
        // Existing social user, direct them to sign in using their chosen provider.
        if (isOffline) {
            Toast.makeText(this,
                    getString(R.string.fui_no_internet),
                    Toast.LENGTH_SHORT).show()
            return
        }

        val response = IdpResponse.Builder(user).build()
        val credential = ProviderUtils.getAuthCredential(response)
        mSocialLinkingHandler.setRequestedSignInCredentialForEmail(credential, user.email)

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

        provider.operation.observe(this, object : ResourceObserver<IdpResponse>(this) {
            override fun onSuccess(response: IdpResponse) {
                mSocialLinkingHandler.startSignIn(response)
            }

            override fun onFailure(e: java.lang.Exception) {
                mSocialLinkingHandler.startSignIn(IdpResponse.from(e))
            }
        })

    }

    override fun onNewUser(user: User) {
        // TODO switch to sign up fragment
        switchToSignUp()


    }

    override fun signUpClicked(email: String, password: String) {
        if (isOffline) {
            Toast.makeText(this,
                    getString(R.string.fui_no_internet),
                    Toast.LENGTH_SHORT).show()
            return
        }
    }

    override fun switchToSignIn() {
        TODO("not implemented")
    }

    override fun switchToSignUp() {
        TODO("not implemented")
    }

    override fun onDeveloperFailure(e: Exception) {
        finishOnDeveloperError(e)
    }

    private fun finishOnDeveloperError(e: Exception) {
        finish(Activity.RESULT_CANCELED, IdpResponse.getErrorIntent(FirebaseUiException(ErrorCodes.DEVELOPER_ERROR, e.message!!)))
    }

    companion object {

        fun createIntent(context: Context, flowParams: FlowParameters): Intent {
            return HelperActivityBase.createBaseIntent(context, AuthMethodPickerActivity::class.java, flowParams)
        }
    }
}
