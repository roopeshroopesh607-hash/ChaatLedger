package com.example.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(private val context: Context) {
    private val tag = "FirebaseAuthManager"

    val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    val currentUser: FirebaseUser?
        get() = if (isFirebaseInitialized) {
            try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                null
            }
        } else null

    val isSignedIn: Boolean
        get() = currentUser != null

    suspend fun silentSignIn(): Result<FirebaseUser?> {
        return try {
            if (!isFirebaseInitialized) {
                Log.w(tag, "Firebase is not initialized yet. Skipping silent sign in.")
                return Result.success(null)
            }

            val auth = FirebaseAuth.getInstance()
            val existing = auth.currentUser
            if (existing != null) {
                Log.d(tag, "Already signed in anonymously: ${existing.uid}")
                return Result.success(existing)
            }

            Log.d(tag, "Signing in anonymously silently...")
            val result = auth.signInAnonymously().await()
            val user = result.user
            Log.d(tag, "Silent anonymous sign in successful: ${user?.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.w(tag, "Silent anonymous sign in encountered an issue (offline or unconfigured): ${e.message}")
            Result.failure(e)
        }
    }
}
