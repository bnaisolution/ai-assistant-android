package org.dpdns.bnai.assistant.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.service.FloatingBubbleService
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun ChatAndSkillsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedSkill by viewModel.selectedSkill.collectAsState()
    val sessionMode by viewModel.sessionMode.collectAsState()
    val useFastPath by viewModel.useFastPath.collectAsState()
    val ceoMode by viewModel.ceoMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var skillDropdownExpanded by remember { mutableStateOf(false) }
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(onClick = { projectDropdownExpanded = true }) {
                            Text("Proj: ${selectedProject.namespace}", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = projectDropdownExpanded, onDismissRequest = { projectDropdownExpanded = false }) {
                            viewModel.projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text("${proj.namespace} — ${proj.name}") },
                                    onClick = { viewModel.selectProject(proj); projectDropdownExpanded = false }
                                )
                            }
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { modeDropdownExpanded = true }) {
                            Text("Mode: $sessionMode", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = modeDropdownExpanded, onDismissRequest = { modeDropdownExpanded = false }) {
                            viewModel.sessionModes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = { viewModel.setSessionMode(mode); modeDropdownExpanded = false }
                                )
                            }
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { skillDropdownExpanded = true }) {
                            Text("Skill: ${selectedSkill.name.take(10)}..", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = skillDropdownExpanded, onDismissRequest = { skillDropdownExpanded = false }) {
                            viewModel.skills.forEach { sk ->
                                DropdownMenuItem(
                                    text = { Text(sk.name + if (sk.ownerOnly) " [Owner]" else "") },
                                    onClick = { viewModel.selectSkill(sk); skillDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = useFastPath, onCheckedChange = { viewModel.toggleFastPath(it) })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (useFastPath) "Fast (Groq Direct)" else "n8n (WF-00)", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ceoMode, onCheckedChange = { viewModel.toggleCeoMode(it) })
                        Text("CEO Mode", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } else {
                                context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
                            }
                        }
                    ) {
                        Text("Bubble", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary
                                else if (msg.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 330.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = msg.content, fontSize = 14.sp)
                            if (!isUser) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Provider: ${msg.provider} | Verification: ${msg.verificationStatus} | Project: ${selectedProject.namespace}",
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it },
                placeholder = { Text("Ask Primary AI Brain (${selectedProject.namespace})...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { val q = inputText; inputText = ""; viewModel.sendChatMessage(q) },
                enabled = !isLoading && inputText.isNotBlank()
            ) {
                Text(if (isLoading) "..." else "Send")
            }
        }
    }
}
