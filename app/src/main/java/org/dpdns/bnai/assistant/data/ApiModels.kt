package org.dpdns.bnai.assistant.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)
data class SignupRequest(val email: String, val password: String)
data class VerifyOtpRequest(val email: String, val otp: String)
data class AuthResponse(val token: String?, @SerializedName("access_token") val accessToken: String?, @SerializedName("user_id") val userId: String?, @SerializedName("workspace_id") val workspaceId: String?, val role: String?, val email: String?)

data class DynamicSkill(val key: String, val name: String, val description: String?, @SerializedName("owner_only") val ownerOnly: Boolean)
data class SkillsResponse(val status: String, val skills: List<DynamicSkill>)

// Exact mapping from provider_configs table
data class AIModelConfig(
    val id: String, 
    @SerializedName("provider_name") val provider: String, 
    @SerializedName("model_default") val modelName: String, 
    @SerializedName("is_enabled") val isActive: Boolean
)
data class ModelsResponse(val status: String, val models: List<AIModelConfig>)

// Exact mapping from prompt_versions table
data class PromptConfig(
    val id: String, 
    val name: String, 
    val content: String, 
    @SerializedName("is_active") val isActive: Boolean
)
data class PromptsResponse(val status: String, val prompts: List<PromptConfig>)

data class FastChatRequest(@SerializedName("request_id") val requestId: String, val message: String, @SerializedName("user_id") val userId: String, @SerializedName("workspace_id") val workspaceId: String, @SerializedName("project_id") val projectId: String? = null, @SerializedName("conversation_id") val conversationId: String? = null, @SerializedName("skill_key") val skillKey: String = "ai_admin", @SerializedName("ceo_mode") val ceoMode: Boolean = false)

data class FastChatResponse(@SerializedName("request_id") val requestId: String?, val reply: String?, val message: String?, val result: JsonElement?, val provider: String?, val model: String?, val status: String?, @SerializedName("conversation_id") val conversationId: String? = null, val verification: VerificationBlock? = null)

data class GatewayExecuteRequest(@SerializedName("request_id") val requestId: String, @SerializedName("user_id") val userId: String, @SerializedName("workspace_id") val workspaceId: String, @SerializedName("project_id") val projectId: String? = null, @SerializedName("conversation_id") val conversationId: String? = null, val intent: String, @SerializedName("source_mode") val sourceMode: String = "manual", val payload: JsonObject)

data class GatewayErrorItem(val category: String?, val message: String?)
data class GatewayWarningItem(val message: String?)
data class VerificationBlock(val status: String?, val evidence: List<String>?, val persisted: Boolean?, @SerializedName("gateway_persisted") val gatewayPersisted: Boolean?, @SerializedName("live_verified") val liveVerified: Boolean?, @SerializedName("delivered_confirmed") val deliveredConfirmed: Boolean?)
data class GatewayExecuteResponse(@SerializedName("request_id") val requestId: String?, @SerializedName("task_id") val taskId: String?, val status: String?, val result: JsonObject?, val errors: List<GatewayErrorItem>?, val warnings: List<GatewayWarningItem>?, val verification: VerificationBlock?)

data class ChatMessageUi(val id: String, val role: String, val content: String, val timestamp: String, val verificationStatus: String = "verified", val provider: String = "groq", val isError: Boolean = false, val rawJsonResult: String? = null)
data class ProjectUi(val id: String, val name: String, val description: String = "", val status: String = "")
data class SkillUi(val key: String, val name: String, val description: String = "", val ownerOnly: Boolean = false)
data class TaskItemUi(val id: String, val title: String, val goal: String = "", val status: String, val priority: String, val approvalToken: String? = null, val actionType: String? = null, val resourceType: String? = null, val resourceId: String? = null, val cancelEligible: Boolean = true, val retryEligible: Boolean = false, val createdAt: String = "", val updatedAt: String = "")
data class MemoryItemUi(val id: String, val scope: String, val key: String?, val content: String, val category: String, val knowledgeStatus: String = "", val confidence: Double? = null, val createdAt: String = "")
data class MemoryWriteRequest(val scope: String, val key: String?, val content: String, val category: String, @SerializedName("project_id") val projectId: String? = null)
data class ReminderItemUi(val id: String, val title: String, val scheduledAt: String, val status: String, val priority: String)
data class SecurityIncidentUi(val id: String, val severity: String, val category: String, val component: String, val status: String, val timestamp: String, val requiredAction: String)
