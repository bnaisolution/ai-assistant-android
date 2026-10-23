package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun OwnerAdminScreen(viewModel: MainViewModel) {
    val aiModels by viewModel.aiModels.collectAsState()
    val prompts by viewModel.prompts.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    
    var promptText by remember { mutableStateOf("") }
    var byoiEndpoint by remember { mutableStateOf("") }
    var byoiKey by remember { mutableStateOf("") }
    var byoiModel by remember { mutableStateOf("") }

    LaunchedEffect(prompts) {
        prompts.find { it.name == "ai_admin" }?.let { promptText = it.content }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Owner Admin Dashboard (M2 Config)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        
        statusBanner?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Dynamic AI Router", fontWeight = FontWeight.Bold)
                if (aiModels.isEmpty()) {
                    Text("No models configured", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                aiModels.forEach { model ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${model.provider.uppercase()} - ${model.modelName}")
                        Switch(checked = model.isActive, onCheckedChange = { viewModel.toggleModel(model.id) })
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("System Prompt (Versioning)", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = promptText, onValueChange = { promptText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp), label = { Text("System Prompt for AI Admin") }
                )
                Button(onClick = { viewModel.updatePrompt("ai_admin", promptText) }, modifier = Modifier.padding(top = 8.dp)) { Text("Update Prompt") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("BYOI (Bring Your Own AI)", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = byoiEndpoint, onValueChange = { byoiEndpoint = it }, label = { Text("Custom API Endpoint (URL)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = byoiKey, onValueChange = { byoiKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = byoiModel, onValueChange = { byoiModel = it }, label = { Text("Model Name") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.saveByoi(byoiEndpoint, byoiKey, byoiModel) }, modifier = Modifier.padding(top = 8.dp)) { Text("Save Custom AI") }
            }
        }
    }
}
