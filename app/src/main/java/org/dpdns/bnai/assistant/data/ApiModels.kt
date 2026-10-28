package org.dpdns.bnai.assistant.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// --- AUTH MODELS ---
data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val token: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("workspace_id") val workspaceId: String?,
    val role: String?,
    val email: String?
)

// --- FAST PATH: DIRECT CHAT ---
data class FastChatRequest(
    @SerializedName("request_id") val requestId: String,
    val message: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("skill_key") val skillKey: String = "ai_admin",
    @SerializedName("ceo_mode") val ceoMode: Boolean = false
)

data class FastChatResponse(
    @SerializedName("request_id") val requestId: String?,
    val reply: String?,
    val message: String?,
    val result: JsonElement?,
    val provider: String?,
    val model: String?,
    val status: String?
)

// --- BACKGROUND PATH: GATEWAY ---
data class GatewayExecuteRequest(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("conversation_id") val conversationId: String? = null,
    val intent: String,
    @SerializedName("source_mode") val sourceMode: String = "manual",
    val payload: JsonObject
)

data class GatewayErrorItem(val category: String?, val message: String?)
data class GatewayWarningItem(val message: String?)

data class VerificationBlock(
    val status: String?,
    val evidence: List<String>?,
    val persisted: Boolean?,
    @SerializedName("gateway_persisted") val gatewayPersisted: Boolean?,
    @SerializedName("live_verified") val liveVerified: Boolean?,
    @SerializedName("delivered_confirmed") val deliveredConfirmed: Boolean?
)

data class GatewayExecuteResponse(
    @SerializedName("request_id") val requestId: String?,
    @SerializedName("task_id") val taskId: String?,
    val status: String?,
    val result: JsonObject?,
    val errors: List<GatewayErrorItem>?,
    val warnings: List<GatewayWarningItem>?,
    val verification: VerificationBlock?
)

// --- COPILOT & SECURITY MODELS ---
data class CopilotDiagnoseRequest(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("workflow_id") val workflowId: String? = null,
    @SerializedName("failed_node") val failedNode: String? = null,
    @SerializedName("error_message") val errorMessage: String,
    @SerializedName("execution_logs") val executionLogs: String? = null
)

data class DiffViewerRequest(
    @SerializedName("workflow_id") val workflowId: String,
    @SerializedName("project_namespace") val projectNamespace: String,
    @SerializedName("original_json") val originalJson: JsonObject,
    @SerializedName("proposed_json") val proposedJson: JsonObject
)

data class SandboxExecuteRequest(
    @SerializedName("target_workflow") val targetWorkflow: String,
    @SerializedName("user_confirmed") val userConfirmed: Boolean,
    @SerializedName("residual_risk_accepted") val residualRiskAccepted: Boolean = true
)

data class ProtectedChangeExecuteRequest(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("target_resource") val targetResource: String,
    val action: String,
    @SerializedName("approval_token") val approvalToken: String,
    @SerializedName("expected_base_version_hash") val expectedBaseVersionHash: String? = null,
    @SerializedName("proposed_change_hash") val proposedChangeHash: String? = null
)

data class ApprovalDecisionRequest(
    @SerializedName("approval_id") val approvalId: String,
    val decision: String, // "approved" | "rejected" | "cancelled"
    @SerializedName("decision_note") val decisionNote: String? = null
)

data class CinematicStageActionRequest(
    @SerializedName("stage_id") val stageId: String,
    val operation: String, // "queue" | "cancel" | "retry"
    @SerializedName("episode_id") val episodeId: String = "EP-01",
    @SerializedName("scene_id") val sceneId: String = "SCN-01"
)

// --- UI DOMAIN MODELS ---
data class ChatMessageUi(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: String,
    val verificationStatus: String = "verified",
    val provider: String = "groq",
    val isError: Boolean = false,
    val rawJsonResult: String? = null
)

data class ProjectUi(
    val id: String,
    val namespace: String, // "Assistant" | "Cinematic"
    val name: String,
    val description: String,
    val status: String
)

data class SkillUi(
    val key: String,
    val name: String,
    val description: String,
    val ownerOnly: Boolean = false
)

data class TaskItemUi(
    val id: String,
    val title: String,
    val goal: String,
    val status: String,
    val priority: String,
    val approvalToken: String? = null,
    val cancelEligible: Boolean = true,
    val retryEligible: Boolean = false,
    val updatedAt: String = ""
)

data class ApprovalItemUi(
    val id: String,
    val actionType: String,
    val resourceType: String,
    val resourceId: String,
    val status: String, // pending, approved, rejected, cancelled
    val riskCategory: String,
    val reason: String,
    val issuedToken: String? = null,
    val expiresAt: String = ""
)

data class MemoryItemUi(
    val id: String,
    val scope: String,
    val key: String?,
    val content: String,
    val category: String,
    val knowledgeStatus: String,
    val confidence: Double?
)

data class ReminderItemUi(
    val id: String,
    val title: String,
    val scheduledAt: String,
    val status: String,
    val priority: String
)

data class CinematicStageUi(
    val stageId: String,
    val sectionName: String,
    val mappedWorkflow: String,
    val status: String,
    val lastUpdated: String
)

data class SecurityIncidentUi(
    val id: String,
    val severity: String,
    val category: String,
    val component: String,
    val status: String,
    val timestamp: String,
    val requiredAction: String
)
