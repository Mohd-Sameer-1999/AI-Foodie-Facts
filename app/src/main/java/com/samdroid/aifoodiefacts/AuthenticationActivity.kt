package com.samdroid.aifoodiefacts

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar

class AuthenticationActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 101
    private val TAG = "AuthenticationActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        setTitle("Sign in")

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                    .setAvailableProviders(providers)
                    .build(),
                RC_SIGN_IN)
    }

    private fun showSnackbar(id : Int){
        Snackbar.make(findViewById(R.id.constraint_layout), resources.getString(id), Snackbar.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){

            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("signedIn", 1)
                startActivity(intent)
                finish()
                return
            }
            else {
                if(response == null){
                    //If no response from the Server
                    showSnackbar(R.string.sign_in_cancelled)
                    return
                }
                if(response.error?.errorCode == ErrorCodes.NO_NETWORK){
                    //If there was a network problem the user's phone
                    showSnackbar(R.string.no_internet_connection)
                    return
                }
                if(response.error?.errorCode == ErrorCodes.UNKNOWN_ERROR){
                    //If the error cause was unknown
                    showSnackbar(R.string.unknown_error)
                    return
                }
            }
        }
        else {
            showSnackbar(R.string.unknown_sign_in_response) //if the sign in response was unknown
        }
    }
}