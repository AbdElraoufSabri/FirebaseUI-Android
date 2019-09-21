package com.firebase.ui.auth.ui.email

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView

import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.ui.FragmentBase
import com.firebase.ui.auth.util.ExtraConstants
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.util.ui.ImeHelper
import com.firebase.ui.auth.util.ui.fieldvalidators.BaseValidator
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator
import com.firebase.ui.auth.util.ui.fieldvalidators.NoOpValidator
import com.firebase.ui.auth.util.ui.fieldvalidators.PasswordFieldValidator
import com.firebase.ui.auth.util.ui.fieldvalidators.RequiredFieldValidator
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.firebase.ui.auth.viewmodel.email.EmailProviderResponseHandler
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

import androidx.annotation.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders

/**
 * Fragment to display an email/name/password sign up form for new users.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RegisterEmailFragment : FragmentBase(), View.OnClickListener, View.OnFocusChangeListener, ImeHelper.DonePressedListener {

    private lateinit var mHandler: EmailProviderResponseHandler

    private lateinit var mNextButton: Button
    private lateinit var mProgressBar: ProgressBar

    private lateinit var mEmailEditText: EditText
    private lateinit var mNameEditText: EditText
    private lateinit var mPasswordEditText: EditText
    private lateinit var mEmailInput: TextInputLayout
    private lateinit var mPasswordInput: TextInputLayout

    private lateinit var mEmailFieldValidator: EmailFieldValidator
    private lateinit var mPasswordFieldValidator: PasswordFieldValidator
    private lateinit var mNameValidator: BaseValidator

    private lateinit var mUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            mUser = User.getUser(arguments)
        } else {
            mUser = User.getUser(savedInstanceState)
        }

        mHandler = ViewModelProviders.of(this).get(EmailProviderResponseHandler::class.java)
        mHandler.init(flowParams)
        mHandler.operation.observe(this, object : ResourceObserver<IdpResponse>(
                this, R.string.fui_progress_dialog_signing_up) {
            override fun onSuccess(response: IdpResponse) {
                startSaveCredentials(
                        mHandler.currentUser,
                        response,
                        mPasswordEditText.text.toString())
            }

            override fun onFailure(e: Exception) {
                if (e is FirebaseAuthWeakPasswordException) {
                    mPasswordInput.error = resources.getQuantityString(
                            R.plurals.fui_error_weak_password,
                            R.integer.fui_min_password_length)
                } else if (e is FirebaseAuthInvalidCredentialsException) {
                    mEmailInput.error = getString(R.string.fui_invalid_email_address)
                } else {
                    // General error message, this branch should not be invoked but
                    // covers future API changes
                    mEmailInput.error = getString(R.string.fui_email_account_creation_error)
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fui_register_email_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mNextButton = view.findViewById(R.id.button_create)
        mProgressBar = view.findViewById(R.id.top_progress_bar)

        mEmailEditText = view.findViewById(R.id.email)
        mNameEditText = view.findViewById(R.id.name)
        mPasswordEditText = view.findViewById(R.id.password)
        mEmailInput = view.findViewById(R.id.email_layout)
        mPasswordInput = view.findViewById(R.id.password_layout)
        val nameInput = view.findViewById<TextInputLayout>(R.id.name_layout)

        // Get configuration
        val emailConfig = ProviderUtils.getConfigFromIdpsOrThrow(
                flowParams.providers, EmailAuthProvider.PROVIDER_ID)
        val requireName = emailConfig.params
                .getBoolean(ExtraConstants.REQUIRE_NAME, true)
        mPasswordFieldValidator = PasswordFieldValidator(
                mPasswordInput,
                resources.getInteger(R.integer.fui_min_password_length))
        mNameValidator = if (requireName)
            RequiredFieldValidator(nameInput,
                    resources.getString(R.string.fui_missing_first_and_last_name))
        else
            NoOpValidator(nameInput)
        mEmailFieldValidator = EmailFieldValidator(mEmailInput)

        ImeHelper.setImeOnDoneListener(mPasswordEditText, this)

        mEmailEditText.onFocusChangeListener = this
        mNameEditText.onFocusChangeListener = this
        mPasswordEditText.onFocusChangeListener = this
        mNextButton.setOnClickListener(this)

        // Only show the name field if required
        nameInput.visibility = if (requireName) View.VISIBLE else View.GONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && flowParams.enableCredentials) {
            mEmailEditText.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }

        val footerText = view.findViewById<TextView>(R.id.email_footer_tos_and_pp_text)
        PrivacyDisclosureUtils.setupTermsOfServiceFooter(
                requireContext(), flowParams, footerText)

        // WARNING: Nothing below this line will be executed on rotation
        if (savedInstanceState != null) {
            return
        }

        // If email is passed in, fill in the field and move down to the name field.
        val email = mUser.email
        if (!TextUtils.isEmpty(email)) {
            mEmailEditText.setText(email)
        }

        // If name is passed in, fill in the field and move down to the password field.
        val name = mUser.name
        if (!TextUtils.isEmpty(name)) {
            mNameEditText.setText(name)
        }

        // See http://stackoverflow.com/questions/11082341/android-requestfocus-ineffective#comment51774752_11082523
        if (!requireName || !TextUtils.isEmpty(mNameEditText.text)) {
            safeRequestFocus(mPasswordEditText)
        } else if (!TextUtils.isEmpty(mEmailEditText.text)) {
            safeRequestFocus(mNameEditText)
        } else {
            safeRequestFocus(mEmailEditText)
        }
    }

    private fun safeRequestFocus(v: View) {
        v.post { v.requestFocus() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = requireActivity()
        activity.setTitle(R.string.fui_title_register_email)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(ExtraConstants.USER,
                User.Builder(EmailAuthProvider.PROVIDER_ID, mEmailEditText.text.toString())
                        .setName(mNameEditText.text.toString())
                        .setPhotoUri(mUser.photoUri)
                        .build())
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (hasFocus) return  // Only consider fields losing focus

        val id = view.id
        if (id == R.id.email) {
            mEmailFieldValidator.validate(mEmailEditText.text)
        } else if (id == R.id.name) {
            mNameValidator.validate(mNameEditText.text)
        } else if (id == R.id.password) {
            mPasswordFieldValidator.validate(mPasswordEditText.text)
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.button_create) {
            validateAndRegisterUser()
        }
    }

    override fun onDonePressed() {
        validateAndRegisterUser()
    }

    override fun showProgress(message: Int) {
        mNextButton.isEnabled = false
        mProgressBar.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        mNextButton.isEnabled = true
        mProgressBar.visibility = View.INVISIBLE
    }

    private fun validateAndRegisterUser() {
        val email = mEmailEditText.text.toString()
        val password = mPasswordEditText.text.toString()
        val name = mNameEditText.text.toString()

        val emailValid = mEmailFieldValidator.validate(email)
        val passwordValid = mPasswordFieldValidator.validate(password)
        val nameValid = mNameValidator.validate(name)
        if (emailValid && passwordValid && nameValid) {
            mHandler.startSignIn(IdpResponse.Builder(
                    User.Builder(EmailAuthProvider.PROVIDER_ID, email)
                            .setName(name)
                            .setPhotoUri(mUser.photoUri)
                            .build())
                    .build(),
                    password)
        }
    }

    companion object {
        val TAG = "RegisterEmailFragment"


        fun newInstance(user: User): RegisterEmailFragment {
            val fragment = RegisterEmailFragment()
            val args = Bundle()
            args.putParcelable(ExtraConstants.USER, user)
            fragment.arguments = args
            return fragment
        }
    }
}
