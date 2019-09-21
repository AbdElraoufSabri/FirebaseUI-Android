

package com.firebase.ui.auth.ui.email

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProviders
import com.afollestad.vvalidator.form
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseUiException
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.ui.FragmentBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.ui.fieldvalidators.VValidation.Companion.formatErrorMessages
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.email.CheckEmailHandler
import com.firebase.ui.auth.viewmodel.email.EmailSignInHandler
import com.google.android.gms.common.internal.ConnectionErrorMessages.getErrorMessage
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.android.synthetic.main.fui_sign_in_layout.*
import kotlinx.android.synthetic.main.fui_sign_in_layout.top_progress_bar
import kotlinx.android.synthetic.main.fui_sign_in_layout.view.*
import kotlinx.android.synthetic.main.fui_welcome_back_password_prompt_layout.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SignInFragment : FragmentBase() {
    companion object {
        val TAG = "SignInFragment"

        fun newInstance(user: User): SignInFragment {
            val fragment = SignInFragment()
            val args = Bundle()
            args.putParcelable(ExtraConstants.USER, user)
            fragment.arguments = args
            return fragment
        }

    }

    private lateinit var mUser: User
    private lateinit var mHandler: EmailSignInHandler
    private lateinit var mCheckEmailHandler: CheckEmailHandler
    private lateinit var mCheckEmailListener: CheckEmailListener

    private lateinit var mAuthenticationButtonsListener: AuthenticationButtonsListener
    @StringRes
    private fun getErrorMessage(exception: Exception): Int {
        return if (exception is FirebaseAuthInvalidCredentialsException) {
            R.string.fui_error_invalid_password
        } else R.string.fui_error_unknown

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            mUser = User.getUser(arguments)
        } else {
            mUser = User.getUser(savedInstanceState)
        }

        mHandler = ViewModelProviders.of(this).get(EmailSignInHandler::class.java)
        mHandler.init(flowParams)

        mHandler.operation.observe(this, object : ResourceObserver<IdpResponse>(
                this, R.string.fui_progress_dialog_signing_in) {
            override fun onSuccess(response: IdpResponse) {
                startSaveCredentials(
                        mHandler.currentUser, response, mHandler.pendingPassword)
            }

            override fun onFailure(e: Exception) {
                passwordEditTextLayout.error = getString(getErrorMessage(e))
            }
        })



    }
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fui_sign_in_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /*
        // TODO  :: Ux improvement
        // ImeHelper.setImeOnDoneListener(mEmailEditText, new ImeHelper.DonePressedListener() {
        //     @Override
        //     public void onDonePressed() {
        //
        //     }
        // });
        //
        // ImeHelper.setImeOnDoneListener(mPasswordEditText, new ImeHelper.DonePressedListener() {
        //     @Override
        //     public void onDonePressed() {
        //
        //     }
        // });

        // if (!flowParameters.shouldShowProviderChoice()) {
        //     PrivacyDisclosureUtils.setupTermsOfServiceAndPrivacyPolicyText(requireContext(),
        //             flowParameters,
        //             termsText)
        // } else {
        //     termsText.setVisibility(View.GONE)
        //     PrivacyDisclosureUtils.setupTermsOfServiceFooter(requireContext(),
        //             flowParameters,
        //             footerText)
        // }

        // TODO unsetErrorOfLayoutOnTyping from iClinic Validation.kt
         */

        txt_dont_have_account_sign_up.setOnClickListener {
            mAuthenticationButtonsListener.switchToSignUp()
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
                matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).*$").description("" +
                        "## can contain special characters " +
                        "## at least 1 uppercase letter " +
                        "## at least 1 lowercase letter " +
                        "## at least 1 number"
                )
                onErrors { _, errors ->
                    formatErrorMessages(errors, passwordEditTextLayout)
                }
            }

            submitWith(view.signInButton) {
                val email = emailEditText.text.toString()
                mCheckEmailHandler.fetchProvider(email)
            }
        }


    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mCheckEmailHandler = ViewModelProviders.of(this).get(CheckEmailHandler::class.java)
        mCheckEmailHandler.init(flowParams)

        val activity = activity

        check(activity is CheckEmailListener) { "Activity must implement CheckEmailListener" }
        check(activity is AuthenticationButtonsListener){"Activity must implement AuthenticationButtonsListener"}

        mCheckEmailListener = activity
        mAuthenticationButtonsListener = activity

        mCheckEmailHandler.operation.observe(this, object : ResourceObserver<User>(
                this, R.string.fui_progress_dialog_checking_accounts) {
            override fun onSuccess(user: User) {
                val email = user.email

                val provider: String? = user.providerId

                if (provider == null) {


                    mCheckEmailListener.onNewUser(
                            User.Builder(EmailAuthProvider.PROVIDER_ID, email)
                                    .setName(user.name)
                                    .setPhotoUri(user.photoUri)
                                    .build()
                    )

                } else if (provider == EmailAuthProvider.PROVIDER_ID) {
                    val password = passwordEditText.text.toString()
                    mCheckEmailListener.onExistingEmailUser(user, password)
                } else {
                    mCheckEmailListener.onExistingIdpUser(user)
                }

            }

            override fun onFailure(e: Exception) {
                if (e is FirebaseUiException && e.errorCode == ErrorCodes.DEVELOPER_ERROR) {
                    mCheckEmailListener.onDeveloperFailure(e)
                }
                // Otherwise just let the user enter their data
            }
        })

        if (savedInstanceState != null) {
            return
        }

        if (flowParams.enableHints) {
            signInButton.performClick()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mCheckEmailHandler.onActivityResult(requestCode, resultCode, data)
    }

    override fun showProgress(message: Int) {
        signInButton.isEnabled = false;
        top_progress_bar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        signInButton.isEnabled = true;
        top_progress_bar.visibility = View.INVISIBLE
    }

    /**
     * Interface to be implemented by Activities hosting this Fragment.
     */
    internal interface CheckEmailListener {

        /**
         * Email entered belongs to an existing email user.
         */
        fun onExistingEmailUser(user: User, password: String)

        /**
         * Email entered belongs to an existing IDP user.
         */
        fun onExistingIdpUser(user: User)

        /**
         * Email entered does not belong to an existing user.
         */
        fun onNewUser(user: User)
        /**
         * Email entered corresponds to an existing user whose sign in methods we do not support.
         */
        fun onDeveloperFailure(e: Exception)

    }
}