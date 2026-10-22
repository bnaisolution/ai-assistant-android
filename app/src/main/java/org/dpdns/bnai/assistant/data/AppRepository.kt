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

    suspend fun executeGatewayIntent(intent: String, payloadObj: JsonObject, projectId: String?): Result<GatewayExecuteResponse> {
        val reqId = newRequestId()
        return try {
            val req = GatewayExecuteRequest(reqId, tokenManager.getUserId(), tokenManager.getWorkspaceId(), projectId, null, intent, "manual", payloadObj)
            val resp = api.executeWorkflow(req)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Intent $intent failed: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun saveWorkflowMemory(workflowJson: String, projectId: String?): Result<Boolean> {
        return try {
            val req = MemoryWriteRequest(
                scope = "workflow",
                key = "generated_wf_${System.currentTimeMillis()}",
                content = workflowJson,
                category = "generated_code",
                projectId = projectId
            )
            val resp = api.saveMemory(req)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Memory save failed: HTTP ${resp.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun testWorkflow(workflowJson: JsonObject): Result<Boolean> {
        return try {
            val payload = JsonObject().apply { add("workflow_json", workflowJson) }
            val resp = api.testWorkflow(payload)
            if (resp.isSuccessful) {
                val status = resp.body()?.get("status")?.toString()?.replace("\"", "")
                if (status == "passed") {
                    Result.success(true)
                } else {
                    val err = resp.body()?.get("error")?.toString()?.replace("\"", "") ?: "Sandbox Error"
                    Result.failure(Exception(err))
                }
            } else {
                val err = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // NEW: Deploy Function
    suspend fun deployWorkflow(workflowJson: JsonObject): Result<String> {
        return try {
            val payload = JsonObject().apply { add("workflow_json", workflowJson) }
            val resp = api.deployWorkflow(payload)
            if (resp.isSuccessful) {
                val status = resp.body()?.get("status")?.toString()?.replace("\"", "")
                if (status == "completed") {
                    val msg = resp.body()?.get("message")?.toString()?.replace("\"", "") ?: "Deployed successfully"
                    Result.success(msg)
                } else {
                    val err = resp.body()?.get("error")?.toString()?.replace("\"", "") ?: "Deploy Error"
                    Result.failure(Exception(err))
                }
            } else {
                val err = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchTasks(): Result<List<TaskItemUi>> {
        return try {
            val resp = api.getTasks()
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val status = body.get("status")?.asString
                if (status == "completed") {
                    val arr = body.getAsJsonArray("tasks") ?: return Result.success(emptyList())
                    val tasks = arr.map { element -> 
                        val obj = element.asJsonObject
                        TaskItemUi(
                            id = obj.get("id")?.asString ?: "",
                            title = obj.get("title")?.asString ?: "Untitled",
                            status = obj.get("status")?.asString ?: "unknown",
                            priority = obj.get("priority")?.asString ?: "normal",
                            createdAt = obj.get("created_at")?.asString ?: "",
                            updatedAt = obj.get("updated_at")?.asString ?: ""
                        )
                    }
                    Result.success(tasks)
                } else {
                    Result.failure(Exception("Backend returned failed status"))
                }
            } else {
                val err = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchMemory(scope: String, projectId: String?): Result<List<MemoryItemUi>> {
        return try {
            val payload = JsonObject().apply { 
                addProperty("scope", scope) 
                if (projectId != null) addProperty("project_id", projectId)
            }
            val resp = api.readMemory(payload)
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val status = body.get("status")?.asString
                if (status == "completed") {
                    val arr = body.getAsJsonArray("memory_items") ?: return Result.success(emptyList())
                    val items = arr.map { element ->
                        val obj = element.asJsonObject
                        MemoryItemUi(
                            id = obj.get("id")?.asString ?: "",
                            scope = obj.get("scope")?.asString ?: "",
                            key = obj.get("key")?.asString,
                            content = obj.get("content")?.asString ?: "",
                            category = obj.get("category")?.asString ?: "general",
                            createdAt = obj.get("created_at")?.asString ?: ""
                        )
                    }
                    Result.success(items)
                } else {
                    Result.failure(Exception("Backend returned failed status"))
                }
            } else {
                val err = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchOwnerDashboard(): Result<JsonObject> {
        return try {
            val resp = api.getOwnerDashboard()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                val err = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun checkBackendHealth(): Boolean = try { api.checkHealth().isSuccessful } catch (e: Exception) { false }
}
