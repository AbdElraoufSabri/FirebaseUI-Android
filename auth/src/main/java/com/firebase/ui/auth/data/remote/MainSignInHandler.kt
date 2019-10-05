package com.firebase.ui.auth.data.remote

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Parcelable
import android.text.TextUtils
import com.firebase.ui.auth.AuthUI.SupportedProvider
import com.firebase.ui.auth.IdentityProviderResponse
import com.firebase.ui.auth.data.model.IntentRequiredException
import com.firebase.ui.auth.data.model.PendingIntentRequiredException
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.data.model.UserCancellationException
import com.firebase.ui.auth.ui.idp.AuthMethodPickerActivity
import com.firebase.ui.auth.ui.idp.SingleSignInActivity
import com.firebase.ui.auth.util.GoogleApiUtils
import com.firebase.ui.auth.util.data.ProviderUtils
import com.firebase.ui.auth.viewmodel.RequestCodes
import com.firebase.ui.auth.viewmodel.SignInViewModelBase
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.TwitterAuthProvider
import java.util.ArrayList

class MainSignInHandler(application: Application) : SignInViewModelBase(application) {

    fun start() {

        val accounts = ArrayList<String>()

        for (idpConfig in arguments.providers) {
            @SupportedProvider val providerId = idpConfig.providerId
            if (providerId == GoogleAuthProvider.PROVIDER_ID) {
                accounts.add(ProviderUtils.providerIdToAccountType(providerId))
            }
        }

        // Only support password credentials if email auth is enabled
        val supportPasswords = ProviderUtils.getConfigFromIdps(arguments.providers, EmailAuthProvider.PROVIDER_ID) != null
        // If the request will be empty, avoid the step entirely

        val willRequestCredentials = supportPasswords || accounts.size > 0

        if (arguments.enableCredentials && willRequestCredentials) {
            setResult(Resource.forLoading())

            GoogleApiUtils.getCredentialsClient(getApplication())
                    .request(
                            CredentialRequest.Builder()
                                    .setPasswordLoginSupported(true)
                                    .setAccountTypes(*accounts.toTypedArray())
                                    .build())
                    .addOnCompleteListener { task ->
                        try {
                            handleCredential(task.getResult(ApiException::class.java)!!.credential)
                        } catch (e: ResolvableApiException) {
                            when {
                                e.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED ->
                                    setResult(Resource.forFailure(PendingIntentRequiredException(e.resolution, RequestCodes.CRED_HINT)))
                                else -> startAuthMethodChoice()
                            }
                        } catch (e: ApiException) {
                            startAuthMethodChoice()
                        }
                    }
        } else {
            startAuthMethodChoice()
        }
    }

    private fun startAuthMethodChoice() {
        val intent = AuthMethodPickerActivity.createIntent(getApplication(), arguments)
        setResult(Resource.forFailure(IntentRequiredException(intent, RequestCodes.AUTH_PICKER_FLOW)))
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCodes.CRED_HINT -> {
                if (resultCode == Activity.RESULT_OK) {
                    val credential = data!!.getParcelableExtra<Parcelable>(Credential.EXTRA_KEY) as Credential
                    handleCredential(credential)
                } else startAuthMethodChoice()
            }
            RequestCodes.EMAIL_FLOW,
            RequestCodes.AUTH_PICKER_FLOW,
            RequestCodes.PROVIDER_FLOW -> {
                val response = IdentityProviderResponse.fromResultIntent(data)
                when {
                    response == null -> setResult(Resource.forFailure(UserCancellationException()))
                    response.isSuccessful -> setResult(Resource.forSuccess(response))
                    else -> setResult(Resource.forFailure(response.error!!))
                }
            }
        }
    }

    private fun handleCredential(credential: Credential) {
        val id = credential.id
        val password = credential.password
        if (TextUtils.isEmpty(password)) {
            when (credential.accountType) {
                null -> startAuthMethodChoice()
                else -> redirectSignIn(ProviderUtils.accountTypeToProviderId(credential.accountType!!)!!, id)
            }
        } else {
            val response = IdentityProviderResponse.Builder(
                    User.Builder(EmailAuthProvider.PROVIDER_ID, id).build()).build()

            setResult(Resource.forLoading())
            auth.signInWithEmailAndPassword(id, password!!)
                    .addOnSuccessListener { result -> handleSuccess(response, result) }
                    .addOnFailureListener { e ->
                        if (e is FirebaseAuthInvalidUserException || e is FirebaseAuthInvalidCredentialsException) {
                            // In this case the credential saved in SmartLock was not
                            // a valid credential, we should delete it from SmartLock
                            // before continuing.
                            GoogleApiUtils.getCredentialsClient(getApplication())
                                    .delete(credential)
                        }
                        startAuthMethodChoice()
                    }
        }
    }

    private fun redirectSignIn(provider: String, id: String) {
        when (provider) {
            EmailAuthProvider.PROVIDER_ID -> {
                val intent = AuthMethodPickerActivity.createIntent(getApplication(), arguments, id)
                setResult(Resource.forFailure(IntentRequiredException(intent, RequestCodes.EMAIL_FLOW)))
            }
            GoogleAuthProvider.PROVIDER_ID,
            FacebookAuthProvider.PROVIDER_ID,
            TwitterAuthProvider.PROVIDER_ID -> {
                val user = User.Builder(provider, id).build()
                val intent = SingleSignInActivity.createIntent(getApplication(), arguments, user)
                setResult(Resource.forFailure(IntentRequiredException(intent, RequestCodes.PROVIDER_FLOW)))
            }

            else -> startAuthMethodChoice()
        }
    }
}
