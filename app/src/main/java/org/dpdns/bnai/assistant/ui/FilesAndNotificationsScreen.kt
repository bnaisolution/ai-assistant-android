package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun FilesAndNotificationsScreen(viewModel: MainViewModel) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val output by viewModel.filesAndNotifsOutput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var fileName by remember { mutableStateOf("workflow_snapshot_v31.json") }
    var artifactType by remember { mutableStateOf("workflow_json") }
    var contentPreview by remember { mutableStateOf("{\"name\":\"BNAI Verified Snapshot\"}") }
    var notifTitle by remember { mutableStateOf("Production Stage Update") }
    var notifMessage by remember { mutableStateOf("Task completed verification gate.") }
    var notifChannel by remember { mutableStateOf("in_app") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Files, Notifications, User Quotas & Audit [${selectedProject.namespace}]", fontSize = 17.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.loadNotificationsAndUserDashboard() }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("User Quotas & Notifs", fontSize = 11.sp) }
            OutlinedButton(onClick = { viewModel.runUi35FinalCapabilityAudit() }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("UI-35 Capability Audit", fontSize = 11.sp) }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Project-Scoped Files & Redacted Exports", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("File Name") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = artifactType, onValueChange = { artifactType = it }, label = { Text("Type") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = contentPreview, onValueChange = { contentPreview = it }, label = { Text("Content Preview (Auto-Redacted)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.uploadProjectFileMetadata(fileName, artifactType, contentPreview) }, enabled = !isLoading) { Text("Upload Metadata", fontSize = 11.sp) }
                    OutlinedButton(onClick = { viewModel.triggerRedactedProjectExport("JSON") }, enabled = !isLoading) { Text("Export JSON", fontSize = 11.sp) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Multi-Channel Notifications", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = notifChannel, onValueChange = { notifChannel = it }, label = { Text("Channel") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = notifMessage, onValueChange = { notifMessage = it }, label = { Text("Notification Body") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.sendMultiChannelNotification(notifTitle, notifMessage, notifChannel) }, enabled = !isLoading) { Text("Dispatch Verified Notification") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Files, Notifications & UI-35 Audit Telemetry:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = output, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
