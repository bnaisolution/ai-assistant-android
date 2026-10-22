package org.dpdns.bnai.assistant.data
import java.util.UUID

class AppRepository(private val api: ApiService, val tokenManager: TokenManager) {
    private fun newRequestId(): String = UUID.randomUUID().toString()

    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            val resp = api.login(LoginRequest(email, pass))
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                val jwt = b.token ?: b.accessToken ?: ""
                if (jwt.isBlank()) return Result.failure(Exception("No token received"))
                
                tokenManager.saveSession(jwt, b.userId ?: tokenManager.getUserId(), b.workspaceId ?: tokenManager.getWorkspaceId(), email, b.role ?: "normal_user")
                Result.success(true)
            } else { 
                Result.failure(Exception("Login failed: HTTP ${resp.code()}")) 
            }
        } catch (e: Exception) { 
            Result.failure(e) 
        }
    }

    suspend fun checkBackendHealth(): Boolean = try { api.checkHealth().isSuccessful } catch (e: Exception) { false }
}
