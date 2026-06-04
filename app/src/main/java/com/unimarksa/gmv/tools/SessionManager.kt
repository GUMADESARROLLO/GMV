package com.unimarksa.gmv.tools

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREFS_NAME = "gmv_session"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_USER = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_PHONE = "phone"
    private const val KEY_EMAIL = "email"
    private const val KEY_MEDICAL_TOKEN = "medical_token"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun saveLogin(username: String, fullName: String, phone: String, email: String) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER, username)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_PHONE, phone)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun getUsername(): String = prefs.getString(KEY_USER, "") ?: ""
    fun savePassword(password: String) { prefs.edit().putString(KEY_PASSWORD, password).apply() }
    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    fun getFullName(): String = prefs.getString(KEY_FULL_NAME, "") ?: ""
    fun getPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""
    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun saveMedicalToken(token: String) { prefs.edit().putString(KEY_MEDICAL_TOKEN, token).apply() }
    fun getMedicalToken(): String = prefs.getString(KEY_MEDICAL_TOKEN, "") ?: ""
    fun clearMedicalToken() { prefs.edit().remove(KEY_MEDICAL_TOKEN).apply() }
}
