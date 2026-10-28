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
fun TasksAndApprovalsScreen(viewModel: MainViewModel) {
    val progressSummary by viewModel.projectProgressSummary.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val approvals by viewModel.approvals.collectAsState()
    val lastIssuedToken by viewModel.lastIssuedApprovalToken.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var newTaskType by remember { mutableStateOf("") }
    var newTaskGoal by remember { mutableStateOf("") }
    var reminderTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.syncLiveTasksAndApprovals() }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tasks, Queue & Approvals (WF-06 / WF-08)", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { viewModel.syncLiveTasksAndApprovals() }, enabled = !isLoading) { Text("Sync DB", fontSize = 11.sp) }
                        Button(onClick = { viewModel.fetchProjectProgressWf06() }, enabled = !isLoading) { Text("WF-06", fontSize = 11.sp) }
                    }
                }
                Text(progressSummary, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTaskType, onValueChange = { newTaskType = it }, label = { Text("Task Title (WF-08)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = newTaskGoal, onValueChange = { newTaskGoal = it }, label = { Text("Goal") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(
                        onClick = { viewModel.createTaskWf08(newTaskType, newTaskGoal); newTaskType = ""; newTaskGoal = "" },
                        enabled = !isLoading && newTaskType.isNotBlank() && newTaskGoal.isNotBlank()
                    ) { Text("+Task") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = reminderTitle, onValueChange = { reminderTitle = it }, label = { Text("Reminder Title (WF-10)") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(
                        onClick = { viewModel.scheduleReminderWf10(reminderTitle, 30); reminderTitle = "" },
                        enabled = !isLoading && reminderTitle.isNotBlank()
                    ) { Text("+30m Reminder") }
                }
            }
        }

        if (!lastIssuedToken.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Latest SHA-256 Bound Approval Token:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(lastIssuedToken!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (approvals.isNotEmpty()) {
                item { Text("Approval Inbox (SHA-256 Action-Bound):", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                items(approvals, key = { it.id }) { appr ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "${appr.actionType} on ${appr.resourceId} [${appr.status.uppercase()}]", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Reason: ${appr.reason} | Risk: ${appr.riskCategory}", fontSize = 11.sp)
                            if (appr.status == "pending" && isOwner) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.submitApprovalDecision(appr.id, "approved") }) { Text("Approve (Owner)", fontSize = 11.sp) }
                                    OutlinedButton(onClick = { viewModel.submitApprovalDecision(appr.id, "rejected") }) { Text("Reject", fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
            }

            item { Text("Persistent Tasks, Cancellation & Reconciliation (UI-20):", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            items(tasks, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${task.title} [${task.status}]", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Goal: ${task.goal}", fontSize = 12.sp)
                            Text("ID: ${task.id.take(12)}... | Priority: ${task.priority}", fontSize = 11.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (task.cancelEligible) {
                                OutlinedButton(onClick = { viewModel.requestTaskCancellation(task.id) }) { Text("Cancel", fontSize = 10.sp) }
                            }
                            OutlinedButton(onClick = { viewModel.reconcileTaskOutcome(task.id, "resume_check") }) { Text("Reconcile", fontSize = 10.sp) }
                        }
                    }
                }
            }

            items(reminders, key = { it.id }) { rem ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Reminder: ${rem.title} [${rem.status}]", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = "Scheduled: ${rem.scheduledAt}", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
