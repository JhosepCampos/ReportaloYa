package com.cibertec.reporteincidenciasurbanas

import android.app.Application
import android.util.Log
import com.cibertec.reporteincidenciasurbanas.utils.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class IncidenciasApp : Application() {

    companion object {
        lateinit var instance: IncidenciasApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            FirebaseApp.initializeApp(this)
            Log.d("IncidenciasApp", "Firebase inicializado correctamente")

            NotificationHelper.createNotificationChannel(this)

            com.cibertec.reporteincidenciasurbanas.services.ChatSyncService.start(this)

            FirebaseMessaging.getInstance().subscribeToTopic("chat_comunidad")
                .addOnSuccessListener {
                    Log.d("IncidenciasApp", "Suscrito al topic chat_comunidad con éxito")
                }
        } catch (e: Exception) {
            Log.w("IncidenciasApp", "Nota: Firebase no pudo inicializarse automáticamente: ${e.message}")
        }
    }
}
