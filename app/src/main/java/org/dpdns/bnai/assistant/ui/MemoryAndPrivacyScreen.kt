package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun MemoryAndPrivacyScreen(viewModel: MainViewModel) {
    val memoryItems by viewModel.memoryItems.collectAsState()
    var selectedScope by remember { mutableStateOf("project") }
    
    val scopes = listOf("project", "workflow", "conversation", "skill", "user_global", "workspace")

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Memory & Privacy", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            scopes.take(3).forEach { sc ->
                FilterChip(
                    selected = selectedScope == sc,
                    onClick = { 
                        selectedScope = sc 
                        viewModel.loadMemory(sc)
                    },
                    label = { Text(sc, fontSize = 11.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            scopes.drop(3).forEach { sc ->
                FilterChip(
                    selected = selectedScope == sc,
                    onClick = { 
                        selectedScope = sc 
                        viewModel.loadMemory(sc)
                    },
                    label = { Text(sc, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (memoryItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No memory items found for $selectedScope", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memoryItems, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.key ?: "Untitled Memory", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.content, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Category: ${item.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
