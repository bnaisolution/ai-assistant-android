package org.dpdns.bnai.assistant.data
import com.google.gson.JsonObject
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
            } else { Result.failure(Exception("Login failed: HTTP ${resp.code()}")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendFastChat(message: String, projectId: String?, convId: String?, skillKey: String, ceoMode: Boolean): Result<Pair<ChatMessageUi, String?>> {
        val reqId = newRequestId()
        return try {
            val req = FastChatRequest(reqId, message, tokenManager.getUserId(), tokenManager.getWorkspaceId(), projectId, convId, skillKey, ceoMode)
            val resp = api.fastChat(req)
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                val msg = ChatMessageUi(reqId, "assistant", b.reply ?: b.message ?: "Verified response.", "Just now", b.verification?.status ?: "verified", b.provider ?: "groq")
                Result.success(Pair(msg, b.conversationId))
            } else { Result.failure(Exception("Fast Chat failed: HTTP ${resp.code()}")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendGatewayChat(message: String, projectId: String?, convId: String?): Result<ChatMessageUi> {
        val reqId = newRequestId()
        return try {
            val payload = JsonObject().apply { addProperty("message", message) }
            val req = GatewayExecuteRequest(reqId, tokenManager.getUserId(), tokenManager.getWorkspaceId(), projectId, convId, "CHAT", "manual", payload)
            val resp = api.executeWorkflow(req)
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                if (b.status == "completed") {
                    val child = b.result?.getAsJsonObject("child_result")
                    val text = child?.get("assistant_message")?.asString ?: "Gateway Success (No msg)"
                    Result.success(ChatMessageUi(reqId, "assistant", text, "Just now", b.verification?.status ?: "verified", "n8n-gateway"))
                } else { Result.failure(Exception(b.errors?.firstOrNull()?.message ?: "Gateway Error")) }
            } else { Result.failure(Exception("Gateway failed: HTTP ${resp.code()}")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun executeGatewayIntent(intent: String, payloadObj: JsonObject): Result<GatewayExecuteResponse> {
        val reqId = newRequestId()
        return try {
            val req = GatewayExecuteRequest(reqId, tokenManager.getUserId(), tokenManager.getWorkspaceId(), null, null, intent, "manual", payloadObj)
            val resp = api.executeWorkflow(req)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Intent $intent failed: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun checkBackendHealth(): Boolean = try { api.checkHealth().isSuccessful } catch (e: Exception) { false }
}
