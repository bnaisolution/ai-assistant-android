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
fun OwnerAdminScreen(viewModel: MainViewModel) {
    val isOwner by viewModel.isOwner.collectAsState()
    val ownerTelemetry by viewModel.ownerDashboardJson.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val lastToken by viewModel.lastIssuedApprovalToken.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var ownerActionType by remember { mutableStateOf("TRIGGER_BACKUP_VERIFICATION") }
    var ownerTargetResource by remember { mutableStateOf("system_baseline_v31") }
    var ownerApprovalToken by remember { mutableStateOf("") }

    LaunchedEffect(lastToken) { if (!lastToken.isNullOrBlank()) ownerApprovalToken = lastToken!! }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Owner Admin Governance Console", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        if (!isOwner) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Restricted Section (UI-19)", fontWeight = FontWeight.Bold)
                    Text("Normal users cannot access Owner-only schema drift or administrative controls. Backend enforces owner_admin role.", fontSize = 12.sp)
                }
            }
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("System Baseline & Schema Governance", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.loadOwnerAdminTelemetry() }, enabled = !isLoading) { Text("Owner Dashboard", fontSize = 11.sp) }
                    OutlinedButton(onClick = { viewModel.loadOwnerSchemaRegistry() }, enabled = !isLoading) { Text("Schema Check", fontSize = 11.sp) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Protected Administrative Action", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = ownerActionType, onValueChange = { ownerActionType = it }, label = { Text("Action Type") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ownerTargetResource, onValueChange = { ownerTargetResource = it }, label = { Text("Target Resource") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ownerApprovalToken, onValueChange = { ownerApprovalToken = it }, label = { Text("SHA-256 Approval Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { viewModel.triggerOwnerConsoleAction(ownerActionType, ownerTargetResource, ownerApprovalToken.ifBlank { null }) }, enabled = !isLoading && ownerActionType.isNotBlank()) { Text("Execute Authorized Owner Action") }
            }
        }

        Text("Active Security & Configuration Incidents:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        incidents.forEach { inc ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${inc.id} [${inc.severity}] — ${inc.category}", fontWeight = FontWeight.Bold)
                    Text("Component: ${inc.component} | Status: ${inc.status}", fontSize = 12.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Live Backend Owner Telemetry JSON:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = ownerTelemetry, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
