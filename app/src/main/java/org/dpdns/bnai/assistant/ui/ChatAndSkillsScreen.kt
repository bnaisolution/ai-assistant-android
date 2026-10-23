package org.dpdns.bnai.assistant.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        
        // CLEAN TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
            Text("AI Admin", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            Row {
                // Floating Bubble Launcher
                IconButton(onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    } else {
                        context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
                    }
                }) { Icon(Icons.Default.Star, contentDescription = "Floating Bubble") }
                
                IconButton(onClick = { viewModel.startNewChat() }) { Icon(Icons.Default.Add, contentDescription = "New Chat") }
            }
        }

        // MESSAGES (ChatGPT Style)
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                    if (isUser) {
                        // User Bubble
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(max = 300.dp)) {
                            Text(text = msg.content, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        // AI Text (Clean, no bubble)
                        Row(verticalAlignment = Alignment.Top) {
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("AI", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary) }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = msg.content, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                                Row {
                                    IconButton(onClick = { viewModel.sendFeedback(msg.id, "thumbs_up") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(14.dp)) }
                                    IconButton(onClick = { viewModel.sendFeedback(msg.id, "thumbs_down") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ThumbDown, null, modifier = Modifier.size(14.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CLEAN INPUT BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Attach File logic */ }) { Icon(Icons.Default.Add, "Attach") }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask anything...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { 
                    val q = inputText; inputText = ""
                    viewModel.streamChatMessage(q) 
                },
                enabled = !isLoading && inputText.isNotBlank(),
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            ) { Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}
