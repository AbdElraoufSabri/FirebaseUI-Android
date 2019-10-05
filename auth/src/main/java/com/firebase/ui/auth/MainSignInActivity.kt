package com.firebase.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.UserCancellationException
import com.firebase.ui.auth.data.remote.MainSignInHandler
import com.firebase.ui.auth.ui.InvisibleActivityBase
import com.firebase.ui.auth.viewmodel.ResourceObserver
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnSuccessListener

import androidx.annotation.*
import androidx.lifecycle.ViewModelProviders
import com.firebase.ui.auth.ui.HelperActivityBase

class MainSignInActivity : InvisibleActivityBase() {
    private lateinit var mHandler: MainSignInHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = ViewModelProviders.of(this).get(MainSignInHandler::class.java)
        mHandler.init(flowParams)

        mHandler.operation.observe(this, object : ResourceObserver<IdentityProviderResponse>(this) {
            override fun onSuccess(response: IdentityProviderResponse) {
                finish(Activity.RESULT_OK, response.toIntent())
            }

            override fun onFailure(e: Exception) {
                if (e is UserCancellationException) {
                    finish(Activity.RESULT_CANCELED, null)
                } else {
                    finish(Activity.RESULT_CANCELED, IdentityProviderResponse.getErrorIntent(e))
                }
            }
        })

        GoogleApiAvailability.getInstance()
                .makeGooglePlayServicesAvailable(this)
                .addOnSuccessListener(this, OnSuccessListener {
                    if (savedInstanceState != null) {
                        return@OnSuccessListener
                    }
                    mHandler.start()
                })
                .addOnFailureListener(this) { e ->
                    finish(Activity.RESULT_CANCELED, IdentityProviderResponse.getErrorIntent(FirebaseUiException(
                            ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED, e)))
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mHandler.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        fun createIntent(context: Context, flowParams: FlowParameters): Intent {
            return HelperActivityBase.createBaseIntent(context, MainSignInActivity::class.java, flowParams)
        }
    }

}
