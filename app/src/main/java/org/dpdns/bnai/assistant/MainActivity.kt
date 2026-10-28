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
import androidx.compose.ui.text.font.FontWeight
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
    val selectedProject by viewModel.selectedProject.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    var activeScreen by remember { mutableStateOf("HOME") }

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BNAI Unified v3.1", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Project: ${selectedProject.namespace} • Screen: $activeScreen", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.logout() }) { Text("Logout", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = activeScreen == "HOME", onClick = { activeScreen = "HOME" }, icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = activeScreen == "CHAT", onClick = { activeScreen = "CHAT" }, icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") }, label = { Text("Chat") })
                NavigationBarItem(selected = activeScreen in listOf("PROJECTS", "CINEMATIC", "WF_BUILDER"), onClick = { activeScreen = "PROJECTS" }, icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") }, label = { Text("Projects") })
                NavigationBarItem(selected = activeScreen == "TASKS", onClick = { activeScreen = "TASKS" }, icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Tasks") }, label = { Text("Tasks") })
                NavigationBarItem(selected = activeScreen in listOf("MORE", "COPILOT", "MEMORY", "FILES_NOTIFS", "OWNER"), onClick = { activeScreen = "MORE" }, icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") }, label = { Text("More") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            statusBanner?.let { banner ->
                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(banner, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearBanner() }) { Text("OK", fontSize = 11.sp) }
                    }
                }
            }

            when (activeScreen) {
                "HOME" -> HomeDashboardScreen(viewModel = viewModel, onNavigateToSection = { dest -> activeScreen = dest })
                "CHAT" -> ChatAndSkillsScreen(viewModel = viewModel)
                "PROJECTS" -> ProjectsHubScreen(viewModel = viewModel, onNavigate = { dest -> activeScreen = dest })
                "CINEMATIC" -> CinematicStudioScreen(viewModel = viewModel, onOpenCopilotWithContext = { activeScreen = "COPILOT" })
                "WF_BUILDER" -> WorkflowBuilderScreen(viewModel = viewModel)
                "TASKS" -> TasksAndApprovalsScreen(viewModel = viewModel)
                "COPILOT" -> CopilotAndDiagnosticsScreen(viewModel = viewModel)
                "MEMORY" -> MemoryAndPrivacyScreen(viewModel = viewModel)
                "FILES_NOTIFS" -> FilesAndNotificationsScreen(viewModel = viewModel)
                "OWNER" -> OwnerAdminScreen(viewModel = viewModel)
                "MORE" -> MoreMenuScreen(onSelectDestination = { dest -> activeScreen = dest })
            }
        }
    }
}

@Composable
fun ProjectsHubScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Two Logical Projects Hub", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        viewModel.projects.forEach { proj ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${proj.name} [${proj.namespace}]", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(proj.description, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.selectProject(proj) }) { Text(if (selectedProject.id == proj.id) "Active Context" else "Switch Context") }
                        if (proj.namespace == "Assistant") {
                            OutlinedButton(onClick = { onNavigate("WF_BUILDER") }) { Text("Open WF Builder") }
                        } else {
                            OutlinedButton(onClick = { onNavigate("CINEMATIC") }) { Text("Open Cinematic Studio") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreMenuScreen(onSelectDestination: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("More Capabilities", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Button(onClick = { onSelectDestination("COPILOT") }, modifier = Modifier.fillMaxWidth()) { Text("AI Copilot, Inspector, Diff, Sandbox & Recovery") }
        Button(onClick = { onSelectDestination("CINEMATIC") }, modifier = Modifier.fillMaxWidth()) { Text("Project B — AI Cinematic Studio") }
        Button(onClick = { onSelectDestination("WF_BUILDER") }, modifier = Modifier.fillMaxWidth()) { Text("Project A — n8n Workflow Builder") }
        Button(onClick = { onSelectDestination("MEMORY") }, modifier = Modifier.fillMaxWidth()) { Text("7-Layer Scoped Memory & Data Privacy") }
        Button(onClick = { onSelectDestination("FILES_NOTIFS") }, modifier = Modifier.fillMaxWidth()) { Text("Files, Exports, Notifications & Audit") }
        Button(onClick = { onSelectDestination("OWNER") }, modifier = Modifier.fillMaxWidth()) { Text("Owner Admin Governance Console") }
    }
}
