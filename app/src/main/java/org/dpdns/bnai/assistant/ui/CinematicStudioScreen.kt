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
fun CinematicStudioScreen(
    viewModel: MainViewModel,
    onOpenCopilotWithContext: () -> Unit
) {
    val stages by viewModel.cinematicStages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.syncLiveCinematicStudio() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Project B — AI Cinematic Studio", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(text = "Isolated Cinematic Namespace • Story Bible • Continuity • Scene & Media Pipeline", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.projects.find { it.namespace == "Cinematic" }?.let { viewModel.selectProject(it) }
                        onOpenCopilotWithContext()
                    }) {
                        Text("Open Cinematic Copilot", fontSize = 11.sp)
                    }
                }
            }
        }

        Text("15 Production Pipeline Sections:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stages, key = { it.stageId }) { stage ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${stage.stageId}: ${stage.sectionName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Mapped WF: ${stage.mappedWorkflow} | State: ${stage.status}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.triggerCinematicStageOperation(stage.stageId, "queue") }, enabled = !isLoading) { Text("Run/Queue", fontSize = 10.sp) }
                            if (stage.status == "Running" || stage.status == "Queued") {
                                TextButton(onClick = { viewModel.triggerCinematicStageOperation(stage.stageId, "cancel") }, enabled = !isLoading) { Text("Cancel", fontSize = 10.sp, color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
