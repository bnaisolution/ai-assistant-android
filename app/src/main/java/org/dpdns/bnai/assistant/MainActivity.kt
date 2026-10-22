package org.dpdns.bnai.assistant
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.assistant.data.*
import org.dpdns.bnai.assistant.ui.*
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(applicationContext)
        val repository = AppRepository(ApiService.create(tokenManager), tokenManager)
        val viewModel = MainViewModel(repository)
        setContent { BnaiTheme { MainAppScaffold(viewModel) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    var currentTab by remember { mutableStateOf(0) }
    
    if (!isLoggedIn) { AuthScreen(viewModel); return }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("BNAI Assistant v3.0") }, actions = { TextButton(onClick = { viewModel.logout() }) { Text("Logout", color = MaterialTheme.colorScheme.error) } }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Text("C") }, label = { Text("Chat") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Text("B") }, label = { Text("Builder") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            statusBanner?.let { Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)) { Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) { Text(it, modifier = Modifier.weight(1f)); TextButton(onClick = { viewModel.clearBanner() }) { Text("OK") } } } }
            when (currentTab) { 
                0 -> Text("Chat Screen - Phase 2 Pending") 
                1 -> Text("Builder Screen - Phase 3 Pending") 
                else -> Text("Soon") 
            }
        }
    }
}
