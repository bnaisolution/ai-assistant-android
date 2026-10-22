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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _backendHealthy = MutableStateFlow(true)
    val backendHealthy: StateFlow<Boolean> = _backendHealthy.asStateFlow()

    // Chat States
    val projects = listOf(ProjectUi("11111111-1111-1111-1111-111111111111", "Default Project"))
    private val _selectedProject = MutableStateFlow(projects.first())
    val selectedProject: StateFlow<ProjectUi> = _selectedProject.asStateFlow()

    val skills = listOf(SkillUi("ai_admin", "AI Admin"), SkillUi("n8n_wf_builder", "WF Builder"))
    private val _selectedSkill = MutableStateFlow(skills.first())
    val selectedSkill: StateFlow<SkillUi> = _selectedSkill.asStateFlow()

    private val _ceoMode = MutableStateFlow(false)
    val ceoMode: StateFlow<Boolean> = _ceoMode.asStateFlow()

    private val _useFastPath = MutableStateFlow(true)
    val useFastPath: StateFlow<Boolean> = _useFastPath.asStateFlow()

    private val _messages = MutableStateFlow(listOf(ChatMessageUi("welcome", "assistant", "Namaste! Main aapka BNAI Assistant (v3.0) hun.", "Ready", "verified", "system")))
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()
    private var currentConversationId: String? = null

    // Builder States (Phase 3)
    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()
    private val _builderOutput = MutableStateFlow("")
    val builderOutput: StateFlow<String> = _builderOutput.asStateFlow()

    init { viewModelScope.launch { _backendHealthy.value = repo.checkBackendHealth() } }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.login(email, pass).onSuccess { _isLoggedIn.value = true; _statusBanner.value = "Logged in as ${repo.tokenManager.getRole()}" }.onFailure { _statusBanner.value = "Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun logout() { repo.tokenManager.clearSession(); _isLoggedIn.value = false; currentConversationId = null }
    fun clearBanner() { _statusBanner.value = null }
    fun selectProject(p: ProjectUi) { _selectedProject.value = p }
    fun selectSkill(s: SkillUi) { _selectedSkill.value = s }
    fun toggleCeoMode(enabled: Boolean) { _ceoMode.value = enabled }
    fun toggleFastPath(enabled: Boolean) { _useFastPath.value = enabled }

    fun startNewChat() {
        currentConversationId = null
        _messages.value = listOf(ChatMessageUi("welcome", "assistant", "Nayi chat shuru! Main aapki kya madad kar sakta hoon?", "Now", "verified", "system"))
    }

    private suspend fun simulateTypewriter(fullText: String, provider: String, verification: String) {
        val tempId = UUID.randomUUID().toString()
        var currentText = ""
        _messages.value = _messages.value + ChatMessageUi(tempId, "assistant", "", "typing", "typing", provider)
        for (char in fullText) {
            currentText += char
            _messages.value = _messages.value.map { if (it.id == tempId) it.copy(content = currentText) else it }
            delay(15)
        }
        _messages.value = _messages.value.map { if (it.id == tempId) it.copy(verificationStatus = verification, timestamp = "Just now") else it }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "user", userText, "Now")
        
        viewModelScope.launch {
            _isLoading.value = true
            val pid = _selectedProject.value.id
            val sk = _selectedSkill.value.key
            val ceo = _ceoMode.value

            if (_useFastPath.value) {
                repo.sendFastChat(userText, pid, currentConversationId, sk, ceo).onSuccess { (msg, convId) ->
                    if (convId != null) currentConversationId = convId
                    _isLoading.value = false
                    simulateTypewriter(msg.content, msg.provider, msg.verificationStatus)
                }.onFailure { 
                    _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "assistant", "Error: ${it.message}", "Now", "failed", "system", isError = true) 
                    _isLoading.value = false
                }
            } else {
                repo.sendGatewayChat(userText, pid, currentConversationId).onSuccess { 
                    _isLoading.value = false
                    simulateTypewriter(it.content, it.provider, it.verificationStatus)
                }.onFailure { 
                    _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "assistant", "Error: ${it.message}", "Now", "failed", "system", isError = true) 
                    _isLoading.value = false
                }
            }
        }
    }

    // --- PHASE 3: WF BUILDER LOGIC (Blueprint v1.6 Part 13) ---
    fun buildWorkflow(requirements: String) {
        if (requirements.isBlank()) return
        _isBuilding.value = true
        _builderOutput.value = "🚀 Starting Generation Cycle...\n"

        viewModelScope.launch {
            try {
                // Step 1: Generate (WF-02)
                _builderOutput.value += "1. Generating Workflow via WF-02...\n"
                val genPayload = JsonObject().apply {
                    addProperty("requirements", requirements)
                    addProperty("target_n8n_version", "1.80.0")
                }
                
                val genRes = repo.executeGatewayIntent("GENERATE_WORKFLOW", genPayload)
                if (genRes.isFailure) throw Exception("Generation failed: ${genRes.exceptionOrNull()?.message}")
                
                var currentWfJson = genRes.getOrNull()?.result?.getAsJsonObject("child_result")?.getAsJsonObject("generated_workflow_json")
                if (currentWfJson == null) throw Exception("No workflow JSON generated.")
                
                _builderOutput.value += "✅ Workflow Generated!\n\n"

                // Step 2: 5-Attempt Repair Loop (Validate -> Update)
                var attempt = 0
                var passed = false
                while (attempt < 5 && !passed) {
                    _builderOutput.value += "🔍 Validating (WF-04) | Attempt #${attempt + 1}...\n"
                    val valPayload = JsonObject().apply {
                        add("workflow_json", currentWfJson)
                        addProperty("target_n8n_version", "1.80.0")
                    }
                    
                    val valRes = repo.executeGatewayIntent("VALIDATE_WORKFLOW", valPayload)
                    val valChild = valRes.getOrNull()?.result?.getAsJsonObject("child_result")
                    val valStatus = valChild?.get("validation_status")?.asString ?: "failed"

                    if (valStatus == "passed") {
                        passed = true
                        _builderOutput.value += "✅ Validation Passed!\n\n"
                        break
                    }

                    attempt++
                    if (attempt >= 5) {
                        _builderOutput.value += "❌ Max 5 attempts reached. Validation failed.\n"
                        break
                    }

                    _builderOutput.value += "⚠️ Issues found. Auto-repairing via WF-03 (Attempt #$attempt)...\n"
                    val updPayload = JsonObject().apply {
                        add("original_workflow_json", currentWfJson)
                        addProperty("requested_changes", "Fix validation errors.")
                    }
                    val updRes = repo.executeGatewayIntent("UPDATE_WORKFLOW", updPayload)
                    val updatedWf = updRes.getOrNull()?.result?.getAsJsonObject("child_result")?.getAsJsonObject("updated_workflow_json")
                    
                    if (updatedWf != null) {
                        currentWfJson = updatedWf
                    } else {
                        _builderOutput.value += "❌ Auto-repair failed to return JSON.\n"
                        break
                    }
                }

                _builderOutput.value += "\n--- FINAL WORKFLOW JSON ---\n"
                _builderOutput.value += currentWfJson.toString()

            } catch (e: Exception) {
                _builderOutput.value += "\n❌ ERROR: ${e.message}"
            } finally {
                _isBuilding.value = false
            }
        }
    }
}
