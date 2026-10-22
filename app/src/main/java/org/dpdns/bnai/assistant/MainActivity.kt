package org.dpdns.bnai.assistant
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.assistant.ui.*
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = MainViewModel()
        setContent {
            BnaiTheme {
                MainAppScaffold(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var currentTab by remember { mutableStateOf(0) }
    
    if (!isLoggedIn) { 
        AuthScreen(viewModel) 
        return 
    }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("BNAI Assistant v3.0") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Text("C") }, label = { Text("Chat") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Text("B") }, label = { Text("Builder") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                0 -> ChatAndSkillsScreen(viewModel)
                1 -> WorkflowBuilderScreen(viewModel)
                else -> Text("Coming Soon")
            }
        }
    }
}
