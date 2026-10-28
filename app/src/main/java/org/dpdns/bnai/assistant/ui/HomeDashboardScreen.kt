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
fun HomeDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSection: (String) -> Unit
) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val sessionMode by viewModel.sessionMode.collectAsState()
    val backendHealthy by viewModel.backendHealthy.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val approvals by viewModel.approvals.collectAsState()
    val incidents by viewModel.incidents.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Welcome to BNAI Unified Control Center", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "Active Project: ${selectedProject.namespace} (${selectedProject.name})", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                Text(text = "Session Mode: $sessionMode | Backend Connection: ${if (backendHealthy) "Verified" else "Offline"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onNavigateToSection("CHAT") }, modifier = Modifier.weight(1f)) { Text("Ask AI", fontSize = 12.sp) }
            Button(onClick = { onNavigateToSection("COPILOT") }, modifier = Modifier.weight(1f)) { Text("Open Copilot", fontSize = 12.sp) }
            Button(onClick = { onNavigateToSection("CINEMATIC") }, modifier = Modifier.weight(1f)) { Text("Cinematic", fontSize = 12.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onNavigateToSection("WF_BUILDER") }, modifier = Modifier.weight(1f)) { Text("WF Builder", fontSize = 12.sp) }
            OutlinedButton(onClick = { onNavigateToSection("TASKS") }, modifier = Modifier.weight(1f)) { Text("Tasks & Approvals", fontSize = 12.sp) }
            OutlinedButton(onClick = { onNavigateToSection("MEMORY") }, modifier = Modifier.weight(1f)) { Text("7-Layer Memory", fontSize = 12.sp) }
        }

        Text("Logical Projects (Isolated Contexts)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        viewModel.projects.forEach { proj ->
            val isSelected = proj.id == selectedProject.id
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${proj.name} [${proj.namespace}]", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = proj.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { viewModel.selectProject(proj) }, enabled = !isSelected) {
                        Text(if (isSelected) "Active" else "Switch", fontSize = 11.sp)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Attention Required (Verified Evidence Only)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Pending Approvals: ${approvals.count { it.status == "pending" }}", fontSize = 12.sp)
                Text("• Active / Blocked Tasks: ${tasks.count { it.status != "COMPLETED" }}", fontSize = 12.sp)
                Text("• Security / Governance Alerts: ${incidents.size}", fontSize = 12.sp)
            }
        }
    }
}
