@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.BuildConfig

@Composable
fun SettingsScreen(vm: ResumeViewModel) {
    val mode by vm.themeMode.collectAsState()
    val lang by vm.language.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (id, label) ->
                        SegmentedButton(
                            selected = mode == id,
                            onClick = { vm.setThemeMode(id) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(label) }
                    }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Language", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Lang.options.forEachIndexed { i, (code, label) ->
                        SegmentedButton(
                            selected = lang == code,
                            onClick = { vm.setLanguage(code) },
                            shape = SegmentedButtonDefaults.itemShape(i, Lang.options.size),
                        ) { Text(label) }
                    }
                }
                Text(
                    "Changes the app menus and buttons. Your resume text is never translated.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (com.example.api.BackendClient.isConfigured) SoftCard(Modifier.fillMaxWidth()) {
            val account by vm.auth.collectAsState()
            val syncText by vm.syncStatus.collectAsState()
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Account", style = MaterialTheme.typography.titleMedium)
                Text(
                    account?.let { "${it.name}\n${it.email}" } ?: "Not signed in",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Your resumes are saved to your account and sync with the web app. PDFs stay on this phone only.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (syncText.isNotBlank()) Text(syncText, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.runSync() }, shape = MaterialTheme.shapes.small) { Text("Sync now") }
                    OutlinedButton(onClick = { vm.logout() }, shape = MaterialTheme.shapes.small) { Text("Sign out") }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            val aiOn by vm.aiAvailableFlow.collectAsState()
            val busy by vm.aiBusy.collectAsState()
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AI writing help", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (aiOn) "On. The key is built into this app. Suggestions, summary, bullets, skills and the Ask AI helper work in the editor."
                    else "Off. This build has no NVIDIA key. Add NVIDIA_API_KEY to your GitHub secrets and rebuild the app.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (aiOn) OutlinedButton(onClick = { vm.testAi() }, enabled = busy == null, shape = MaterialTheme.shapes.small) {
                    Text(if (busy == "test") "Testing..." else "Test AI connection")
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your data", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Resumes are stored only on this phone. Nothing is uploaded unless you use an AI feature, which sends the text you are working on to NVIDIA's AI API.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text("AutoResume ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
