package org.dpdns.bnai.assistant.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // Context States (v1.6 Part 3)
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

    private val _messages = MutableStateFlow(listOf(ChatMessageUi("welcome", "assistant", "Namaste! Main aapka BNAI Assistant (v3.0) hun. Aaj kis project par kaam karein?", "Ready", "verified", "system")))
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    init { viewModelScope.launch { _backendHealthy.value = repo.checkBackendHealth() } }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.login(email, pass).onSuccess { _isLoggedIn.value = true; _statusBanner.value = "Logged in as ${repo.tokenManager.getRole()}" }.onFailure { _statusBanner.value = "Error: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun logout() { repo.tokenManager.clearSession(); _isLoggedIn.value = false }
    fun clearBanner() { _statusBanner.value = null }
    fun selectProject(p: ProjectUi) { _selectedProject.value = p }
    fun selectSkill(s: SkillUi) { _selectedSkill.value = s }
    fun toggleCeoMode(enabled: Boolean) { _ceoMode.value = enabled }
    fun toggleFastPath(enabled: Boolean) { _useFastPath.value = enabled }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "user", userText, "Now")
        
        viewModelScope.launch {
            _isLoading.value = true
            val pid = _selectedProject.value.id
            val sk = _selectedSkill.value.key
            val ceo = _ceoMode.value

            val result = if (_useFastPath.value) {
                repo.sendFastChat(userText, pid, null, sk, ceo)
            } else {
                repo.sendGatewayChat(userText, pid, null)
            }

            result.onSuccess { 
                _messages.value = _messages.value + it 
            }.onFailure { 
                _messages.value = _messages.value + ChatMessageUi(UUID.randomUUID().toString(), "assistant", "Error: ${it.message}", "Now", isError = true) 
            }
            _isLoading.value = false
        }
    }
}
