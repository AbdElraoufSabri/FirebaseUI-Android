package com.firebase.ui.auth.ui.email

interface AuthenticationButtonsListener {

    fun signUpClicked(email : String, password: String)

    fun switchToSignIn()

    fun switchToSignUp()

}