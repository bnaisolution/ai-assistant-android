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
    private val _messages = MutableStateFlow(listOf(ChatMessageUi("welcome", "assistant", "Namaste! Main aapka BNAI Assistant hun.", "Ready", "verified", "system")))
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
}
