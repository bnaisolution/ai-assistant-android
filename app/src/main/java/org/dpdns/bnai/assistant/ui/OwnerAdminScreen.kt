package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.data.AIModelConfig
import org.dpdns.bnai.assistant.data.PromptConfig
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun OwnerAdminScreen(viewModel: MainViewModel) {
    val aiModels by viewModel.aiModels.collectAsState()
    val prompts by viewModel.prompts.collectAsState()
    
    var expandedSection by remember { mutableStateOf("") } // ROUTER, PROMPT, BYOI
    var byoiTab by remember { mutableStateOf("CHAT_API") } // CHAT_API, DB, VPS, GPU

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Owner Admin Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        
        // --- ACCORDION 1: DYNAMIC AI ROUTER ---
        AccordionCard(
            title = "Dynamic AI Router",
            subtitle = "Set Primary & Fallback Models",
            isExpanded = expandedSection == "ROUTER",
            onToggle = { expandedSection = if (expandedSection == "ROUTER") "" else "ROUTER" }
        ) {
            AIRouterUI(aiModels, viewModel)
        }

        // --- ACCORDION 2: SYSTEM PROMPT ---
        AccordionCard(
            title = "System Prompt Versioning",
            subtitle = "Manage AI Instructions per Skill",
            isExpanded = expandedSection == "PROMPT",
            onToggle = { expandedSection = if (expandedSection == "PROMPT") "" else "PROMPT" }
        ) {
            PromptUI(prompts, viewModel)
        }

        // --- ACCORDION 3: BYOI ---
        AccordionCard(
            title = "BYOI Integrations",
            subtitle = "Custom APIs, Databases & GPUs",
            isExpanded = expandedSection == "BYOI",
            onToggle = { expandedSection = if (expandedSection == "BYOI") "" else "BYOI" }
        ) {
            BYOIUI(byoiTab, onTabChange = { byoiTab = it }, viewModel = viewModel)
        }
    }
}

@Composable
fun AccordionCard(title: String, subtitle: String, isExpanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand"
                )
            }
            if (isExpanded) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Column(modifier = Modifier.padding(16.dp)) { content() }
            }
        }
    }
}

@Composable
fun AIRouterUI(aiModels: List<AIModelConfig>, viewModel: MainViewModel) {
    val primaryModel = aiModels.find { it.isActive && it.priority == 1 } ?: aiModels.firstOrNull { it.isActive }
    val fallbackModel = aiModels.find { it.isActive && it.priority == 2 }

    var primaryExpanded by remember { mutableStateOf(false) }
    var fallbackExpanded by remember { mutableStateOf(false) }

    Text("Primary Model", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { primaryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(primaryModel?.let { "${it.provider.uppercase()} - ${it.modelName}" } ?: "Select Primary")
        }
        DropdownMenu(expanded = primaryExpanded, onDismissRequest = { primaryExpanded = false }) {
            aiModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text("${model.provider.uppercase()} - ${model.modelName}") },
                    onClick = { viewModel.setPrimaryModel(model.id); primaryExpanded = false }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text("Fallback Model", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { fallbackExpanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(fallbackModel?.let { "${it.provider.uppercase()} - ${it.modelName}" } ?: "None (Optional)")
        }
        DropdownMenu(expanded = fallbackExpanded, onDismissRequest = { fallbackExpanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { viewModel.setFallbackModel(""); fallbackExpanded = false })
            aiModels.forEach { model ->
                if (model.id != primaryModel?.id) {
                    DropdownMenuItem(
                        text = { Text("${model.provider.uppercase()} - ${model.modelName}") },
                        onClick = { viewModel.setFallbackModel(model.id); fallbackExpanded = false }
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    var showAddProvider by remember { mutableStateOf(false) }
    Button(onClick = { showAddProvider = !showAddProvider }, modifier = Modifier.fillMaxWidth()) {
        Text(if(showAddProvider) "Close Add Provider" else "+ Add New AI Provider")
    }
    
    if (showAddProvider) {
        var pName by remember { mutableStateOf("") }
        var mName by remember { mutableStateOf("") }
        var apiKey by remember { mutableStateOf("") }
        var baseUrl by remember { mutableStateOf("") }
        
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Provider Name (e.g., mistral)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mName, onValueChange = { mName = it }, label = { Text("Model Name (e.g., mistral-large)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL (Optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
        
        Button(onClick = { 
            viewModel.addNewProvider(pName, mName, baseUrl, apiKey)
            showAddProvider = false 
        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Save Provider")
        }
    }
}

@Composable
fun PromptUI(prompts: List<PromptConfig>, viewModel: MainViewModel) {
    var selectedSkill by remember { mutableStateOf("ai_admin") }
    var promptText by remember { mutableStateOf("") }
    var skillExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(prompts, selectedSkill) {
        prompts.find { it.name == selectedSkill }?.let { promptText = it.content }
    }

    Text("Select Skill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { skillExpanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedSkill)
        }
        DropdownMenu(expanded = skillExpanded, onDismissRequest = { skillExpanded = false }) {
            DropdownMenuItem(text = { Text("ai_admin") }, onClick = { selectedSkill = "ai_admin"; skillExpanded = false })
            DropdownMenuItem(text = { Text("n8n_wf_builder") }, onClick = { selectedSkill = "n8n_wf_builder"; skillExpanded = false })
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = promptText, onValueChange = { promptText = it },
        modifier = Modifier.fillMaxWidth().height(120.dp), label = { Text("Prompt Content") }
    )
    Button(onClick = { viewModel.updatePrompt(selectedSkill, promptText) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Update Prompt")
    }
}

@Composable
fun BYOIUI(selectedTab: String, onTabChange: (String) -> Unit, viewModel: MainViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("CHAT_API" to "Chat AI", "DB" to "Database", "VPS" to "VPS", "GPU" to "GPU").forEach { (key, label) ->
            FilterChip(
                selected = selectedTab == key,
                onClick = { onTabChange(key) },
                label = { Text(label, fontSize = 11.sp) }
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    
    when (selectedTab) {
        "CHAT_API" -> BYOIForm("Custom Chat API", "Endpoint URL", "API Key", "custom_llm", viewModel)
        "DB" -> BYOIForm("Database Connection", "DB URL (postgres://...)", "Password", "custom_db", viewModel)
        "VPS" -> BYOIForm("VPS Server", "IP Address / SSH", "Password", "custom_vps", viewModel)
        "GPU" -> BYOIForm("GPU Server", "IP Address / Port", "Access Token", "custom_gpu", viewModel)
    }
}

@Composable
fun BYOIForm(title: String, field1: String, field2: String, integrationType: String, viewModel: MainViewModel) {
    var f1 by remember { mutableStateOf("") }
    var f2 by remember { mutableStateOf("") }
    var f3 by remember { mutableStateOf("") }

    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(value = f1, onValueChange = { f1 = it }, label = { Text(field1) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = f2, onValueChange = { f2 = it }, label = { Text(field2) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = f3, onValueChange = { f3 = it }, label = { Text("Model / Additional Info") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { viewModel.saveByoi(integrationType, f1, f2, f3) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("Save Configuration")
    }
}
