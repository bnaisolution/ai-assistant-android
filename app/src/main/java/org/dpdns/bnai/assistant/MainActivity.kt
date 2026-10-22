package org.dpdns.bnai.assistant
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.data.ApiService
import org.dpdns.bnai.assistant.data.AppRepository
import org.dpdns.bnai.assistant.data.TokenManager
import org.dpdns.bnai.assistant.ui.*
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(applicationContext)
        val apiService = ApiService.create(tokenManager)
        val repository = AppRepository(apiService, tokenManager)
        val mainViewModel = MainViewModel(repository)
        setContent {
            BnaiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainAppScaffold(viewModel = mainViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var currentTab by remember { mutableIntStateOf(0) }
    if (!isLoggedIn) { AuthScreen(viewModel = viewModel); return }
    Scaffold(
        topBar = { TopAppBar(title = { Text("BNAI Assistant v3.0", fontSize = 18.sp) }, actions = { TextButton(onClick = { viewModel.logout() }) { Text("Logout", color = MaterialTheme.colorScheme.error) } }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("Builder") })
                NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Tasks") })
                NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.Storage, null) }, label = { Text("Memory") })
                NavigationBarItem(selected = currentTab == 4, onClick = { currentTab = 4 }, icon = { Icon(Icons.Default.Security, null) }, label = { Text("Owner") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
                0 -> ChatAndSkillsScreen(viewModel)
                1 -> WorkflowBuilderScreen(viewModel)
                2 -> TasksAndApprovalsScreen(viewModel)
                3 -> MemoryAndPrivacyScreen(viewModel)
                4 -> OwnerAdminScreen(viewModel)
            }
        }
    }
}
