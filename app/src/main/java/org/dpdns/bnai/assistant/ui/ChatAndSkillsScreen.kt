package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // --- Top Controls ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // Project Selector
                    Box {
                        AssistChip(
                            onClick = { showProjectMenu = true },
                            label = { Text(selectedProject.name, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(expanded = showProjectMenu, onDismissRequest = { showProjectMenu = false }) {
                            viewModel.projects.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { viewModel.selectProject(p); showProjectMenu = false }) }
                        }
                    }
                    // Skill Selector
                    Box {
                        AssistChip(
                            onClick = { showSkillMenu = true },
                            label = { Text(selectedSkill.name, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(expanded = showSkillMenu, onDismissRequest = { showSkillMenu = false }) {
                            viewModel.skills.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { viewModel.selectSkill(s); showSkillMenu = false }) }
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = useFastPath, 
                            onCheckedChange = { viewModel.toggleFastPath(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (useFastPath) "Fast AI" else "n8n Gateway", 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Medium,
                            color = if(useFastPath) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    // New Chat Button
                    FilledTonalButton(
                        onClick = { viewModel.startNewChat() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 12.sp)
                    }
                }
            }
        }

        // --- Chat List ---
        LazyColumn(
            state = listState, 
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp), 
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // AI Avatar (Left)
                    if (!isUser) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                            Text("AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Message Bubble
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) MaterialTheme.colorScheme.primary 
                                else if (msg.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) 
                                else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = msg.content, fontSize = 14.sp, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface)
                            
                            if (!isUser && msg.verificationStatus != "typing") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${msg.provider} • ${msg.timestamp}", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // User Avatar (Right)
                    if (isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Text("U", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Thinking Indicator
            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                            Text("AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Thinking...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // --- Modern Input Bar ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Message...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send Icon Button
                FloatingActionButton(
                    onClick = { val q = inputText; inputText = ""; viewModel.sendChatMessage(q) },
                    containerColor = if (inputText.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (inputText.isNotBlank() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

// Helper for Switch scaling
fun Modifier.scale(scale: Float): Modifier = this.then(androidx.compose.ui.draw.scale(scale))
