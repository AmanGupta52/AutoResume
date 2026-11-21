@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 72) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { show = !show }) {
                Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (show) "Hide password" else "Show password")
            }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Login, registration, email code and password reset. Shown until the person is signed in. */
@Composable
fun AuthScreen(vm: ResumeViewModel) {
    val stage by vm.authStage.collectAsState()
    val busy by vm.authBusy.collectAsState()
    val error by vm.authError.collectAsState()
    val notice by vm.authNotice.collectAsState()
    val pending by vm.pendingEmail.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    // seconds before "send a new code" works again
    var cooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(stage) {
        code = ""
        if (stage == AuthStage.VERIFY) cooldown = 60
    }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) { delay(1000); cooldown -= 1 }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text("AutoResume", style = MaterialTheme.typography.headlineMedium)
        Text(
            when (stage) {
                AuthStage.LOGIN -> "Log in to see your resumes on every device."
                AuthStage.REGISTER -> "Create an account. Your resumes are saved to it, so they follow you to the web."
                AuthStage.VERIFY -> "Check your email"
                AuthStage.FORGOT -> "Enter your email and we will send you a code."
                AuthStage.RESET -> "Choose a new password"
            },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )

        notice?.let {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        error?.let {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        when (stage) {
            AuthStage.LOGIN -> {
                Field("Email", email, { email = it }, Modifier.fillMaxWidth(), keyboard = KeyboardType.Email)
                PasswordField("Password", password) { password = it }
                PrimaryAction(if (busy) "Logging in..." else "Log in", busy || email.isBlank() || password.isBlank()) { vm.login(email, password) }
                TextButton(onClick = { vm.authGo(AuthStage.FORGOT) }) { Text("Forgot password?") }
                TextButton(onClick = { vm.authGo(AuthStage.REGISTER) }) { Text("Create an account") }
            }
            AuthStage.REGISTER -> {
                Field("Your name", name, { name = it }, Modifier.fillMaxWidth())
                Field("Email", email, { email = it }, Modifier.fillMaxWidth(), keyboard = KeyboardType.Email)
                PasswordField("Password", password) { password = it }
                Text("At least 8 characters, with a letter and a number.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryAction(if (busy) "Creating..." else "Create account", busy || name.isBlank() || email.isBlank() || password.length < 8) { vm.register(name, email, password) }
                TextButton(onClick = { vm.authGo(AuthStage.LOGIN) }) { Text("I already have an account") }
            }
            AuthStage.VERIFY -> {
                Text(pending, style = MaterialTheme.typography.bodyMedium)
                Field("6-digit code", code, { v -> code = v.filter { it.isDigit() }.take(6) }, Modifier.fillMaxWidth(), keyboard = KeyboardType.Number)
                PrimaryAction(if (busy) "Checking..." else "Verify email", busy || code.length != 6) { vm.verify(code) }
                TextButton(onClick = { vm.resendCode(); cooldown = 60 }, enabled = !busy && cooldown == 0) {
                    Text(if (cooldown > 0) "Send a new code (${cooldown}s)" else "Send a new code")
                }
                TextButton(onClick = { vm.authGo(AuthStage.LOGIN) }) { Text("Back to log in") }
            }
            AuthStage.FORGOT -> {
                Field("Email", email, { email = it }, Modifier.fillMaxWidth(), keyboard = KeyboardType.Email)
                PrimaryAction(if (busy) "Sending..." else "Send code", busy || email.isBlank()) { vm.forgotPassword(email) }
                TextButton(onClick = { vm.authGo(AuthStage.LOGIN) }) { Text("Back to log in") }
            }
            AuthStage.RESET -> {
                Field("6-digit code", code, { v -> code = v.filter { it.isDigit() }.take(6) }, Modifier.fillMaxWidth(), keyboard = KeyboardType.Number)
                PasswordField("New password", password) { password = it }
                PrimaryAction(if (busy) "Saving..." else "Change password", busy || code.length != 6 || password.length < 8) { vm.resetPassword(code, password) }
                TextButton(onClick = { vm.authGo(AuthStage.LOGIN) }) { Text("Back to log in") }
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, disabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !disabled, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.small) {
        Text(label)
    }
}
