@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(vm: ResumeViewModel) {
    var name by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        EmptyState(
            "Welcome to AutoResume",
            "Three quick details and your first resume is started. You can change everything later.",
            Modifier.padding(0.dp),
        )
        Field("Your name", name, { name = it })
        Field("The job you want", job, { job = it }, placeholder = "e.g. Graphic Designer")
        Field("Email (optional)", email, { email = it }, keyboard = KeyboardType.Email)
        Button(
            onClick = { vm.finishOnboarding(name, job, email) },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.small,
        ) { Text("Create my resume") }
        TextButton(onClick = { vm.finishOnboarding("", "", "") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Skip for now")
        }
    }
}
