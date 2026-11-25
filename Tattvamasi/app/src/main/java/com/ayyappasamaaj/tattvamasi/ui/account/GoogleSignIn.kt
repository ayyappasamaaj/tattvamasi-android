package com.ayyappasamaaj.tattvamasi.ui.account

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.ayyappasamaaj.tattvamasi.BuildConfig
import com.ayyappasamaaj.tattvamasi.util.AppLog
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Continue with Google: Android's Credential Manager shows the phone's Google accounts and gives back an ID token for
 * the website's own Google client (BuildConfig.GOOGLE_WEB_CLIENT_ID), which the website checks before signing the
 * person in (website: docs/app-sign-in.md, "Google"). Google lets the app ask because its package name and signing
 * key are registered in the website's Google Cloud project (apps doc: "Tattvamasi Android (Play)" and "(test builds)").
 */
object GoogleSignIn {
    val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    sealed interface Result {
        data class Token(val idToken: String) : Result
        data object Cancelled : Result
        data class Problem(val message: String) : Result
    }

    /** [context] must be an Activity's, for Google's account picker. */
    suspend fun idToken(context: Context): Result {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Problem("Google didn't sign you in. Please try again, or use your email and password.")
            }
        } catch (_: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (_: NoCredentialException) {
            Result.Problem("There's no Google account on this device. Add one in Settings, or use your email and password.")
        } catch (e: GetCredentialException) {
            AppLog.w("GoogleSignIn", "${e.type}: ${e.message}")
            Result.Problem("Google sign-in isn't available right now. Please use your email and password.")
        }
    }
}
