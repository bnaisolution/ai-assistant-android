package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAndSkillsScreen(viewModel: MainViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val useFastPath by viewModel.useFastPath.collectAsState()
    val ceoMode by viewModel.ceoMode.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedSkill by viewModel.selectedSkill.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showProjectMenu by remember { mutableStateOf(false) }
    var showSkillMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Top Controls (Blueprint v1.6 Part 3)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box {
                        OutlinedButton(onClick = { showProjectMenu = true }) { Text("Project: ${selectedProject.name}", fontSize = 11.sp) }
                        DropdownMenu(expanded = showProjectMenu, onDismissRequest = { showProjectMenu = false }) {
                            viewModel.projects.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { viewModel.selectProject(p); showProjectMenu = false }) }
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { showSkillMenu = true }) { Text("Skill: ${selectedSkill.name}", fontSize = 11.sp) }
                        DropdownMenu(expanded = showSkillMenu, onDismissRequest = { showSkillMenu = false }) {
                            viewModel.skills.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { viewModel.selectSkill(s); showSkillMenu = false }) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = useFastPath, onCheckedChange = { viewModel.toggleFastPath(it) })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (useFastPath) "Fast Path (Groq)" else "n8n Gateway", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ceoMode, onCheckedChange = { viewModel.toggleCeoMode(it) })
                        Text("CEO Mode", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat List
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary else if (msg.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = msg.content, fontSize = 14.sp)
                            if (!isUser) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Provider: ${msg.provider} | Status: ${msg.verificationStatus}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        // Input Bar
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = inputText, onValueChange = { inputText = it }, placeholder = { Text("Type a message...") }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { val q = inputText; inputText = ""; viewModel.sendChatMessage(q) },
                enabled = !isLoading && inputText.isNotBlank()
            ) { Text(if (isLoading) "..." else "Send") }
        }
    }
}
