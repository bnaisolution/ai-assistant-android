package org.dpdns.bnai.assistant.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun ChatAndSkillsScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val selectedSkill by viewModel.selectedSkill.collectAsState()
    val useFastPath by viewModel.useFastPath.collectAsState()
    val ceoMode by viewModel.ceoMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val skills by viewModel.skills.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSkillSheet by remember { mutableStateOf(false) }
    var attachedFileContext by remember { mutableStateOf<String?>(null) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            attachedFileContext = "Content of the attached file: This is a mock document about the user's query."
            attachedFileName = "document.pdf"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "History") }
                    OutlinedButton(onClick = { showSkillSheet = true }) { Text("Skill: ${selectedSkill.name}", fontSize = 11.sp) }
                    Row {
                        IconButton(onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } else {
                                context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
                            }
                        }) { Icon(Icons.Default.Star, contentDescription = "Bubble") }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = useFastPath, onCheckedChange = { viewModel.toggleFastPath(it) })
                        Text(if (useFastPath) "Fast AI" else "n8n", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = ceoMode, onCheckedChange = { viewModel.toggleCeoMode(it) })
                        Text("CEO Mode", fontSize = 11.sp)
                    }
                    TextButton(onClick = { viewModel.startNewChat() }) { Text("+ New", fontSize = 11.sp) }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                    Surface(shape = RoundedCornerShape(12.dp), color = if (isUser) MaterialTheme.colorScheme.primary else if (msg.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface, modifier = Modifier.widthIn(max = 320.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = msg.content, fontSize = 14.sp)
                            if (!isUser) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${msg.provider} • ${msg.verificationStatus}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.sendFeedback(msg.id, "thumbs_up") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(14.dp)) }
                                    IconButton(onClick = { viewModel.sendFeedback(msg.id, "thumbs_down") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ThumbDown, null, modifier = Modifier.size(14.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        attachedFileName?.let {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                Text(" $it", fontSize = 12.sp)
                IconButton(onClick = { attachedFileContext = null; attachedFileName = null }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { filePickerLauncher.launch("*/*") }) { Icon(Icons.Default.AttachFile, "Attach") }
            OutlinedTextField(value = inputText, onValueChange = { inputText = it }, placeholder = { Text("Message...") }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { 
                val q = inputText; val f = attachedFileContext
                inputText = ""; attachedFileContext = null; attachedFileName = null
                viewModel.streamChatMessage(q, f) 
            }, enabled = !isLoading && inputText.isNotBlank()) { Text(if (isLoading) "..." else "➤") }
        }
    }

    if (showSkillSheet) {
        SkillBottomSheet(skills = skills.filter { !it.ownerOnly || isOwner }, selectedSkill = selectedSkill, onSkillSelected = { sk -> viewModel.selectSkill(sk); showSkillSheet = false }, onDismiss = { showSkillSheet = false })
    }
}
