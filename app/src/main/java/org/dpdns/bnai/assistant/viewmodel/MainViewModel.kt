package org.dpdns.bnai.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.assistant.data.*
import java.util.UUID

class MainViewModel(private val repo: AppRepository) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(repo.tokenManager.getToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _isOwner = MutableStateFlow(repo.tokenManager.getRole().lowercase().contains("owner"))
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()
    
    private val _authStep = MutableStateFlow("LOGIN")
    val authStep: StateFlow<String> = _authStep.asStateFlow()
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()
    private val _backendHealthy = MutableStateFlow(true)
    val backendHealthy: StateFlow<Boolean> = _backendHealthy.asStateFlow()
    
    val projects = listOf(ProjectUi("11111111-1111-1111-1111-111111111111", "Default Project"))
    private val _selectedProject = MutableStateFlow(projects.first())
    val selectedProject: StateFlow<ProjectUi> = _selectedProject.asStateFlow()
    
    private val _skills = MutableStateFlow<List<SkillUi>>(emptyList())
    val skills: StateFlow<List<SkillUi>> = _skills.asStateFlow()
    private val _selectedSkill = MutableStateFlow(SkillUi("ai_admin", "AI Admin"))
    val selectedSkill: StateFlow<SkillUi> = _selectedSkill.asStateFlow()
    
    private val _aiModels = MutableStateFlow<List<AIModelConfig>>(emptyList())
    val aiModels: StateFlow<List<AIModelConfig>> = _aiModels.asStateFlow()
    private val _prompts = MutableStateFlow<List<PromptConfig>>(emptyList())
    val prompts: StateFlow<List<PromptConfig>> = _prompts.asStateFlow()

    private val _ceoMode = MutableStateFlow(false)
    val ceoMode: StateFlow<Boolean> = _ceoMode.asStateFlow()
    private val _useFastPath = MutableStateFlow(true)
    val useFastPath: StateFlow<Boolean> = _useFastPath.asStateFlow()
    private val _messages = MutableStateFlow(listOf(ChatMessageUi("welcome", "assistant", "Namaste! Main aapka BNAI Assistant (v3.0) hun.", "Ready", "verified", "system")))
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()
    
    private var currentConversationId: String? = null
    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()
    private val _builderOutput = MutableStateFlow("")
    val builderOutput: StateFlow<String> = _builderOutput.asStateFlow()
    private val _isDeploying = MutableStateFlow(false)
    val isDeploying: StateFlow<Boolean> = _isDeploying.asStateFlow()
    private val _lastGeneratedWfJson = MutableStateFlow<JsonObject?>(null)
    val lastGeneratedWfJson: StateFlow<JsonObject?> = _lastGeneratedWfJson.asStateFlow()
    
    private val _tasks = MutableStateFlow<List<TaskItemUi>>(emptyList())
    val tasks: StateFlow<List<TaskItemUi>> = _tasks.asStateFlow()
    private val _memoryItems = MutableStateFlow<List<MemoryItemUi>>(emptyList())
    val memoryItems: StateFlow<List<MemoryItemUi>> = _memoryItems.asStateFlow()

    fun setAuthStep(step: String) { _authStep.value = step; _authError.value = null }
    fun signup(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true; _authError.value = null
            repo.signup(email, pass).onSuccess { _authStep.value = "OTP"; _isLoading.value = false }.onFailure { _authError.value = it.message; _isLoading.value = false }
        }
    }
    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _isLoading.value = true; _authError.value = null
            repo.verifyOtp(email, otp).onSuccess { _authStep.value = "LOGIN"; _authError.value = "Verified! Please login."; _isLoading.value = false }.onFailure { _authError.value = it.message; _isLoading.value = false }
        }
    }

    init {
        viewModelScope.launch {
            _backendHealthy.value = repo.checkBackendHealth()
            if (_isLoggedIn.value) { loadSkills(); loadConfigs(); loadTasks(); loadMemory("project") }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true; _authError.value = null
            repo.login(email, pass).onSuccess {
                _isLoggedIn.value = true
                _isOwner.value = repo.tokenManager.getRole().lowercase().contains("owner")
                loadSkills(); loadConfigs()
                _statusBanner.value = "Logged in as ${repo.tokenManager.getRole()}"
                loadTasks(); loadMemory("project")
            }.onFailure { e -> 
                if (e.message == "UNVERIFIED_USER") { _authStep.value = "OTP"; _authError.value = "Please verify your email OTP." }
                else { _authError.value = e.message }
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout(); _isLoggedIn.value = false; _isOwner.value = false; _authStep.value = "LOGIN"
            _skills.value = emptyList(); _selectedSkill.value = SkillUi("ai_admin", "AI Admin"); currentConversationId = null 
        }
    }

    fun loadSkills() {
        viewModelScope.launch {
            repo.getSkills().onSuccess { dynamicSkills ->
                val mapped = dynamicSkills.map { SkillUi(it.key, it.name, it.description ?: "", it.ownerOnly) }
                if (mapped.isNotEmpty()) {
                    _skills.value = mapped
                    if (mapped.none { it.key == _selectedSkill.value.key }) _selectedSkill.value = mapped.first()
                }
            }.onFailure {
                _skills.value = listOf(SkillUi("ai_admin", "AI Admin"), SkillUi("n8n_wf_builder", "WF Builder", ownerOnly = true))
            }
        }
    }

    fun loadConfigs() {
        viewModelScope.launch {
            repo.getAiModels().onSuccess { _aiModels.value = it }
            repo.getPrompts().onSuccess { _prompts.value = it }
        }
    }

    fun toggleModel(id: String) { viewModelScope.launch { repo.toggleAiModel(id).onSuccess { loadConfigs() } } }
    fun updatePrompt(skillKey: String, text: String) { viewModelScope.launch { repo.updatePrompt(skillKey, text).onSuccess { loadConfigs(); _statusBanner.value = "Prompt Updated" } } }
    fun saveByoi(ep: String, key: String, model: String) { viewModelScope.launch { repo.saveByoi(ep, key, model).onSuccess { _statusBanner.value = "BYOI Saved" }.onFailure { _statusBanner.value = "BYOI Error: ${it.message}" } } }
    
    fun loadTasks() { viewModelScope.launch { repo.fetchTasks().onSuccess { _tasks.value = it } } }
    fun loadMemory(scope: String) { viewModelScope.launch { repo.fetchMemory(scope, _selectedProject.value.id).onSuccess { _memoryItems.value = it } } }
    fun clearBanner() { _statusBanner.value = null }
    fun selectProject(p: ProjectUi) { _selectedProject.value = p }
    fun selectSkill(s: SkillUi) { _selectedSkill.value = s }
    fun toggleCeoMode(enabled: Boolean) { _ceoMode.value = enabled }
    fun toggleFastPath(enabled: Boolean) { _useFastPath.value = enabled }
    fun startNewChat() { currentConversationId = null; _messages.value = listOf(ChatMessageUi("welcome", "assistant", "Nayi chat shuru!", "Now", "verified", "system")) }
    fun clearBuilder() { _builderOutput.value = ""; _lastGeneratedWfJson.value = null }

    private suspend fun simulateTypewriter(fullText: String, provider: String, verification: String) {
        val tempId = UUID.randomUUID().toString()
        var currentText = ""
        _messages.value = _messages.value + ChatMessageUi(tempId, "assistant", "", "typing", "typing", provider)
        for (char in fullText) {
            currentText += char
            _messages.value = _messages.value.map { if (it.id == tempId) it.copy(content = currentText) else it }
            delay(8)
        }
        _messages.value = _messages.value.map { if (it.id == tempId) it.copy(verificationStatus = verification, timestamp = "Just now") else it }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "user", userText, "Now")
        viewModelScope.launch {
            _isLoading.value = true
            val pid = _selectedProject.value.id; val sk = _selectedSkill.value.key; val ceo = _ceoMode.value
            if (_useFastPath.value) {
                repo.sendFastChat(userText, pid, currentConversationId, sk, ceo).onSuccess { (msg, convId) ->
                    if (convId != null) currentConversationId = convId                    
                    _isLoading.value = false
                    simulateTypewriter(msg.content, msg.provider, msg.verificationStatus)
                }.onFailure { _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "assistant", "Error: ${it.message}", "Now", "failed", "system", isError = true); _isLoading.value = false }
            } else {
                repo.sendGatewayChat(userText, pid, currentConversationId).onSuccess { _isLoading.value = false; simulateTypewriter(it.content, it.provider, it.verificationStatus) }.onFailure { _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "assistant", "Error: ${it.message}", "Now", "failed", "system", isError = true); _isLoading.value = false }
            }
        }
    }

    fun buildWorkflow(requirements: String) {
        if (requirements.isBlank()) return
        _isBuilding.value = true; _builderOutput.value = "🚀 Starting Smart Hybrid Cycle...\n"; _lastGeneratedWfJson.value = null
        val pid = _selectedProject.value.id
        viewModelScope.launch {
            try {
                _builderOutput.value += "1. Generating Workflow via WF-02...\n"
                val genRes = repo.executeGatewayIntent("GENERATE_WORKFLOW", JsonObject().apply { addProperty("requirements", requirements); addProperty("target_n8n_version", "1.80.0") }, pid)
                if (genRes.isFailure) throw Exception("Generation failed: ${genRes.exceptionOrNull()?.message}")
                val childObj = genRes.getOrNull()?.result?.get("child_result")
                if (childObj == null || !childObj.isJsonObject) throw Exception("Invalid response from backend.")
                val currentWfJson = childObj.asJsonObject.get("generated_workflow_json")
                if (currentWfJson == null || !currentWfJson.isJsonObject) throw Exception("WF-02 failed.")
                
                var wfJsonObj = currentWfJson.asJsonObject
                _builderOutput.value += "✅ Workflow Generated!\n\n"
                var attempt = 0; var passed = false; var lastError = "Unknown Error"
                while (attempt < 5) {
                    attempt++
                    _builderOutput.value += "🔍 Attempt #${attempt} | Static Check (WF-04)...\n"
                    val valRes = repo.executeGatewayIntent("VALIDATE_WORKFLOW", JsonObject().apply { add("workflow_json", wfJsonObj); addProperty("target_n8n_version", "1.80.0") }, pid)
                    val valChild = valRes.getOrNull()?.result?.get("child_result")
                    val valStatus = if(valChild != null && valChild.isJsonObject) valChild.asJsonObject.get("validation_status")?.asString else "failed"
                    val failedTests = if(valChild != null && valChild.isJsonObject) valChild.asJsonObject.getAsJsonArray("failed_tests") else null
                    
                    val staticPassed = (valStatus == "passed") || (valStatus == "partial" && (failedTests == null || failedTests.size() == 0))
                    if (staticPassed) {
                        _builderOutput.value += "✅ Static Check Passed. Moving to Real Sandbox...\n"
                        val testRes = repo.testWorkflow(wfJsonObj)
                        if (testRes.isSuccess) { passed = true; _builderOutput.value += "✅ Real Sandbox Test Passed!\n\n"; break }
                        else { lastError = testRes.exceptionOrNull()?.message ?: "Sandbox Error"; _builderOutput.value += "❌ Sandbox Failed: $lastError\n" }
                    } else { lastError = failedTests?.toString() ?: "Static Validation Failed"; _builderOutput.value += "❌ Static Check Failed.\n" }
                    
                    if (attempt >= 5) { _builderOutput.value += "❌ Max 5 attempts reached. Stopping Loop.\n"; break }
                    
                    _builderOutput.value += "🧠 Analyzing Error via WF-01...\n"
                    val debugRes = repo.executeGatewayIntent("ANALYZE_WORKFLOW", JsonObject().apply { add("workflow_json", wfJsonObj); addProperty("analysis_mode", "debug_error"); addProperty("error_logs", lastError) }, pid)
                    val debugChild = debugRes.getOrNull()?.result?.get("child_result")
                    var analysisResult = "Fix the validation errors."
                    if (debugChild != null && debugChild.isJsonObject) {
                        val analysis = debugChild.asJsonObject.get("workflow_summary")
                        if (analysis != null && !analysis.isJsonNull) analysisResult = analysis.asString
                    }
                    _builderOutput.value += "⚠️ Auto-repairing via WF-03...\n"
                    val updRes = repo.executeGatewayIntent("UPDATE_WORKFLOW", JsonObject().apply { add("original_workflow_json", wfJsonObj); addProperty("requested_changes", "Fix errors based on this analysis: $analysisResult") }, pid)
                    val updChild = updRes.getOrNull()?.result?.get("child_result")
                    if (updChild != null && updChild.isJsonObject) {
                        val updatedWf = updChild.asJsonObject.get("updated_workflow_json")
                        if (updatedWf != null && updatedWf.isJsonObject) wfJsonObj = updatedWf.asJsonObject
                        else { _builderOutput.value += "❌ Auto-repair failed to return JSON.\n"; break }
                    } else { _builderOutput.value += "❌ Auto-repair failed.\n"; break }
                }
                if (passed) {
                    _lastGeneratedWfJson.value = wfJsonObj
                    _builderOutput.value += "\n--- FINAL PRODUCTION WORKFLOW JSON ---\n" + wfJsonObj.toString() + "\n\n🧠 Saving workflow to Memory...\n"
                    repo.saveWorkflowMemory(wfJsonObj.toString(), pid).onSuccess { _builderOutput.value += " ✅ Saved!"; loadMemory("workflow") }.onFailure { _builderOutput.value += " ❌ Failed: ${it.message}" }
                } else {
                    _builderOutput.value += "\n\n🚨 HUMAN CONFIRMATION REQUIRED 🚨\nSystem failed after 5 attempts.\nLast Error: $lastError\n"
                }
            } catch (e: Exception) { _builderOutput.value += "\n❌ SYSTEM ERROR: ${e.message}" } finally { _isBuilding.value = false }
        }
    }

    fun deployCurrentWorkflow() {
        val wfJson = _lastGeneratedWfJson.value ?: return
        _isDeploying.value = true; _builderOutput.value += "\n\n🚀 Deploying to Live n8n...\n"
        viewModelScope.launch {
            repo.deployWorkflow(wfJson).onSuccess { msg ->
                _builderOutput.value += "✅ $msg\n✅ Deployment Complete. This workflow is now LIVE!\n"
                _statusBanner.value = "Workflow Deployed Successfully!"
                _lastGeneratedWfJson.value = null
            }.onFailure { err -> _builderOutput.value += "❌ Deploy Failed: ${err.message}" }
            _isDeploying.value = false
        }
    }
}
