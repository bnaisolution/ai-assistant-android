package org.dpdns.bnai.assistant.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.dpdns.bnai.assistant.BuildConfig

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bnai_secure_prefs_v31",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("bnai_fallback_prefs_v31", Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, userId: String, workspaceId: String, email: String, role: String) {
        prefs.edit()
            .putString("jwt_token", token)
            .putString("user_id", userId)
            .putString("workspace_id", workspaceId)
            .putString("email", email)
            .putString("role", role)
            .apply()
    }

    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun getUserId(): String = prefs.getString("user_id", BuildConfig.DEFAULT_USER_ID) ?: BuildConfig.DEFAULT_USER_ID
    fun getWorkspaceId(): String = prefs.getString("workspace_id", BuildConfig.DEFAULT_WORKSPACE_ID) ?: BuildConfig.DEFAULT_WORKSPACE_ID
    fun getEmail(): String = prefs.getString("email", "") ?: ""
    fun getRole(): String = prefs.getString("role", "normal_user") ?: "normal_user"

    fun isOwner(): Boolean =
        getRole().equals("owner_admin", ignoreCase = true) || getRole().equals("owner", ignoreCase = true)

    fun setSessionMode(mode: String) {
        prefs.edit().putString("session_mode", mode).apply()
    }

    fun getSessionMode(): String = prefs.getString("session_mode", "Auto") ?: "Auto"

    fun setActiveProjectId(projectId: String) {
        prefs.edit().putString("active_project_id", projectId).apply()
    }

    fun getActiveProjectId(): String =
        prefs.getString("active_project_id", BuildConfig.ASSISTANT_PROJECT_ID) ?: BuildConfig.ASSISTANT_PROJECT_ID

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
