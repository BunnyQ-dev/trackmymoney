package com.example.expensetracker

import android.content.Context

object TokenManager {
    private const val PREF_NAME = "expense_prefs"
    private const val KEY_TOKEN = "auth_token"

    fun getToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun saveToken(context: Context, token: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    fun clearToken(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove(KEY_TOKEN)
            apply()
        }
    }

    fun getAuthHeader(context: Context): String? {
        val token = getToken(context)
        return if (token != null) "Bearer $token" else null
    }
}