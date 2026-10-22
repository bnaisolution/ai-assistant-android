package org.dpdns.bnai.assistant.data
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)
data class AuthResponse(@SerializedName("token") val token: String?, @SerializedName("access_token") val accessToken: String?, @SerializedName("user_id") val userId: String?, @SerializedName("workspace_id") val workspaceId: String?, val role: String?, val email: String?)
data class FastChatRequest(@SerializedName("request_id") val requestId: String, val message: String, @SerializedName("user_id") val userId: String, @SerializedName("workspace_id") val workspaceId: String, @SerializedName("project_id") val projectId: String? = null, @SerializedName("conversation_id") val conversationId: String? = null, @SerializedName("skill_key") val skillKey: String = "ai_admin", @SerializedName("ceo_mode") val ceoMode: Boolean = false)
data class FastChatResponse(@SerializedName("request_id") val requestId: String?, @SerializedName("conversation_id") val conversationId: String?, val reply: String?, val message: String?, val provider: String?, val model: String?, val status: String?, val verification: VerificationBlock?)
data class GatewayExecuteRequest(@SerializedName("request_id") val requestId: String, @SerializedName("user_id") val userId: String, @SerializedName("workspace_id") val workspaceId: String, @SerializedName("project_id") val projectId: String? = null, @SerializedName("conversation_id") val conversationId: String? = null, val intent: String, @SerializedName("source_mode") val sourceMode: String = "manual", val payload: JsonObject)
data class GatewayErrorItem(val category: String?, val message: String?)
data class GatewayWarningItem(val message: String?)
data class VerificationBlock(val status: String?, val evidence: List<String>?, val persisted: Boolean?, @SerializedName("gateway_persisted") val gatewayPersisted: Boolean?, @SerializedName("live_verified") val liveVerified: Boolean?, @SerializedName("delivered_confirmed") val deliveredConfirmed: Boolean?)
data class GatewayExecuteResponse(@SerializedName("request_id") val requestId: String?, @SerializedName("task_id") val taskId: String?, val status: String?, val result: JsonObject?, val errors: List<GatewayErrorItem>?, val warnings: List<GatewayWarningItem>?, val verification: VerificationBlock?)
data class ChatMessageUi(val id: String, val role: String, val content: String, val timestamp: String, val verificationStatus: String = "verified", val provider: String = "system", val isError: Boolean = false)
data class ProjectUi(val id: String, val name: String)
data class SkillUi(val key: String, val name: String)
data class MemoryWriteRequest(val scope: String, val key: String?, val content: String, val category: String, @SerializedName("project_id") val projectId: String? = null)

// Updated Tasks Model
data class TaskItemUi(val id: String, val title: String, val status: String, val priority: String, val createdAt: String, val updatedAt: String)
data class MemoryItemUi(val id: String, val scope: String, val key: String?, val content: String, val category: String, val createdAt: String)
