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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
    val isOwner by viewModel.isOwner.collectAsState()
    var currentTab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val history by viewModel.history.collectAsState()

    LaunchedEffect(isLoggedIn) { if (!isLoggedIn) currentTab = 0 }

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Chat History", modifier = Modifier.padding(16.dp), fontSize = 20.sp)
                Divider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(history) { conv ->
                        ListItem(
                            headlineText = { Text(conv.title) },
                            modifier = Modifier.clickable { 
                                viewModel.loadConversation(conv.id)
                                currentTab = 0 
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("BNAI Assistant v3.0", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "History")
                        }
                    },
                    actions = { TextButton(onClick = { viewModel.logout() }) { Text("Logout", color = MaterialTheme.colorScheme.error) } }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") })
                    if (isOwner) NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("WF Builder") })
                    NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Tasks") })
                    NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.Storage, null) }, label = { Text("Memory") })
                    if (isOwner) NavigationBarItem(selected = currentTab == 4, onClick = { currentTab = 4 }, icon = { Icon(Icons.Default.Security, null) }, label = { Text("Owner") })
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
                }
            }
        }
    }
}
