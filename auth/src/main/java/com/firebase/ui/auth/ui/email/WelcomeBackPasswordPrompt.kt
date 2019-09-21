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

package com.firebase.ui.auth.ui.email

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.TextView

import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.ui.AppCompatBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.util.ui.ImeHelper
import com.firebase.ui.auth.util.ui.TextHelper
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.email.EmailSignInHandler
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

import androidx.annotation.*
import androidx.lifecycle.ViewModelProviders
import com.firebase.ui.auth.ui.HelperActivityBase
import kotlinx.android.synthetic.main.fui_welcome_back_password_prompt_layout.*

/**
 * Activity to link a pre-existing email/password account to a new IDP sign-in by confirming the
 * password before initiating a link.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WelcomeBackPasswordPrompt : AppCompatBase(), View.OnClickListener, ImeHelper.DonePressedListener {
    private lateinit var mIdpResponse: IdpResponse
    private lateinit var mHandler: EmailSignInHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fui_welcome_back_password_prompt_layout)

        // Show keyboard
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        mIdpResponse = IdpResponse.fromResultIntent(intent)!!
        val email = mIdpResponse.email

        ImeHelper.setImeOnDoneListener(password, this)

        // Create welcome back text with email bolded.
        val bodyText = getString(R.string.fui_welcome_back_password_prompt_body, email)

        val spannableStringBuilder = SpannableStringBuilder(bodyText)
        TextHelper.boldAllOccurencesOfText(spannableStringBuilder, bodyText, email!!)

        val bodyTextView = findViewById<TextView>(R.id.welcome_back_password_body)
        bodyTextView.text = spannableStringBuilder

        // Click listeners
        button_done.setOnClickListener(this)
        findViewById<View>(R.id.trouble_signing_in).setOnClickListener(this)

        // Initialize ViewModel with arguments
        mHandler = ViewModelProviders.of(this).get(EmailSignInHandler::class.java)
        mHandler.init(flowParams)

        // Observe the state of the main auth operation
        mHandler.operation.observe(this, object : ResourceObserver<IdpResponse>(
                this, R.string.fui_progress_dialog_signing_in) {
            override fun onSuccess(response: IdpResponse) {
                startSaveCredentials(
                        mHandler.currentUser, response, mHandler.pendingPassword)
            }

            override fun onFailure(e: Exception) {
                    password_layout.error = getString(getErrorMessage(e))
            }
        })

        val footerText = findViewById<TextView>(R.id.email_footer_tos_and_pp_text)
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(this, flowParams, footerText)
    }

    @StringRes
    private fun getErrorMessage(exception: Exception): Int {
        return if (exception is FirebaseAuthInvalidCredentialsException) {
            R.string.fui_error_invalid_password
        } else R.string.fui_error_unknown

    }

    private fun onForgotPasswordClicked() {
        startActivity(RecoverPasswordActivity.createIntent(
                this,
                flowParams,
                mIdpResponse.email))
    }

    override fun onDonePressed() {
        validateAndSignIn()
    }

    private fun validateAndSignIn(password: String = this.password.text.toString()) {
        // Check for null or empty password
        if (TextUtils.isEmpty(password)) {
            password_layout.error = getString(R.string.fui_error_invalid_password)
            return
        } else {
            password_layout.error = null
        }

        val authCredential = ProviderUtils.getAuthCredential(mIdpResponse)
        mHandler.startSignIn(mIdpResponse.email!!, password, mIdpResponse, authCredential)
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.button_done) {
            validateAndSignIn()
        } else if (id == R.id.trouble_signing_in) {
            onForgotPasswordClicked()
        }
    }

    override fun showProgress(message: Int) {
        button_done.isEnabled = false
        top_progress_bar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        button_done.isEnabled = true
        top_progress_bar.visibility = View.INVISIBLE
    }

    companion object {

        fun createIntent(context: Context, flowParams: FlowParameters, response: IdpResponse): Intent {
            return HelperActivityBase.createBaseIntent(context, WelcomeBackPasswordPrompt::class.java, flowParams)
                    .putExtra(ExtraConstants.IDP_RESPONSE, response)
        }
    }
}
