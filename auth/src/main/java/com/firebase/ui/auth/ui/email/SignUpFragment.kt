package com.firebase.ui.auth.ui.email

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProviders
import com.afollestad.vvalidator.form
import com.firebase.ui.auth.IdentityProviderResponse
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.ui.FragmentBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.ui.fieldvalidators.VValidation.Companion.formatErrorMessages
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.email.EmailProviderResponseHandler
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.android.synthetic.main.fui_sign_in_layout.*
import kotlinx.android.synthetic.main.fui_sign_up_layout.*
import kotlinx.android.synthetic.main.fui_sign_up_layout.emailEditText
import kotlinx.android.synthetic.main.fui_sign_up_layout.emailEditTextLayout
import kotlinx.android.synthetic.main.fui_sign_up_layout.passwordEditText
import kotlinx.android.synthetic.main.fui_sign_up_layout.passwordEditTextLayout
import kotlinx.android.synthetic.main.fui_sign_up_layout.view.*

/**
 * Fragment to display an email/name/password sign up form for new users.
 */
class SignUpFragment() : FragmentBase() {

    companion object {
        val TAG = "SignUpFragment"

        fun newInstance(user: User): SignUpFragment {
            val fragment = SignUpFragment()
            val args = Bundle()
            args.putParcelable(ExtraConstants.USER, user)
            fragment.arguments = args
            return fragment
        }

    }

    private lateinit var mUser: User
    private lateinit var mAuthenticationButtonsListener: AuthenticationButtonsListener
    private lateinit var mHandler: EmailProviderResponseHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUser = if (savedInstanceState == null) User.getUser(arguments) else User.getUser(savedInstanceState)

        mHandler = ViewModelProviders.of(this).get(EmailProviderResponseHandler::class.java)
        mHandler.init(flowParams)
        mHandler.operation.observe(this, object : ResourceObserver<IdentityProviderResponse>(
                this, R.string.fui_progress_dialog_signing_up) {
            override fun onSuccess(response: IdentityProviderResponse) {
                startSaveCredentials(
                        mHandler.currentUser,
                        response,
                        passwordEditText.text.toString())
            }

            override fun onFailure(e: Exception) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    emailEditTextLayout.error = getString(R.string.fui_invalid_email_address)
                } else {
                    // General error message, this branch should not be invoked but
                    // covers future API changes
                    emailEditTextLayout.error = getString(R.string.fui_email_account_creation_error)
                }
            }
        })


    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fui_sign_up_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /*
         TODO  :: Ux improvement
         ImeHelper.setImeOnDoneListener(mEmailEditText, new ImeHelper.DonePressedListener() {
             @Override
             public void onDonePressed() {

             }
         });

         ImeHelper.setImeOnDoneListener(mPasswordEditText, new ImeHelper.DonePressedListener() {
             @Override
             public void onDonePressed() {

             }
         })

         if (!flowParameters.shouldShowProviderChoice()) {
             PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicyText(requireContext(),
                     flowParameters,
                     termsText)
         } else {
             termsText.setVisibility(View.GONE)
             PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(),
                     flowParameters,
                     footerText)
         }

        TODO unsetErrorOfLayoutOnTyping from iClinic Validation.kt
        */

        txt_have_account_sign_in.setOnClickListener {
            mAuthenticationButtonsListener.switchToSignIn()
        }

        form {
            input(view.emailEditText) {
                isEmail().description("## email format error")
                isNotEmpty().description("## email cannot be empty")
                onErrors { _, errors ->
                    formatErrorMessages(errors, emailEditTextLayout)
                }
            }

            input(view.passwordEditText) {
                length().atLeast(8).description("## at least 8 characters")
                length().atMost(32).description("## at maximum 32 characters")
                matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).*$").description("## can contain special characters ## at least 1 uppercase letter ## at least 1 lowercase letter ## at least 1 number")
                onErrors { _, errors ->
                    formatErrorMessages(errors, passwordEditTextLayout)
                }
            }

            input(view.passwordRepeatEditText) {
                isNotEmpty().description("## password cannot be empty")
                assert("## password not matched") {
                    passwordRepeatEditText.text.toString() == passwordEditText.text.toString()
                }
                onErrors { _, errors ->
                    formatErrorMessages(errors, passwordRepeatEditTextLayout)
                }
            }

            submitWith(view.signUpButton) {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()

                mHandler.startSignIn(IdentityProviderResponse.Builder(
                        User.Builder(EmailAuthProvider.PROVIDER_ID, email)
                                .setPhotoUri(mUser.photoUri)
                                .build())
                        .build(),
                        password)
            }
        }

//        val emailConfig = ProviderUtils.getConfigFromIdpsOrThrow(flowParams.providers, EmailAuthProvider.PROVIDER_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && flowParams.enableCredentials) {
            view.emailEditText.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }

        val email = mUser.email
        if(email?.isNotEmpty()!!) view.emailEditText.setText(email)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()
        check(activity is AuthenticationButtonsListener) { "Activity must implement AuthenticationButtonsListener" }

        mAuthenticationButtonsListener = activity

        if (savedInstanceState != null) {
            return
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(ExtraConstants.USER,
                User.Builder(EmailAuthProvider.PROVIDER_ID, emailEditText.text.toString())
                        .setPhotoUri(mUser.photoUri)
                        .build())
    }


    override fun showProgress(message: Int) {
        signUpButton.isEnabled = false
        top_progress_bar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        signUpButton.isEnabled = true
        top_progress_bar.visibility = View.INVISIBLE
    }
}