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
    val memories by viewModel.memoryItems.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val scopes = listOf("project", "conversation", "workflow", "skill", "user_global", "workspace")
    var selectedScope by remember { mutableStateOf(scopes.first()) }
    var memKey by remember { mutableStateOf("") }
    var memContent by remember { mutableStateOf("") }

    LaunchedEffect(selectedScope, selectedProject.id) { viewModel.syncScopedMemoryFromBackend(selectedScope) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "7-Layer Scoped Memory [${selectedProject.namespace}]", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { viewModel.syncScopedMemoryFromBackend(selectedScope) }) { Text("Sync Scope", fontSize = 11.sp) }
        }
        Text(text = "Isolation Rule: Memory from ${selectedProject.namespace} is never leaked to other projects.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            scopes.take(4).forEach { sc ->
                FilterChip(selected = selectedScope == sc, onClick = { selectedScope = sc }, label = { Text(sc, fontSize = 11.sp) })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Write Scoped Memory to Supabase (Secrets Auto-Redacted)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = memKey, onValueChange = { memKey = it }, label = { Text("Memory Key (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = memContent, onValueChange = { memContent = it }, label = { Text("Memory Content (Scope: $selectedScope)") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { viewModel.addLocalOrRemoteMemory(selectedScope, memKey, memContent); memKey = ""; memContent = "" },
                    enabled = memContent.isNotBlank()
                ) { Text("Save to Scope '$selectedScope'") }
            }
        }

        Text("Retained Memory Items & Deletion Control:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("[${item.scope.uppercase()}] ${item.key ?: "Entry"}", fontWeight = FontWeight.Bold)
                            Text(item.content, fontSize = 13.sp)
                            Text(text = "Status: ${item.knowledgeStatus} | Cat: ${item.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        TextButton(onClick = { viewModel.requestMemoryDeletion(item.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
