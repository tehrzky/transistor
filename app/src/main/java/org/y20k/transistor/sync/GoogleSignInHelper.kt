package org.y20k.transistor.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class GoogleSignInHelper(private val context: Context) {
    
    companion object {
        const val RC_SIGN_IN = 9001
    }
    
    private val signInClient: GoogleSignInClient
    
    init {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        signInClient = GoogleSignIn.getClient(context, signInOptions)
    }
    
    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }
    
    fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }
    
    fun signOut(callback: () -> Unit) {
        signInClient.signOut().addOnCompleteListener {
            callback()
        }
    }
    
    fun getSignedInAccountEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }
}
