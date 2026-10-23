package org.dpdns.bnai.assistant.data

import com.google.gson.JsonObject
import java.util.UUID

class AppRepository(private val api: ApiService, val tokenManager: TokenManager) {
    private fun newRequestId(): String = UUID.randomUUID().toString()
    private fun parseError(errBody: String?, defaultMsg: String): String {
        if (errBody == null) return defaultMsg
        return try {
            val json = com.google.gson.JsonParser.parseString(errBody).asJsonObject
            val errors = json.getAsJsonArray("errors")
            if (errors != null && errors.size() > 0) errors[0].asJsonObject.get("message")?.asString ?: defaultMsg else defaultMsg
        } catch (e: Exception) { defaultMsg }
    }

    suspend fun getSkills(): Result<List<DynamicSkill>> {
        return try {
            val resp = api.getSkills()
            if (resp.isSuccessful && resp.body()?.status == "completed") Result.success(resp.body()?.skills ?: emptyList())
            else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Failed to load skills")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAiModels(): Result<List<AIModelConfig>> {
        return try {
            val resp = api.getAiModels()
            if (resp.isSuccessful && resp.body()?.status == "completed") Result.success(resp.body()?.models ?: emptyList())
            else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Failed to load models")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateAiModel(id: String? = null, name: String? = null, model: String? = null, priority: Int? = null, isEnabled: Boolean? = null, baseUrl: String? = null, apiKey: String? = null): Result<Boolean> {
        return try {
            val payload = JsonObject().apply {
                if (id != null) addProperty("id", id)
                if (name != null) addProperty("provider_name", name)
                if (model != null) addProperty("model_default", model)
                if (priority != null) addProperty("priority", priority)
                if (isEnabled != null) addProperty("is_enabled", isEnabled)
                if (baseUrl != null) addProperty("base_url", baseUrl)
                if (apiKey != null) addProperty("api_key", apiKey)
            }
            val resp = api.updateAiModel(payload)
            if (resp.isSuccessful) Result.success(true)
            else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Update failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPrompts(): Result<List<PromptConfig>> {
        return try {
            val resp = api.getPrompts()
            if (resp.isSuccessful && resp.body()?.status == "completed") Result.success(resp.body()?.prompts ?: emptyList())
            else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Failed to load prompts")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updatePrompt(skillKey: String, text: String): Result<Boolean> {
        return try {
            val payload = JsonObject().apply { addProperty("name", skillKey); addProperty("content", text) }
            val resp = api.updatePrompt(payload)
            if (resp.isSuccessful) Result.success(true)
            else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Update failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    // Updated BYOI to accept type
    suspend fun saveByoi(type: String, f1: String, f2: String, f3: String): Result<Boolean> {
        return try {
            val payload = JsonObject().apply { 
                addProperty("integration_type", type)
                addProperty("endpoint", f1)
                addProperty("api_key", f2)
                addProperty("model", f3) 
            }
            val resp = api.saveByoi(payload)
            if (resp.isSuccessful) Result.success(true)
            else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Save failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun signup(email: String, pass: String): Result<String> {
        return try {
            val resp = api.signup(SignupRequest(email, pass))
            if (resp.isSuccessful) Result.success("OTP Sent") else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Signup failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<Boolean> {
        return try {
            val resp = api.verifyOtp(VerifyOtpRequest(email, otp))
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Invalid OTP")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun logout() {
        try { api.logout() } catch (e: Exception) {} finally { tokenManager.clearSession() }
    }

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
                val errStr = resp.errorBody()?.string() ?: ""
                if (resp.code() == 403 && errStr.contains("verify", ignoreCase = true)) Result.failure(Exception("UNVERIFIED_USER"))
                else Result.failure(Exception(parseError(errStr, "Login failed: HTTP ${resp.code()}")))
            }
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
            } else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Fast Chat failed")))
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
                } else Result.failure(Exception(b.errors?.firstOrNull()?.message ?: "Gateway Error"))
            } else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Gateway failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun executeGatewayIntent(intent: String, payloadObj: JsonObject, projectId: String?): Result<GatewayExecuteResponse> {
        val reqId = newRequestId()
        return try {
            val req = GatewayExecuteRequest(reqId, tokenManager.getUserId(), tokenManager.getWorkspaceId(), projectId, null, intent, "manual", payloadObj)
            val resp = api.executeWorkflow(req)
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Intent $intent failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun saveWorkflowMemory(workflowJson: String, projectId: String?): Result<Boolean> {
        return try {
            val req = MemoryWriteRequest(scope = "workflow", key = "generated_wf_${System.currentTimeMillis()}", content = workflowJson, category = "generated_code", projectId = projectId)
            val resp = api.saveMemory(req)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("Memory save failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun testWorkflow(workflowJson: JsonObject): Result<Boolean> {
        return try {
            val payload = JsonObject().apply { add("workflow_json", workflowJson) }
            val resp = api.testWorkflow(payload)
            if (resp.isSuccessful) {
                val status = resp.body()?.get("status")?.toString()?.replace("\"", "")
                if (status == "passed") Result.success(true) else Result.failure(Exception(resp.body()?.get("error")?.toString()?.replace("\"", "") ?: "Sandbox Error"))
            } else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Sandbox Test Failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deployWorkflow(workflowJson: JsonObject): Result<String> {
        return try {
            val payload = JsonObject().apply { add("workflow_json", workflowJson) }
            val resp = api.deployWorkflow(payload)
            if (resp.isSuccessful) {
                val status = resp.body()?.get("status")?.toString()?.replace("\"", "")
                if (status == "completed") Result.success(resp.body()?.get("message")?.toString()?.replace("\"", "") ?: "Deployed")
                else Result.failure(Exception(resp.body()?.get("error")?.toString()?.replace("\"", "") ?: "Deploy Error"))
            } else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Deploy Failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchTasks(): Result<List<TaskItemUi>> {
        return try {
            val resp = api.getTasks()
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.get("status")?.asString == "completed") {
                    val arr = body.getAsJsonArray("tasks") ?: return Result.success(emptyList())
                    Result.success(arr.map { element ->
                        val obj = element.asJsonObject
                        TaskItemUi(id = obj.get("id")?.asString ?: "", title = obj.get("title")?.asString ?: "Untitled", status = obj.get("status")?.asString ?: "unknown", priority = obj.get("priority")?.asString ?: "normal", createdAt = obj.get("created_at")?.asString ?: "", updatedAt = obj.get("updated_at")?.asString ?: "")
                    })
                } else Result.failure(Exception("Backend failed status"))
            } else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Fetch Tasks Failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchMemory(scope: String, projectId: String?): Result<List<MemoryItemUi>> {
        return try {
            val payload = JsonObject().apply { addProperty("scope", scope); if (projectId != null) addProperty("project_id", projectId) }
            val resp = api.readMemory(payload)
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.get("status")?.asString == "completed") {
                    val arr = body.getAsJsonArray("memory_items") ?: return Result.success(emptyList())
                    Result.success(arr.map { element ->
                        val obj = element.asJsonObject
                        MemoryItemUi(id = obj.get("id")?.asString ?: "", scope = obj.get("scope")?.asString ?: "", key = obj.get("key")?.asString, content = obj.get("content")?.asString ?: "", category = obj.get("category")?.asString ?: "general", createdAt = obj.get("created_at")?.asString ?: "")
                    })
                } else Result.failure(Exception("Backend failed status"))
            } else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Fetch Memory Failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchOwnerDashboard(): Result<JsonObject> {
        return try {
            val resp = api.getOwnerDashboard()
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!) else Result.failure(Exception(parseError(resp.errorBody()?.string(), "Owner Dashboard Failed")))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun checkBackendHealth(): Boolean = try { api.checkHealth().isSuccessful } catch (e: Exception) { false }
}
