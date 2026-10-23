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
    val authStep by viewModel.authStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BNAI Assistant v3.0", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))

        if (authError != null) {
            Text(authError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
        }

        when (authStep) {
            "LOGIN" -> {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.login(email.trim(), password.trim()) }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && email.isNotBlank()) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Login")
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { viewModel.setAuthStep("SIGNUP") }) { Text("New user? Sign Up") }
            }
            "SIGNUP" -> {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (Min 8 chars, 1 Upper, 1 Digit)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.signup(email.trim(), password.trim()) }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && email.isNotBlank()) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Send OTP")
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { viewModel.setAuthStep("LOGIN") }) { Text("Already have an account? Login") }
            }
            "OTP" -> {
                Text("Enter 6-digit OTP sent to $email", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = otp, onValueChange = { if (it.length <= 6) otp = it }, label = { Text("OTP Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.verifyOtp(email.trim(), otp.trim()) }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && otp.length == 6) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Verify OTP")
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { viewModel.setAuthStep("LOGIN") }) { Text("Back to Login") }
            }
        }
    }
}
