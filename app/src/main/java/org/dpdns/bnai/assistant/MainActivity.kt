package org.dpdns.bnai.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") }, label = { Text("Chat", fontSize = 10.sp) })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Build, contentDescription = "Builder") }, label = { Text("Builder", fontSize = 10.sp) })
                NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Tasks") }, label = { Text("Tasks", fontSize = 10.sp) })
                NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.Storage, contentDescription = "Memory") }, label = { Text("Memory", fontSize = 10.sp) })
                NavigationBarItem(selected = currentTab == 4, onClick = { currentTab = 4 }, icon = { Icon(Icons.Default.Security, contentDescription = "Owner") }, label = { Text("Owner", fontSize = 10.sp) })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            statusBanner?.let { 
                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)) { 
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) { 
                        Text(it, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearBanner() }) { Text("OK") } 
                    } 
                } 
            }
            when (currentTab) { 
                0 -> ChatAndSkillsScreen(viewModel)
                1 -> WorkflowBuilderScreen(viewModel)
                2 -> TasksAndApprovalsScreen(viewModel)
                3 -> MemoryAndPrivacyScreen(viewModel)
                4 -> OwnerAdminScreen(viewModel)
                else -> Text("Coming Soon")
            }
        }
    }
}
