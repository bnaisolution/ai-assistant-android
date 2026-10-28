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
fun WorkflowBuilderScreen(viewModel: MainViewModel) {
    val output by viewModel.wfBuilderOutput.collectAsState()
    val lastGenJson by viewModel.lastGeneratedWfJson.collectAsState()
    val repairCount by viewModel.repairAttemptCount.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var requirementsText by remember { mutableStateOf("") }
    var workflowJsonInput by remember { mutableStateOf("") }
    var requestedChanges by remember { mutableStateOf("") }
    var liveWfId by remember { mutableStateOf("") }
    var docQuestion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "n8n WF Builder & Inspector [${selectedProject.namespace}]", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (repairCount > 0) {
            Text(text = "Auto-Repair Cycle Attempts Used: $repairCount / 5 (Blueprint v1.6 Part 13 Limit)", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "1. Generate + Validate + Max 5x Auto-Repair (WF-02 -> WF-04 -> WF-03)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = requirementsText, onValueChange = { requirementsText = it }, label = { Text("Blueprint / Workflow Requirements") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.generateAndAutoRepairWorkflow(requirementsText) }, enabled = !isLoading) { Text("Build & Verify WF") }
                    if (lastGenJson.isNotBlank()) {
                        OutlinedButton(onClick = { workflowJsonInput = lastGenJson }) { Text("Load Generated JSON") }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Analyze (WF-01) or Controlled Update (WF-03)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = workflowJsonInput, onValueChange = { workflowJsonInput = it }, label = { Text("Paste Workflow JSON (Uploaded Mode)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = requestedChanges, onValueChange = { requestedChanges = it }, label = { Text("Requested Changes (for WF-03 Update)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.analyzeWorkflowWf01(workflowJsonInput) }, enabled = !isLoading) { Text("Audit (WF-01)") }
                    Button(onClick = { viewModel.updateWorkflowWf03(workflowJsonInput, requestedChanges) }, enabled = !isLoading) { Text("Update (WF-03)") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Doc Research (WF-05) & Connected n8n Inspector (WF-09)", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = docQuestion, onValueChange = { docQuestion = it }, label = { Text("n8n Documentation / API Question (WF-05)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.researchDocsWf05(docQuestion) }, enabled = !isLoading) { Text("Research (WF-05)") }
                    Button(onClick = { viewModel.listLiveWorkflowsWf09() }, enabled = !isLoading) { Text("List Live WFs (WF-09)") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = liveWfId, onValueChange = { liveWfId = it }, label = { Text("Live WF ID") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(onClick = { viewModel.getLiveWorkflowWf09(liveWfId) }, enabled = !isLoading) { Text("Fetch Live WF") }
                }
            }
        }

        if (output.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Execution Result & Verification Evidence:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = output, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}
