package org.dpdns.bnai.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.assistant.BuildConfig
import org.dpdns.bnai.assistant.data.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MainViewModel(private val repo: AppRepository) : ViewModel() {
    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    // --- SESSION, ROLE & GLOBAL NAVIGATION STATE ---
    private val _isLoggedIn = MutableStateFlow(repo.tokenManager.getToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isOwner = MutableStateFlow(repo.tokenManager.isOwner())
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _backendHealthy = MutableStateFlow(true)
    val backendHealthy: StateFlow<Boolean> = _backendHealthy.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // --- TWO LOGICAL PROJECTS ---
    val projects = listOf(
        ProjectUi(
            id = BuildConfig.ASSISTANT_PROJECT_ID,
            namespace = "Assistant",
            name = "Project A — AI Assistant App",
            description = "Central AI Brain, Copilot, WF-00..WF-12 & System Operations",
            status = "Active"
        ),
        ProjectUi(
            id = BuildConfig.CINEMATIC_PROJECT_ID,
            namespace = "Cinematic",
            name = "Project B — AI Cinematic Engine",
            description = "Story Bible, Character Continuity, Scenes & Media Pipeline (WF-00..WF-13)",
            status = "Active"
        )
    )

    val skills = listOf(
        SkillUi("ai_admin", "AI Admin (Default)", "Hinglish Assistant, Context & Minimum Workflow Routing", false),
        SkillUi("n8n_wf_builder", "n8n WF Builder", "Blueprint Intake -> Generate -> Validate -> Max 5x Repair Loop", false),
        SkillUi("copilot", "AI Copilot & Troubleshooter", "Error Diagnosis, Screenshot Analysis & Diff Viewer", false),
        SkillUi("cinematic_director", "Cinematic Director", "Story Bible, Characters, Continuity & Scene Pipeline", false),
        SkillUi("owner_admin", "Owner Admin Console", "System Health, Schema Drift, Protected Deploy & Approvals", true)
    )

    val sessionModes = listOf("Auto", "Vision", "Story", "n8n Assistant", "Admin")

    private val _selectedProject = MutableStateFlow(projects.first())
    val selectedProject: StateFlow<ProjectUi> = _selectedProject.asStateFlow()

    private val _selectedSkill = MutableStateFlow(skills.first())
    val selectedSkill: StateFlow<SkillUi> = _selectedSkill.asStateFlow()

    private val _sessionMode = MutableStateFlow(repo.tokenManager.getSessionMode())
    val sessionMode: StateFlow<String> = _sessionMode.asStateFlow()

    private val _ceoMode = MutableStateFlow(false)
    val ceoMode: StateFlow<Boolean> = _ceoMode.asStateFlow()

    private val _useFastPath = MutableStateFlow(true)
    val useFastPath: StateFlow<Boolean> = _useFastPath.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessageUi(
                id = "welcome-v31",
                role = "assistant",
                content = "Namaste! Main aapka Unified BNAI Primary AI Brain (v3.1) hun. Project A (AI Assistant) aur Project B (AI Cinematic Engine) dono active hain. Aaj hum kis par kaam karein?",
                timestamp = "Ready",
                verificationStatus = "verified",
                provider = "system"
            )
        )
    )
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    private val _wfBuilderOutput = MutableStateFlow("")
    val wfBuilderOutput: StateFlow<String> = _wfBuilderOutput.asStateFlow()

    private val _lastGeneratedWfJson = MutableStateFlow("")
    val lastGeneratedWfJson: StateFlow<String> = _lastGeneratedWfJson.asStateFlow()

    private val _repairAttemptCount = MutableStateFlow(0)
    val repairAttemptCount: StateFlow<Int> = _repairAttemptCount.asStateFlow()

    private val _copilotConsoleOutput = MutableStateFlow("Select a diagnostic tool, screenshot analyzer, workflow inspector, diff check, sandbox, or rollback operation.")
    val copilotConsoleOutput: StateFlow<String> = _copilotConsoleOutput.asStateFlow()

    private val _lastBaseVersionHash = MutableStateFlow("")
    val lastBaseVersionHash: StateFlow<String> = _lastBaseVersionHash.asStateFlow()

    private val _lastProposedChangeHash = MutableStateFlow("")
    val lastProposedChangeHash: StateFlow<String> = _lastProposedChangeHash.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItemUi>>(emptyList())
    val tasks: StateFlow<List<TaskItemUi>> = _tasks.asStateFlow()

    private val _approvals = MutableStateFlow<List<ApprovalItemUi>>(emptyList())
    val approvals: StateFlow<List<ApprovalItemUi>> = _approvals.asStateFlow()

    private val _lastIssuedApprovalToken = MutableStateFlow<String?>(null)
    val lastIssuedApprovalToken: StateFlow<String?> = _lastIssuedApprovalToken.asStateFlow()

    private val _reminders = MutableStateFlow<List<ReminderItemUi>>(emptyList())
    val reminders: StateFlow<List<ReminderItemUi>> = _reminders.asStateFlow()

    private val _projectProgressSummary = MutableStateFlow("Tap 'Refresh Progress (WF-06)' or 'Sync Live DB Tasks & Approvals'.")
    val projectProgressSummary: StateFlow<String> = _projectProgressSummary.asStateFlow()

    private val _cinematicStages = MutableStateFlow<List<CinematicStageUi>>(
        listOf(
            CinematicStageUi("STG-01", "Story Bible", "Cinematic / WF-01", "Completed", "Verified"),
            CinematicStageUi("STG-02", "Seasons and Episodes", "Cinematic / WF-02", "Completed", "Verified"),
            CinematicStageUi("STG-03", "Scripts", "Cinematic / WF-03", "Completed", "Verified"),
            CinematicStageUi("STG-04", "Characters", "Cinematic / WF-04", "Completed", "Verified"),
            CinematicStageUi("STG-05", "Character References", "Cinematic / WF-05", "Completed", "Verified"),
            CinematicStageUi("STG-06", "Continuity", "Cinematic / WF-06", "Completed", "Verified"),
            CinematicStageUi("STG-07", "Scenes", "Cinematic / WF-07", "Running", "Active"),
            CinematicStageUi("STG-08", "Image Assets", "Cinematic / WF-08", "Queued", "Pending"),
            CinematicStageUi("STG-09", "Animation Assets", "Cinematic / WF-09", "Not Started", "Idle"),
            CinematicStageUi("STG-10", "Voice and Emotion", "Cinematic / WF-10", "Not Started", "Idle"),
            CinematicStageUi("STG-11", "Audio and BGM", "Cinematic / WF-11", "Not Started", "Idle"),
            CinematicStageUi("STG-12", "SFX", "Cinematic / WF-11", "Not Started", "Idle"),
            CinematicStageUi("STG-13", "Rendering", "Cinematic / WF-12", "Waiting for Approval", "Gate"),
            CinematicStageUi("STG-14", "Quality Control", "Cinematic / WF-13", "Not Started", "Idle"),
            CinematicStageUi("STG-15", "Publishing", "Cinematic / WF-13", "Waiting for Approval", "Gate")
        )
    )
    val cinematicStages: StateFlow<List<CinematicStageUi>> = _cinematicStages.asStateFlow()

    private val _memoryItems = MutableStateFlow<List<MemoryItemUi>>(emptyList())
    val memoryItems: StateFlow<List<MemoryItemUi>> = _memoryItems.asStateFlow()

    private val _retentionPoliciesJson = MutableStateFlow("")
    val retentionPoliciesJson: StateFlow<String> = _retentionPoliciesJson.asStateFlow()

    private val _filesAndNotifsOutput = MutableStateFlow("Use the controls above to upload file metadata, export redacted data, dispatch notifications, or run UI-35 Capability Audit.")
    val filesAndNotifsOutput: StateFlow<String> = _filesAndNotifsOutput.asStateFlow()

    private val _ownerDashboardJson = MutableStateFlow("Tap 'Load Live Owner Telemetry' or 'Inspect 34-Table Schema' to view live governance data.")
    val ownerDashboardJson: StateFlow<String> = _ownerDashboardJson.asStateFlow()

    private val _incidents = MutableStateFlow(
        listOf(
            SecurityIncidentUi(
                id = "INC-301",
                severity = "WARNING",
                category = "Credential Rotation Pending",
                component = "WF-00 / Backend .env",
                status = "OWNER_ACTION_REQUIRED",
                timestamp = "Active Baseline",
                requiredAction = "Ensure x-service-token matches TRUSTED_SERVICE_TOKEN across WF-00"
            )
        )
    )
    val incidents: StateFlow<List<SecurityIncidentUi>> = _incidents.asStateFlow()

    init { refreshHealth() }

    fun refreshHealth() {
        viewModelScope.launch { _backendHealthy.value = repo.checkBackendHealth() }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.login(email, pass)
            _isLoading.value = false
            res.onSuccess {
                _isLoggedIn.value = true
                _isOwner.value = repo.tokenManager.isOwner()
                _statusBanner.value = "Authenticated as ${repo.tokenManager.getEmail()} (${repo.tokenManager.getRole()})"
            }.onFailure { e -> _statusBanner.value = "Authentication failed: ${e.message}" }
        }
    }

    fun logout() {
        repo.tokenManager.clearSession()
        _isLoggedIn.value = false
    }

    fun selectProject(project: ProjectUi) {
        _selectedProject.value = project
        repo.tokenManager.setActiveProjectId(project.id)
        if (project.namespace == "Cinematic" && _sessionMode.value == "Auto") setSessionMode("Story")
        _statusBanner.value = "Switched isolated context to ${project.namespace} (${project.name})"
    }

    fun selectSkill(skill: SkillUi) {
        if (skill.ownerOnly && !_isOwner.value) {
            _statusBanner.value = "Access Denied: Owner Admin role required (Backend enforced)"
            return
        }
        _selectedSkill.value = skill
    }

    fun setSessionMode(mode: String) {
        if (mode == "Admin" && !_isOwner.value) _statusBanner.value = "Mode Safety: Selecting Admin mode does not grant Owner privileges."
        _sessionMode.value = mode
        repo.tokenManager.setSessionMode(mode)
    }

    fun toggleCeoMode(enabled: Boolean) { _ceoMode.value = enabled }
    fun toggleFastPath(enabled: Boolean) { _useFastPath.value = enabled }
    fun clearBanner() { _statusBanner.value = null }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessageUi(id = UUID.randomUUID().toString(), role = "user", content = userText, timestamp = "Now")
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            _isLoading.value = true
            val pid = _selectedProject.value.id
            if (_useFastPath.value) {
                val res = repo.sendFastChat(userText, pid, null, _selectedSkill.value.key, _ceoMode.value)
                res.onSuccess { reply -> _messages.value = _messages.value + reply }
                .onFailure { err -> _messages.value = _messages.value + ChatMessageUi(id = UUID.randomUUID().toString(), role = "assistant", content = "Fast Chat Error: ${err.message}", timestamp = "Now", verificationStatus = "not_verified", isError = true) }
            } else {
                val payload = JsonObject().apply { addProperty("message", userText); addProperty("session_mode", _sessionMode.value) }
                val res = repo.executeGatewayIntent("CHAT", pid, null, payload)
                res.onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result")
                    val text = child?.get("assistant_message")?.asString ?: gw.errors?.firstOrNull()?.message ?: prettyGson.toJson(gw.result)
                    _messages.value = _messages.value + ChatMessageUi(id = gw.requestId ?: UUID.randomUUID().toString(), role = "assistant", content = text, timestamp = "Now", verificationStatus = gw.verification?.status ?: "not_verified", provider = child?.get("provider_used")?.asString ?: "WF-00", isError = gw.status == "failed", rawJsonResult = prettyGson.toJson(gw))
                }.onFailure { err -> _messages.value = _messages.value + ChatMessageUi(id = UUID.randomUUID().toString(), role = "assistant", content = "Gateway Error: ${err.message}", timestamp = "Now", verificationStatus = "not_verified", isError = true) }
            }
            _isLoading.value = false
        }
    }

    fun generateAndAutoRepairWorkflow(requirements: String, targetVersion: String = "1.80.0") {
        if (requirements.isBlank()) { _wfBuilderOutput.value = "Error: Requirements text is required for WF-02 generation."; return }
        viewModelScope.launch {
            _isLoading.value = true
            _repairAttemptCount.value = 0
            val buildJobId = UUID.randomUUID().toString()
            val logBuilder = StringBuilder("=== STEP 1: WF-02 Generating Workflow JSON (Job: ${buildJobId.take(8)}) ===\n")
            
            val genPayload = JsonObject().apply {
                addProperty("requirements", requirements)
                addProperty("target_n8n_version", targetVersion)
                addProperty("blueprint_reference", "Unified Master Blueprint v3.1")
            }
            val genRes = repo.executeGatewayIntent("GENERATE_WORKFLOW", _selectedProject.value.id, null, genPayload)
            val genEnv = genRes.getOrNull()
            val childRes = genEnv?.result?.getAsJsonObject("child_result") ?: genEnv?.result
            var currentWfJson = childRes?.getAsJsonObject("generated_workflow_json")

            if (currentWfJson == null) {
                logBuilder.appendLine("Generation failed or returned no JSON:\n${prettyGson.toJson(genEnv)}")
                _wfBuilderOutput.value = logBuilder.toString()
                _isLoading.value = false
                return@launch
            }

            _lastGeneratedWfJson.value = prettyGson.toJson(currentWfJson)
            logBuilder.appendLine("WF-02 generated initial workflow JSON successfully.")

            var passedValidation = false
            for (attempt in 1..5) {
                logBuilder.appendLine("\n=== VALIDATION CHECK (WF-04) — Attempt #$attempt ===")
                val valPayload = JsonObject().apply { add("workflow_json", currentWfJson); addProperty("target_n8n_version", targetVersion) }
                val valRes = repo.executeGatewayIntent("VALIDATE_WORKFLOW", _selectedProject.value.id, null, valPayload)
                val valEnv = valRes.getOrNull()
                val valChild = valEnv?.result?.getAsJsonObject("child_result") ?: valEnv?.result
                val vStatus = valChild?.get("validation_status")?.asString?.lowercase() ?: ""

                if (valEnv?.status == "completed" && (vStatus == "passed" || vStatus == "valid" || valEnv.errors.isNullOrEmpty())) {
                    logBuilder.appendLine("WF-04 Validation PASSED on check #$attempt.")
                    repo.recordRepairAttempt(buildJobId, true, emptyList())
                    passedValidation = true
                    break
                }

                _repairAttemptCount.value = attempt
                val issueSummary = valEnv?.errors?.joinToString("; ") { it.message ?: "" } ?: valChild?.get("failed_tests")?.toString() ?: "Fix validation structure and node parameters"
                repo.recordRepairAttempt(buildJobId, false, listOf(issueSummary))
                logBuilder.appendLine("Validation issues detected: $issueSummary\nRunning WF-03 Auto-Repair Attempt #$attempt of 5...")

                val updPayload = JsonObject().apply { add("original_workflow_json", currentWfJson); addProperty("requested_changes", "Repair validation issues: $issueSummary") }
                val updRes = repo.executeGatewayIntent("UPDATE_WORKFLOW", _selectedProject.value.id, null, updPayload)
                val updEnv = updRes.getOrNull()
                val updChild = updEnv?.result?.getAsJsonObject("child_result") ?: updEnv?.result
                val repairedJson = updChild?.getAsJsonObject("updated_workflow_json")

                if (repairedJson != null) {
                    currentWfJson = repairedJson
                    _lastGeneratedWfJson.value = prettyGson.toJson(currentWfJson)
                    logBuilder.appendLine("WF-03 Repair #$attempt applied.")
                } else {
                    logBuilder.appendLine("WF-03 Repair #$attempt returned no updated JSON. Halting loop.")
                    break
                }
            }

            if (!passedValidation) logBuilder.appendLine("\n[WARNING] Unresolved issues remain after ${_repairAttemptCount.value} repair attempts. Do not deploy without manual review.")
            logBuilder.appendLine("\n=== FINAL WORKFLOW JSON ===\n${prettyGson.toJson(currentWfJson)}")
            _wfBuilderOutput.value = logBuilder.toString()
            _isLoading.value = false
        }
    }

    fun analyzeWorkflowWf01(workflowJsonStr: String, mode: String = "full_audit", errorLogs: String = "") {
        val parsed = repo.parseJsonObjectOrNull(workflowJsonStr)
        if (parsed == null) { _wfBuilderOutput.value = "Error: Invalid Workflow JSON object."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { add("workflow_json", parsed); addProperty("analysis_mode", mode); if (errorLogs.isNotBlank()) addProperty("error_logs", errorLogs) }
            repo.executeGatewayIntent("ANALYZE_WORKFLOW", _selectedProject.value.id, null, payload, "uploaded")
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-01 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun updateWorkflowWf03(originalJsonStr: String, requestedChanges: String) {
        val parsed = repo.parseJsonObjectOrNull(originalJsonStr)
        if (parsed == null || requestedChanges.isBlank()) { _wfBuilderOutput.value = "Error: Valid original_workflow_json and requested_changes required."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { add("original_workflow_json", parsed); addProperty("requested_changes", requestedChanges) }
            repo.executeGatewayIntent("UPDATE_WORKFLOW", _selectedProject.value.id, null, payload)
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    child?.getAsJsonObject("updated_workflow_json")?.let { _lastGeneratedWfJson.value = prettyGson.toJson(it) }
                    _wfBuilderOutput.value = prettyGson.toJson(gw)
                }.onFailure { _wfBuilderOutput.value = "WF-03 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun researchDocsWf05(question: String, docVersion: String = "1.80.0") {
        if (question.length < 5) { _wfBuilderOutput.value = "Question must be at least 5 characters for WF-05."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { addProperty("question", question); addProperty("documentation_version", docVersion) }
            repo.executeGatewayIntent("RESEARCH_DOCUMENTATION", _selectedProject.value.id, null, payload)
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-05 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun listLiveWorkflowsWf09() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeGatewayIntent("LIST_LIVE_WORKFLOWS", _selectedProject.value.id, null, JsonObject(), "live")
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-09 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun getLiveWorkflowWf09(workflowId: String) {
        if (workflowId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { addProperty("workflow_id", workflowId.trim()) }
            repo.executeGatewayIntent("GET_LIVE_WORKFLOW", _selectedProject.value.id, null, payload, "live")
                .onSuccess { _wfBuilderOutput.value = prettyGson.toJson(it) }
                .onFailure { _wfBuilderOutput.value = "WF-09 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runCopilotDiagnosis(wfId: String, failedNode: String, errorMsg: String, logs: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = CopilotDiagnoseRequest(projectId = _selectedProject.value.id, workflowId = wfId.ifBlank { null }, failedNode = failedNode.ifBlank { null }, errorMessage = errorMsg, executionLogs = logs.ifBlank { null })
            repo.diagnoseWithCopilot(req)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Copilot Diagnosis Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun analyzeScreenshotOcr(ocrText: String, userQuestion: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.analyzeScreenshotWithCopilot(ocrText, userQuestion)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Screenshot Analysis Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun inspectNamespacedWorkflows(namespaceFilter: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchNamespacedWorkflows(namespaceFilter)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Workflow Inspector Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadCopilotDiagnosticHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchCopilotHistory()
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Copilot History Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runWorkflowDiffViewer(wfId: String, origJsonStr: String, propJsonStr: String) {
        val orig = repo.parseJsonObjectOrNull(origJsonStr)
        val prop = repo.parseJsonObjectOrNull(propJsonStr)
        if (orig == null || prop == null) { _copilotConsoleOutput.value = "Error: Both Original JSON and Proposed JSON must be valid JSON objects."; return }
        viewModelScope.launch {
            _isLoading.value = true
            val req = DiffViewerRequest(workflowId = wfId.ifBlank { "WF-00" }, projectNamespace = _selectedProject.value.namespace, originalJson = orig, proposedJson = prop)
            repo.computeDiff(req)
                .onSuccess { res ->
                    _lastBaseVersionHash.value = res.get("base_version_hash")?.asString ?: ""
                    _lastProposedChangeHash.value = res.get("proposed_change_hash")?.asString ?: ""
                    _copilotConsoleOutput.value = prettyGson.toJson(res)
                }.onFailure { _copilotConsoleOutput.value = "Diff Viewer Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runSandboxDiagnostic(targetWf: String, userConfirmed: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = if (!userConfirmed) repo.runSandboxPreflight(targetWf, "Isolated structural & runtime preflight check")
                      else repo.runSandboxExecution(targetWf, true)
            res.onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
               .onFailure { _copilotConsoleOutput.value = "Sandbox Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun executeProtectedLiveChange(targetResource: String, action: String, approvalToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = ProtectedChangeExecuteRequest(projectId = _selectedProject.value.id, targetResource = targetResource, action = action, approvalToken = approvalToken, expectedBaseVersionHash = _lastBaseVersionHash.value.ifBlank { null }, proposedChangeHash = _lastProposedChangeHash.value.ifBlank { null })
            repo.applyProtectedChange(req)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Protected Change Blocked/Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadRecoveryPoints() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchRecoveryPoints()
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Recovery Points Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun executeResourceRollback(targetResource: String, targetVersionHash: String, approvalToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeRollback(targetResource, targetVersionHash, _lastBaseVersionHash.value.ifBlank { null }, approvalToken)
                .onSuccess { _copilotConsoleOutput.value = prettyGson.toJson(it) }
                .onFailure { _copilotConsoleOutput.value = "Rollback Blocked/Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun syncLiveTasksAndApprovals() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchTasksList().onSuccess { res ->
                val list = mutableListOf<TaskItemUi>()
                res.getAsJsonArray("tasks")?.forEach { el ->
                    val obj = el.asJsonObject
                    val st = obj.get("status")?.asString?.uppercase() ?: "PENDING"
                    list.add(TaskItemUi(
                        id = obj.get("id")?.asString ?: UUID.randomUUID().toString(), title = obj.get("title")?.asString ?: "Task",
                        goal = obj.get("goal")?.asString ?: obj.get("blocked_reason")?.asString ?: "Task", status = st,
                        priority = obj.get("priority")?.asString ?: "normal", cancelEligible = st !in listOf("COMPLETED", "FAILED", "CANCELLED"),
                        retryEligible = st in listOf("FAILED", "PAUSED", "OUTCOME_UNKNOWN"), updatedAt = obj.get("updated_at")?.asString ?: ""
                    ))
                }
                _tasks.value = list
            }
            repo.fetchApprovalsList().onSuccess { res ->
                val list = mutableListOf<ApprovalItemUi>()
                res.getAsJsonArray("approvals")?.forEach { el ->
                    val obj = el.asJsonObject
                    list.add(ApprovalItemUi(
                        id = obj.get("id")?.asString ?: UUID.randomUUID().toString(), actionType = obj.get("action_type")?.asString ?: "ACTION",
                        resourceType = obj.get("resource_type")?.asString ?: "resource", resourceId = obj.get("resource_id")?.asString ?: "id",
                        status = obj.get("status")?.asString ?: "pending", riskCategory = "protected_system_change",
                        reason = obj.getAsJsonObject("metadata")?.get("reason")?.asString ?: "Approval required", expiresAt = obj.get("expires_at")?.asString ?: ""
                    ))
                }
                _approvals.value = list
            }
            _isLoading.value = false
        }
    }

    fun fetchProjectProgressWf06() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeGatewayIntent("GET_PROJECT_PROGRESS", _selectedProject.value.id, null, JsonObject())
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    val summary = child?.get("project_summary")?.asString ?: "Progress loaded."
                    val pendingApprovals = child?.get("pending_approvals")?.asInt ?: 0
                    val auditsCount = child?.get("workflow_audits_count")?.asInt ?: 0
                    _projectProgressSummary.value = "$summary\nPending Approvals: $pendingApprovals | Workflow Audits: $auditsCount"
                    syncLiveTasksAndApprovals()
                }.onFailure { _projectProgressSummary.value = "WF-06 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun createTaskWf08(taskType: String, goal: String, priority: String = "important") {
        if (taskType.isBlank() || goal.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val payload = JsonObject().apply { addProperty("task_type", taskType); addProperty("goal", goal); addProperty("description", goal); addProperty("priority", priority) }
            repo.executeGatewayIntent("EXECUTE_TASK", _selectedProject.value.id, null, payload)
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    val taskObj = child?.getAsJsonObject("task")
                    val newTaskId = taskObj?.get("id")?.asString ?: gw.taskId ?: UUID.randomUUID().toString()
                    val newItem = TaskItemUi(id = newTaskId, title = taskType, goal = goal, status = (taskObj?.get("status")?.asString ?: "CREATED").uppercase(), priority = priority)
                    _tasks.value = listOf(newItem) + _tasks.value
                    _statusBanner.value = "Task created via WF-08 (${newTaskId.take(8)}...)"
                }.onFailure { _statusBanner.value = "Create Task Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun requestTaskCancellation(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.cancelTaskOnBackend(taskId).onSuccess { res ->
                val cState = res.get("cancellation_state")?.asString ?: "cancellation_requested"
                _tasks.value = _tasks.value.map { if (it.id == taskId) it.copy(status = cState.uppercase(), cancelEligible = false, retryEligible = true) else it }
                _statusBanner.value = "Backend Task Cancellation State: $cState"
            }.onFailure { err -> _statusBanner.value = "Cancel Request Error: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun reconcileTaskOutcome(taskId: String, resolution: String = "resume_check") {
        viewModelScope.launch {
            _isLoading.value = true
            repo.reconcileTaskOnBackend(taskId, resolution).onSuccess { res ->
                val taskObj = res.getAsJsonObject("task")
                val updatedStatus = (taskObj?.get("status")?.asString ?: "RECONCILED").uppercase()
                _tasks.value = _tasks.value.map { if (it.id == taskId) it.copy(status = updatedStatus, retryEligible = false) else it }
                _statusBanner.value = "Task ${taskId.take(8)} reconciled -> $updatedStatus"
            }.onFailure { err -> _statusBanner.value = "Reconciliation Failed: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun createApprovalRequest(actionType: String, resourceType: String, resourceId: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.requestNewApproval(actionType, resourceType, resourceId, reason)
                .onSuccess { res ->
                    val appr = res.getAsJsonObject("approval")
                    val id = appr?.get("id")?.asString ?: UUID.randomUUID().toString()
                    val item = ApprovalItemUi(id = id, actionType = actionType, resourceType = resourceType, resourceId = resourceId, status = "pending", riskCategory = "protected_system_change", reason = reason, expiresAt = appr?.get("expires_at")?.asString ?: "+60m")
                    _approvals.value = listOf(item) + _approvals.value
                    _statusBanner.value = "Approval Request Created ($id)"
                }.onFailure { _statusBanner.value = "Approval Request Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun submitApprovalDecision(approvalId: String, decision: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.decideApproval(approvalId, decision, "Decided from Android v1.1 Approval Inbox")
                .onSuccess { res ->
                    val tok = res.get("approval_token")?.asString
                    if (!tok.isNullOrBlank()) _lastIssuedApprovalToken.value = tok
                    _approvals.value = _approvals.value.map { if (it.id == approvalId) it.copy(status = decision, issuedToken = tok) else it }
                    _statusBanner.value = "Approval $decision. ${if (tok != null) "SHA-256 Token issued!" else ""}"
                }.onFailure { _statusBanner.value = "Decision Rejected: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun scheduleReminderWf10(title: String, minutesFromNow: Long = 30, priority: String = "important") {
        if (title.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val scheduledIso = Instant.now().plus(minutesFromNow, ChronoUnit.MINUTES).toString()
            val payload = JsonObject().apply { addProperty("title", title); addProperty("scheduled_at", scheduledIso); addProperty("priority", priority); addProperty("reminder_type", "task_followup") }
            repo.executeGatewayIntent("SET_REMINDER", _selectedProject.value.id, null, payload)
                .onSuccess { gw ->
                    val child = gw.result?.getAsJsonObject("child_result") ?: gw.result
                    val remId = child?.get("reminder_id")?.asString ?: UUID.randomUUID().toString()
                    _reminders.value = listOf(ReminderItemUi(remId, title, scheduledIso, "scheduled", priority)) + _reminders.value
                    _statusBanner.value = "Reminder scheduled via WF-10 for $scheduledIso"
                }.onFailure { _statusBanner.value = "WF-10 Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun syncLiveCinematicStudio() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchCinematicStudio().onSuccess { res ->
                val list = mutableListOf<CinematicStageUi>()
                res.getAsJsonArray("pipeline_sections")?.forEach { el ->
                    val obj = el.asJsonObject
                    list.add(CinematicStageUi(
                        stageId = obj.get("stage_id")?.asString ?: "STG", sectionName = obj.get("section_name")?.asString ?: "Stage",
                        mappedWorkflow = obj.get("mapped_workflow")?.asString ?: "Cinematic / WF", status = obj.get("status")?.asString ?: "Not Started",
                        lastUpdated = obj.get("last_updated")?.asString ?: ""
                    ))
                }
                if (list.isNotEmpty()) _cinematicStages.value = list
            }
            _isLoading.value = false
        }
    }

    fun triggerCinematicStageOperation(stageId: String, operation: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.triggerCinematicStage(stageId, operation)
                .onSuccess { res ->
                    val stg = res.getAsJsonObject("updated_stage")
                    val newStatus = stg?.get("status")?.asString ?: "Queued"
                    _cinematicStages.value = _cinematicStages.value.map { if (it.stageId == stageId) it.copy(status = newStatus, lastUpdated = "Just now") else it }
                    _statusBanner.value = "Cinematic Stage $stageId -> $newStatus"
                }.onFailure { _statusBanner.value = "Cinematic Operation Failed: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun syncScopedMemoryFromBackend(scope: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.readScopedMemory(scope, _selectedProject.value.id).onSuccess { res ->
                val loaded = mutableListOf<MemoryItemUi>()
                res.getAsJsonArray("memory_items")?.forEach { el ->
                    val obj = el.asJsonObject
                    loaded.add(MemoryItemUi(
                        id = obj.get("id")?.asString ?: UUID.randomUUID().toString(), scope = obj.get("scope")?.asString ?: scope,
                        key = if (obj.get("key")?.isJsonNull == false) obj.get("key")?.asString else null, content = obj.get("content")?.asString ?: "",
                        category = obj.get("category")?.asString ?: "general", knowledgeStatus = obj.get("knowledge_status")?.asString ?: "observed", confidence = 0.95
                    ))
                }
                _memoryItems.value = loaded
            }
            repo.fetchRetentionPolicies().onSuccess { pol -> _retentionPoliciesJson.value = prettyGson.toJson(pol) }
            _isLoading.value = false
        }
    }

    fun addLocalOrRemoteMemory(scope: String, key: String, content: String, category: String = "general") {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            repo.writeScopedMemory(scope, key, content, category).onSuccess { res ->
                val obj = res.getAsJsonObject("memory_item")
                val id = obj?.get("id")?.asString ?: UUID.randomUUID().toString()
                val newItem = MemoryItemUi(id = id, scope = scope, key = key.ifBlank { null }, content = content, category = category, knowledgeStatus = obj?.get("knowledge_status")?.asString ?: "observed", confidence = 0.95)
                _memoryItems.value = listOf(newItem) + _memoryItems.value
                _statusBanner.value = "Memory persisted to Supabase in scope '$scope' ($id)."
            }.onFailure { err -> _statusBanner.value = "Memory Write Error: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun requestMemoryDeletion(memoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.deleteScopedMemory(memoryId).onSuccess {
                _memoryItems.value = _memoryItems.value.filterNot { it.id == memoryId }
                _statusBanner.value = "Memory deleted from Supabase & recorded in data_jobs."
            }.onFailure { err -> _statusBanner.value = "Memory Delete Error: ${err.message}" }
            _isLoading.value = false
        }
    }

    fun uploadProjectFileMetadata(fileName: String, artifactType: String, preview: String) {
        if (fileName.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            repo.uploadFileMetadata(fileName, "application/json", preview.toByteArray().size.toLong().coerceAtLeast(128L), artifactType, preview)
                .onSuccess { _filesAndNotifsOutput.value = prettyGson.toJson(it) }
                .onFailure { _filesAndNotifsOutput.value = "File Upload Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun triggerRedactedProjectExport(format: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.exportProjectData(format)
                .onSuccess { _filesAndNotifsOutput.value = prettyGson.toJson(it) }
                .onFailure { _filesAndNotifsOutput.value = "Export Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadNotificationsAndUserDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            val combined = JsonObject()
            repo.fetchNotificationsList().onSuccess { combined.add("notifications_center", it) }
            repo.fetchUserDashboard().onSuccess { combined.add("user_dashboard_quotas", it) }
            _filesAndNotifsOutput.value = prettyGson.toJson(combined)
            _isLoading.value = false
        }
    }

    fun sendMultiChannelNotification(title: String, message: String, channel: String) {
        if (title.isBlank() || message.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            repo.dispatchNotification(title, message, channel, "important")
                .onSuccess { _filesAndNotifsOutput.value = prettyGson.toJson(it) }
                .onFailure { _filesAndNotifsOutput.value = "Notification Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun runUi35FinalCapabilityAudit() {
        viewModelScope.launch {
            _isLoading.value = true
            val auditObj = JsonObject()
            repo.fetchCapabilitiesAudit().onSuccess { auditObj.add("backend_v31_capabilities", it) }
            repo.fetchWorkflowAndContractRegistries().onSuccess { auditObj.add("n8n_workflow_registry", it) }
            _filesAndNotifsOutput.value = prettyGson.toJson(auditObj)
            _isLoading.value = false
        }
    }

    fun loadOwnerAdminTelemetry() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchOwnerDashboard()
                .onSuccess { _ownerDashboardJson.value = prettyGson.toJson(it) }
                .onFailure { _ownerDashboardJson.value = "Owner Access Denied / Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun loadOwnerSchemaRegistry() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.fetchSchemaRegistry()
                .onSuccess { _ownerDashboardJson.value = prettyGson.toJson(it) }
                .onFailure { _ownerDashboardJson.value = "Schema Registry Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun triggerOwnerConsoleAction(actionType: String, targetResource: String, approvalToken: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.executeOwnerAction(actionType, targetResource, approvalToken)
                .onSuccess { _ownerDashboardJson.value = prettyGson.toJson(it) }
                .onFailure { _ownerDashboardJson.value = "Owner Action Blocked/Failed: ${it.message}" }
            _isLoading.value = false
        }
    }
}
