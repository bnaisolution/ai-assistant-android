package org.dpdns.bnai.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.assistant.viewmodel.MainViewModel

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var email by remember { mutableStateOf("admin@example.com") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val backendHealthy by viewModel.backendHealthy.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "BNAI UNIFIED APP v1.1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "One Android App • One Primary AI Brain • Two Logical Projects", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (backendHealthy) "Backend v3.1: Connected (SSL Verified)" else "Backend Status: Unreachable",
            fontSize = 12.sp, color = if (backendHealthy) BnaiSuccess else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email (admin@example.com for Owner Role)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password or Trusted Service Token") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.login(email.trim(), password.trim()) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading && email.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Shared Secure Login (Assistant + Cinematic)")
        }
    }
}
