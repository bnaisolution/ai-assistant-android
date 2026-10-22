package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowBuilderScreen(viewModel: MainViewModel) {
    val isBuilding by viewModel.isBuilding.collectAsState()
    val output by viewModel.builderOutput.collectAsState()
    var requirements by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("n8n Workflow Generator", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = requirements,
                    onValueChange = { requirements = it },
                    label = { Text("Describe the workflow...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Row for Generate & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.buildWorkflow(requirements) },
                        enabled = !isBuilding && requirements.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBuilding) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Building...")
                        } else {
                            Text("Generate Workflow")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            requirements = ""
                            viewModel.clearBuilder()
                        },
                        enabled = !isBuilding && (output.isNotEmpty() || requirements.isNotEmpty())
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Execution Log & JSON", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (output.isNotEmpty() && !isBuilding) {
                        TextButton(onClick = { viewModel.clearBuilder() }) {
                            Text("Clear Log", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.small)
                        .padding(8.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = if (output.isEmpty()) "Output will appear here..." else output,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}
