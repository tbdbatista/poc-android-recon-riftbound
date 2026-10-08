package com.riftbound.recon.ui.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

object GoogleAuthClientHelper {

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val serverClientId = getWebClientId(context)
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (serverClientId.isNotBlank()) {
            gsoBuilder.requestIdToken(serverClientId)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getIdTokenFromIntent(data: Intent?): Result<String> {
        return try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                Result.success(idToken)
            } else {
                Result.failure(IllegalStateException("ID Token do Google não encontrado. Verifique as configurações SHA-1."))
            }
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                context.getString(resId)
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
