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

        SoftCard(Modifier.fillMaxWidth()) {
            val aiOn by vm.aiAvailableFlow.collectAsState()
            var keyInput by remember { mutableStateOf("") }
            var saved by remember { mutableStateOf(vm.hasSavedKey()) }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AI writing help", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (aiOn) "On. Suggestions, summary, bullets, skills and the Ask AI helper are available in the editor."
                    else "Off. Everything else works without it. Paste a free NVIDIA key below to turn it on.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Field("NVIDIA API key", keyInput, { keyInput = it }, placeholder = "nvapi-...", keyboard = androidx.compose.ui.text.input.KeyboardType.Password)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setApiKey(keyInput); keyInput = ""; saved = vm.hasSavedKey() }, enabled = keyInput.isNotBlank(), shape = MaterialTheme.shapes.small) { Text("Save key") }
                    if (saved) OutlinedButton(onClick = { vm.setApiKey(""); saved = false }, shape = MaterialTheme.shapes.small) { Text("Remove key") }
                }
                Text(
                    "Get a free key at build.nvidia.com. It is stored only on this phone.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
