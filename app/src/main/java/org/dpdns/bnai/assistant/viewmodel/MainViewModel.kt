import androidx.lifecycle.ViewModel; class MainViewModel(val repo: Any) : ViewModel() { val isLoggedIn = kotlinx.coroutines.flow.MutableStateFlow(false) }
