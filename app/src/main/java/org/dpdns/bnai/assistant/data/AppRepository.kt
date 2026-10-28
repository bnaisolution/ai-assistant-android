package org.dpdns.bnai.assistant.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

class AppRepository(
    val api: ApiService,
    val tokenManager: TokenManager
) {
    private val gson = Gson()
    private fun newRequestId(): String = UUID.randomUUID().toString()

    suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            val resp = api.login(LoginRequest(email, pass))
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                val jwt = b.token ?: b.accessToken ?: "session_active"
                val uid = b.userId ?: tokenManager.getUserId()
                val wid = b.workspaceId ?: tokenManager.getWorkspaceId()
                val role = b.role ?: if (email.contains("admin", ignoreCase = true)) "owner_admin" else "normal_user"
                tokenManager.saveSession(jwt, uid, wid, email, role)
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifySessionMe(): Result<JsonObject> = safeCall { api.getMe() }

    suspend fun sendFastChat(
        message: String,
        projectId: String?,
        conversationId: String?,
        skillKey: String,
        ceoMode: Boolean
    ): Result<ChatMessageUi> {
        val reqId = newRequestId()
        return try {
            val req = FastChatRequest(
                requestId = reqId,
                message = message,
                userId = tokenManager.getUserId(),
                workspaceId = tokenManager.getWorkspaceId(),
                projectId = projectId,
                conversationId = conversationId,
                skillKey = skillKey,
                ceoMode = ceoMode
            )
            val resp = api.fastChat(req)
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                val replyText = b.reply
                    ?: b.message
                    ?: b.result?.asJsonObject?.get("assistant_message")?.asString
                    ?: b.result?.toString()
                    ?: "Verified response received."
                Result.success(
                    ChatMessageUi(
                        id = reqId,
                        role = "assistant",
                        content = replyText,
                        timestamp = "Just now",
                        verificationStatus = "verified",
                        provider = b.provider ?: "groq (openai/gpt-oss-120b)"
                    )
                )
            } else {
                executeGatewayIntent("CHAT", projectId, conversationId, JsonObject().apply {
                    addProperty("message", message)
                }).map { gw ->
                    val childRes = gw.result?.getAsJsonObject("child_result")
                    val msg = childRes?.get("assistant_message")?.asString
                        ?: gw.errors?.firstOrNull()?.message
                        ?: "Gateway response status: ${gw.status}"
                    ChatMessageUi(
                        id = gw.requestId ?: reqId,
                        role = "assistant",
                        content = msg,
                        timestamp = "Just now",
                        verificationStatus = gw.verification?.status ?: "not_verified",
                        provider = childRes?.get("provider_used")?.asString ?: "wf00-gateway",
                        isError = gw.status == "failed",
                        rawJsonResult = gson.toJson(gw)
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeGatewayIntent(
        intent: String,
        projectId: String?,
        conversationId: String?,
        payload: JsonObject,
        sourceMode: String = "manual"
    ): Result<GatewayExecuteResponse> {
        return try {
            val req = GatewayExecuteRequest(
                requestId = newRequestId(),
                userId = tokenManager.getUserId(),
                workspaceId = tokenManager.getWorkspaceId(),
                projectId = projectId,
                conversationId = conversationId,
                intent = intent,
                sourceMode = sourceMode,
                payload = payload
            )
            val resp = api.executeWorkflow(req)
            val body = resp.body()
            if (body != null) {
                Result.success(body)
            } else {
                val errRaw = resp.errorBody()?.string()
                val parsedErr = try {
                    gson.fromJson(errRaw, GatewayExecuteResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                if (parsedErr != null) Result.success(parsedErr)
                else Result.failure(Exception("HTTP ${resp.code()}: ${errRaw ?: "Empty response"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSkillsAndTools(): Result<JsonObject> = safeCall { api.listSkills() }
    suspend fun analyzeMasterBlueprint(blueprintText: String, projectId: String?, skillKey: String): Result<JsonObject> = safeCall {
        api.analyzeBlueprint(JsonObject().apply {
            addProperty("blueprint_text", blueprintText)
            addProperty("project_id", projectId)
            addProperty("skill_key", skillKey)
        })
    }
    suspend fun recordRepairAttempt(buildJobId: String, passed: Boolean, issues: List<String>): Result<JsonObject> = safeCall {
        val arr = JsonArray().apply { issues.forEach { add(it) } }
        api.recordRepairLoop(JsonObject().apply {
            addProperty("build_job_id", buildJobId)
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("validation_passed", passed)
            add("issues", arr)
        })
    }

    suspend fun fetchCopilotDashboard(): Result<JsonObject> = safeCall { api.getCopilotDashboard() }
    suspend fun diagnoseWithCopilot(req: CopilotDiagnoseRequest): Result<JsonObject> = safeCall { api.diagnoseError(req) }
    suspend fun analyzeScreenshotWithCopilot(ocrText: String, question: String): Result<JsonObject> = safeCall {
        api.analyzeScreenshot(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("ocr_extracted_text", ocrText)
            addProperty("user_question", question)
        })
    }
    suspend fun fetchNamespacedWorkflows(namespace: String? = null): Result<JsonObject> = safeCall {
        api.listNamespacedWorkflows(namespace)
    }
    suspend fun computeDiff(req: DiffViewerRequest): Result<JsonObject> = safeCall { api.computeWorkflowDiff(req) }
    suspend fun fetchCopilotHistory(): Result<JsonObject> = safeCall { api.getCopilotHistory() }

    suspend fun runSandboxPreflight(targetWf: String, purpose: String): Result<JsonObject> = safeCall {
        api.sandboxPreflight(JsonObject().apply {
            addProperty("target_workflow", targetWf)
            addProperty("diagnostic_purpose", purpose)
        })
    }
    suspend fun runSandboxExecution(targetWf: String, confirmed: Boolean): Result<JsonObject> = safeCall {
        api.sandboxExecute(SandboxExecuteRequest(targetWorkflow = targetWf, userConfirmed = confirmed))
    }
    suspend fun applyProtectedChange(req: ProtectedChangeExecuteRequest): Result<JsonObject> = safeCall {
        api.executeProtectedChange(req)
    }
    suspend fun fetchRecoveryPoints(): Result<JsonObject> = safeCall { api.getRecoveryPoints() }
    suspend fun executeRollback(
        targetResource: String,
        targetVersionHash: String,
        expectedCurrentHash: String?,
        approvalToken: String
    ): Result<JsonObject> = safeCall {
        api.executeRollback(JsonObject().apply {
            addProperty("target_resource", targetResource)
            addProperty("target_version_hash", targetVersionHash)
            if (!expectedCurrentHash.isNullOrBlank()) {
                addProperty("expected_current_hash", expectedCurrentHash)
            }
            addProperty("approval_token", approvalToken)
        })
    }

    suspend fun fetchApprovalsList(): Result<JsonObject> = safeCall { api.listApprovals() }
    suspend fun decideApproval(approvalId: String, decision: String, note: String?): Result<JsonObject> = safeCall {
        api.decideApproval(ApprovalDecisionRequest(approvalId, decision, note))
    }
    suspend fun requestNewApproval(actionType: String, resourceType: String, resourceId: String, reason: String): Result<JsonObject> = safeCall {
        api.requestApproval(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("action_type", actionType)
            addProperty("resource_type", resourceType)
            addProperty("resource_id", resourceId)
            addProperty("reason", reason)
        })
    }

    suspend fun fetchCinematicStudio(): Result<JsonObject> = safeCall { api.getCinematicStudio() }
    suspend fun triggerCinematicStage(stageId: String, op: String): Result<JsonObject> = safeCall {
        api.triggerCinematicStage(CinematicStageActionRequest(stageId = stageId, operation = op))
    }
    suspend fun fetchCinematicCopilotHandoff(episode: String, scene: String, stage: String, errorMsg: String): Result<JsonObject> = safeCall {
        api.getCinematicCopilotContext(JsonObject().apply {
            addProperty("current_episode", episode)
            addProperty("current_scene", scene)
            addProperty("failed_production_stage", stage)
            addProperty("relevant_error", errorMsg)
        })
    }

    suspend fun fetchRetentionPolicies(): Result<JsonObject> = safeCall { api.getRetentionPolicies() }
    suspend fun readScopedMemory(scope: String, projectId: String?): Result<JsonObject> = safeCall {
        api.readMemory(JsonObject().apply {
            addProperty("scope", scope)
            addProperty("project_id", projectId ?: tokenManager.getActiveProjectId())
            addProperty("limit", 30)
        })
    }
    suspend fun writeScopedMemory(scope: String, key: String, content: String, category: String): Result<JsonObject> = safeCall {
        api.writeMemory(JsonObject().apply {
            addProperty("scope", scope)
            if (key.isNotBlank()) addProperty("key", key)
            addProperty("content", content)
            addProperty("category", category)
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("knowledge_status", "observed")
        })
    }
    suspend fun deleteScopedMemory(memoryId: String): Result<JsonObject> = safeCall {
        api.deleteMemory(JsonObject().apply { addProperty("memory_id", memoryId) })
    }

    suspend fun fetchTasksList(): Result<JsonObject> = safeCall { api.listTasks() }
    suspend fun cancelTaskOnBackend(taskId: String): Result<JsonObject> = safeCall {
        api.cancelTask(JsonObject().apply { addProperty("task_id", taskId) })
    }
    suspend fun reconcileTaskOnBackend(taskId: String, resolution: String): Result<JsonObject> = safeCall {
        api.reconcileTask(JsonObject().apply {
            addProperty("task_id", taskId)
            addProperty("resolution", resolution)
        })
    }

    suspend fun fetchNotificationsList(): Result<JsonObject> = safeCall { api.listNotifications() }
    suspend fun dispatchNotification(title: String, message: String, channel: String, priority: String): Result<JsonObject> = safeCall {
        api.dispatchNotification(JsonObject().apply {
            addProperty("title", title)
            addProperty("message", message)
            addProperty("channel", channel)
            addProperty("priority", priority)
        })
    }
    suspend fun uploadFileMetadata(fileName: String, mimeType: String, sizeBytes: Long, artifactType: String, preview: String): Result<JsonObject> = safeCall {
        api.uploadFileMetadata(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("file_name", fileName)
            addProperty("mime_type", mimeType)
            addProperty("size_bytes", sizeBytes)
            addProperty("artifact_type", artifactType)
            addProperty("content_preview", preview)
        })
    }
    suspend fun exportProjectData(format: String): Result<JsonObject> = safeCall {
        api.exportProjectData(JsonObject().apply {
            addProperty("project_id", tokenManager.getActiveProjectId())
            addProperty("format", format)
        })
    }
    suspend fun deleteFileArtifact(artifactId: String): Result<JsonObject> = safeCall {
        api.deleteFileArtifact(JsonObject().apply {
            addProperty("artifact_id", artifactId)
            addProperty("project_id", tokenManager.getActiveProjectId())
        })
    }

    suspend fun fetchOwnerDashboard(): Result<JsonObject> = safeCall { api.getOwnerDashboard() }
    suspend fun fetchSchemaRegistry(): Result<JsonObject> = safeCall { api.getSchemaRegistry() }
    suspend fun executeOwnerAction(actionType: String, targetResource: String, approvalToken: String?, params: JsonObject? = null): Result<JsonObject> = safeCall {
        api.executeOwnerAction(JsonObject().apply {
            addProperty("action_type", actionType)
            addProperty("target_resource", targetResource)
            if (!approvalToken.isNullOrBlank()) addProperty("approval_token", approvalToken)
            if (params != null) add("parameters", params)
        })
    }
    suspend fun fetchUserDashboard(): Result<JsonObject> = safeCall { api.getUserDashboard() }
    suspend fun fetchCapabilitiesAudit(): Result<JsonObject> = safeCall { api.getCapabilities() }
    suspend fun fetchWorkflowAndContractRegistries(): Result<JsonObject> = safeCall { api.getWorkflowRegistry() }

    private suspend fun safeCall(call: suspend () -> retrofit2.Response<JsonObject>): Result<JsonObject> {
        return try {
            val resp = call()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                val errStr = resp.errorBody()?.string() ?: "HTTP ${resp.code()}"
                Result.failure(Exception(errStr))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseJsonObjectOrNull(raw: String): JsonObject? {
        return try {
            JsonParser.parseString(raw.trim()).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkBackendHealth(): Boolean {
        return try {
            api.checkHealth().isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
