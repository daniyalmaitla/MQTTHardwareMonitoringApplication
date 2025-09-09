package com.app.mqtthardwareapp.Utils

import android.content.Context

object PrefsHelper {
    private const val PREFS_NAME = "mqtt_prefs"
    private const val KEY_CLIENT_ID = "client_id"

    fun saveClientId(context: Context, clientId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CLIENT_ID, clientId).apply()
    }

    fun getClientId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLIENT_ID, "") ?: ""
    }
}