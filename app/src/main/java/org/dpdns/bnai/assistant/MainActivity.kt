package org.dpdns.bnai.assistant
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.assistant.data.ApiService
import org.dpdns.bnai.assistant.data.AppRepository
import org.dpdns.bnai.assistant.data.TokenManager
import org.dpdns.bnai.assistant.data.Incident
import org.dpdns.bnai.assistant.crash.CrashHandler
import org.dpdns.bnai.assistant.ui.*
import org.dpdns.bnai.assistant.viewmodel.MainViewModel
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))
        val tokenManager = TokenManager(applicationContext)
        val apiService = ApiService.create(tokenManager)
        val repository = AppRepository(apiService, tokenManager)
        val mainViewModel = MainViewModel(repository)
        setContent {
            BnaiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainAppScaffold(viewModel = mainViewModel, apiService = apiService)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel, apiService: ApiService) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    var currentTab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val history by viewModel.history.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val selectedSkill by viewModel.selectedSkill.collectAsState()
    val useFastPath by viewModel.useFastPath.collectAsState()
    val ceoMode by viewModel.ceoMode.collectAsState()
    LaunchedEffect(isLoggedIn) { if (!isLoggedIn) currentTab = 0 }
    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
        return
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Neeraj (Owner)", fontWeight = FontWeight.Bold)
                        Text("admin@example.com", fontSize = 12.sp)
                    }
                }
                TextButton(onClick = { viewModel.logout() }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.ExitToApp, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
                Divider()
                TextButton(onClick = { viewModel.startNewChat(); currentTab = 0; scope.launch { drawerState.close() } }, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Chat", fontWeight = FontWeight.Bold)
                }
                Divider()
                Text("History", modifier = Modifier.padding(16.dp, 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(history) { conv ->
                        ListItem(
                            headlineContent = { Text(conv.title, maxLines = 1) },
                            modifier = Modifier.clickable { viewModel.loadConversation(conv.id); currentTab = 0; scope.launch { drawerState.close() } }
                        )
                    }
                }
                Divider()
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Settings", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = useFastPath, onCheckedChange = { viewModel.toggleFastPath(it) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fast AI Mode", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ceoMode, onCheckedChange = { viewModel.toggleCeoMode(it) })
                        Text("CEO Mode", fontSize = 12.sp)
                    }
                    Text("Active Skill: \${selectedSkill.name}", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") })
                    if (isOwner) NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("Builder") })
                    NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Tasks") })
                    NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.Storage, null) }, label = { Text("Memory") })
                    if (isOwner) NavigationBarItem(selected = currentTab == 4, onClick = { currentTab = 4 }, icon = { Icon(Icons.Default.Security, null) }, label = { Text("Owner") })
                    NavigationBarItem(selected = currentTab == 5, onClick = { currentTab = 5 }, icon = { Icon(Icons.Default.Warning, null) }, label = { Text("Incidents") })
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentTab) {
                    0 -> ChatAndSkillsScreen(viewModel, onOpenDrawer = { scope.launch { drawerState.open() } })
                    1 -> if (isOwner) WorkflowBuilderScreen(viewModel) else ChatAndSkillsScreen(viewModel, onOpenDrawer = { scope.launch { drawerState.open() } })
                    2 -> TasksAndApprovalsScreen(viewModel)
                    3 -> MemoryAndPrivacyScreen(viewModel)
                    4 -> if (isOwner) OwnerAdminScreen(viewModel) else ChatAndSkillsScreen(viewModel, onOpenDrawer = { scope.launch { drawerState.open() } })
                    5 -> IncidentCenterWrapper(apiService)
                }
            }
        }
    }
}
@Composable
fun IncidentCenterWrapper(apiService: ApiService) {
    var incidents by remember { mutableStateOf<List<Incident>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        try {
            val response = apiService.getIncidents()
            if (response.isSuccessful) {
                incidents = response.body() ?: emptyList()
            }
        } catch (e: Exception) {}
        isLoading = false
    }
    IncidentCenterScreen(
        incidents = incidents,
        onAcknowledge = { id ->
            scope.launch {
                try {
                    apiService.acknowledgeIncident(id)
                    incidents = incidents.filter { it.id != id }
                } catch (e: Exception) {}
            }
        },
        isLoading = isLoading
    )
}
