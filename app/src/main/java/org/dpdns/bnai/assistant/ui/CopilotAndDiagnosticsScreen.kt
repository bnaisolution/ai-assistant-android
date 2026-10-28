package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun CopilotAndDiagnosticsScreen(viewModel: MainViewModel) {
    val consoleOutput by viewModel.copilotConsoleOutput.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val baseHash by viewModel.lastBaseVersionHash.collectAsState()
    val propHash by viewModel.lastProposedChangeHash.collectAsState()
    val lastToken by viewModel.lastIssuedApprovalToken.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var wfId by remember { mutableStateOf("WF-00") }
    var failedNode by remember { mutableStateOf("Call Groq Primary") }
    var errorMsg by remember { mutableStateOf("401 Unauthorized: Invalid x-service-token") }
    var execLogs by remember { mutableStateOf("") }
    var screenshotOcrText by remember { mutableStateOf("HTTP 502 Bad Gateway at WF-00 Webhook node") }
    var screenshotQuestion by remember { mutableStateOf("Identify visible UI error and safe resolution") }
    var origJson by remember { mutableStateOf("{\"nodes\":[{\"name\":\"Webhook\"}]}") }
    var propJson by remember { mutableStateOf("{\"nodes\":[{\"name\":\"Webhook\"},{\"name\":\"Validator\"}]}") }
    var sandboxTarget by remember { mutableStateOf("Assistant / WF-04") }
    var sandboxUserConfirmed by remember { mutableStateOf(false) }
    var protectedTarget by remember { mutableStateOf("Assistant / WF-00") }
    var approvalTokenInput by remember { mutableStateOf("") }
    var rollbackTargetHash by remember { mutableStateOf("") }

    LaunchedEffect(lastToken) {
        if (!lastToken.isNullOrBlank()) approvalTokenInput = lastToken!!
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "AI Copilot, Inspector, Diff, Sandbox & Recovery (${selectedProject.namespace})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = "Rule: Generating a fix is a PROPOSAL ONLY, never an automatic live execution (Part 14.3).", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.inspectNamespacedWorkflows(selectedProject.namespace) }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Inspect ${selectedProject.namespace} WFs", fontSize = 11.sp) }
            OutlinedButton(onClick = { viewModel.loadCopilotDiagnosticHistory() }, modifier = Modifier.weight(1f), enabled = !isLoading) { Text("Diagnostic History", fontSize = 11.sp) }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Error Diagnosis (Evidence-Classified)", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = wfId, onValueChange = { wfId = it }, label = { Text("Workflow ID") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = failedNode, onValueChange = { failedNode = it }, label = { Text("Failed Node") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = errorMsg, onValueChange = { errorMsg = it }, label = { Text("Error Summary / Message") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = execLogs, onValueChange = { execLogs = it }, label = { Text("Execution Logs (Optional — Secrets Auto-Redacted)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = { viewModel.runCopilotDiagnosis(wfId, failedNode, errorMsg, execLogs) }, enabled = !isLoading) { Text("Diagnose & Propose Safe Fix") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Screenshot Vision Analysis (7-Day Ephemeral Policy)", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = screenshotOcrText, onValueChange = { screenshotOcrText = it }, label = { Text("Visible Screenshot OCR / UI Error Text") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = screenshotQuestion, onValueChange = { screenshotQuestion = it }, label = { Text("Analysis Question") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { viewModel.analyzeScreenshotOcr(screenshotOcrText, screenshotQuestion) }, enabled = !isLoading) { Text("Analyze Screenshot with Copilot") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Workflow Diff Viewer (TOCTOU Hash Binding)", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = origJson, onValueChange = { origJson = it }, label = { Text("Original Configuration JSON") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = propJson, onValueChange = { propJson = it }, label = { Text("Proposed Configuration JSON") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                if (baseHash.isNotBlank()) Text(text = "Base Hash: ${baseHash.take(16)}... | Proposed Hash: ${propHash.take(16)}...", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                Button(onClick = { viewModel.runWorkflowDiffViewer(wfId, origJson, propJson) }, enabled = !isLoading) { Text("Compare Before / After Diff") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("4. Isolated Sandbox Diagnostic Center", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = sandboxTarget, onValueChange = { sandboxTarget = it }, label = { Text("Namespaced Target Workflow") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sandboxUserConfirmed, onCheckedChange = { sandboxUserConfirmed = it })
                    Text(text = "I explicitly confirm isolated sandbox execution & accept residual risks", fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.runSandboxDiagnostic(sandboxTarget, false) }, enabled = !isLoading) { Text("1. Run Preflight") }
                    Button(onClick = { viewModel.runSandboxDiagnostic(sandboxTarget, sandboxUserConfirmed) }, enabled = !isLoading && sandboxUserConfirmed) { Text("2. Execute in Sandbox") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("5. Protected Changes & Backup/Rollback Center", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = protectedTarget, onValueChange = { protectedTarget = it }, label = { Text("Target Resource") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = approvalTokenInput, onValueChange = { approvalTokenInput = it }, label = { Text("SHA-256 Bound Approval Token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.createApprovalRequest("APPLY_PROTECTED_CHANGE", "workflow_or_system", protectedTarget, "Copilot Proposed Patch Verification") }, enabled = !isLoading) { Text("Request Approval", fontSize = 11.sp) }
                    Button(onClick = { viewModel.executeProtectedLiveChange(protectedTarget, "APPLY_PROTECTED_CHANGE", approvalTokenInput) }, enabled = !isLoading && approvalTokenInput.isNotBlank()) { Text("Apply Change", fontSize = 11.sp) }
                }
                HorizontalDivider()
                Text("Backup & Recovery Rollback:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = rollbackTargetHash, onValueChange = { rollbackTargetHash = it }, label = { Text("Target Recovery Version Hash") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.loadRecoveryPoints() }, enabled = !isLoading) { Text("Inspect Recovery Points", fontSize = 11.sp) }
                    Button(onClick = { viewModel.executeResourceRollback(protectedTarget, rollbackTargetHash, approvalTokenInput) }, enabled = !isLoading && rollbackTargetHash.isNotBlank() && approvalTokenInput.isNotBlank()) { Text("Execute Rollback", fontSize = 11.sp) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Diagnostic & Verification Evidence Output:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = consoleOutput, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
