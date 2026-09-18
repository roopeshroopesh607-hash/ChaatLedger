package com.example

import android.app.Application
import android.util.Log
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import com.google.firebase.FirebaseApp

class ChaatLedgerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("ChaatLedgerApplication", "FirebaseApp init: ${e.message}")
        }
        container = DefaultAppContainer(this)
    }
}
