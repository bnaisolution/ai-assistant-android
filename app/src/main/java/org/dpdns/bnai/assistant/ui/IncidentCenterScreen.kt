package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.assistant.data.Incident

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentCenterScreen(
    incidents: List<Incident>,
    onAcknowledge: (String) -> Unit,
    isLoading: Boolean
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Incident Center") }) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (incidents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No active incidents")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(incidents) { incident ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = incident.error_type, style = MaterialTheme.typography.titleMedium)
                            Text(text = incident.error_message, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Status: ${incident.recovery_status}", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onAcknowledge(incident.id) }) {
                                Text("Acknowledge")
                            }
                        }
                    }
                }
            }
        }
    }
}
