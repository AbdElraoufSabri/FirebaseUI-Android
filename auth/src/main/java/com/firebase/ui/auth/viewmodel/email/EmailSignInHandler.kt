package com.firebase.ui.auth.viewmodel.email

import android.app.Application
import com.firebase.ui.auth.IdentityProviderResponse
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.data.remote.ProfileMerger
import com.firebase.ui.auth.util.data.TaskFailureLogger
import com.firebase.ui.auth.viewmodel.SignInViewModelBase
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider

/**
 * Handles the logic for [com.firebase.ui.auth.ui.email.WelcomeBackPasswordPrompt] including
 * signing in with email and password, linking other credentials, and saving credentials to
 * SmartLock.
 */

class EmailSignInHandler(application: Application) : SignInViewModelBase(application) {

    /**
     * Get the most recent pending password.
     */
    lateinit var pendingPassword: String
        private set

    /**
     * Kick off the sign-in process.
     */
    fun startSignIn(email: String,
                    password: String,
                    inputResponse: IdentityProviderResponse,
                    credential: AuthCredential?) {

        setResult(Resource.forLoading())

        // Store the password before signing in so it can be used for later credential building
        pendingPassword = password

        // Build appropriate IDP response based on inputs
        val outputResponse: IdentityProviderResponse =
                if (credential == null) {
                    // New credential for the email provider
                    IdentityProviderResponse.Builder(
                            User.Builder(EmailAuthProvider.PROVIDER_ID, email).build())
                            .build()
                } else {
                    // New credential for an IDP (Social)
                    IdentityProviderResponse.Builder(inputResponse.user)
                            .setToken(inputResponse.idpToken)
                            .setSecret(inputResponse.idpSecret)
                            .build()
                }

        // Kick off the flow including signing in, linking accounts, and saving with SmartLock
        auth.signInWithEmailAndPassword(email, password)
                .continueWithTask { task ->
                    // Forward task failure by asking for result
                    val result = task.getResult(Exception::class.java)

                    // Task succeeded, link user if necessary
                    if (credential == null) {
                        Tasks.forResult(result)
                    } else {
                        result!!.user!!
                                .linkWithCredential(credential)
                                .continueWithTask(ProfileMerger(outputResponse))
                                .addOnFailureListener(TaskFailureLogger(TAG,
                                        "linkWithCredential+merge failed."))
                    }
                }
                .addOnSuccessListener { result -> handleSuccess(outputResponse, result!!) }
                .addOnFailureListener { e -> setResult(Resource.forFailure(e)) }
                .addOnFailureListener(
                        TaskFailureLogger(TAG, "signInWithEmailAndPassword failed."))

    }

    companion object {
        private val TAG = "WBPasswordHandler"
    }
}
