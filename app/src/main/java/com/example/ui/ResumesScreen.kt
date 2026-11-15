@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Resume
import com.example.ui.theme.ResumeAccents
import java.text.DateFormat
import java.util.Date

@Composable
fun ResumesScreen(vm: ResumeViewModel) {
    val resumes by vm.resumes.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Resume?>(null) }
    var toRename by remember { mutableStateOf<Resume?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your resumes", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        if (resumes.isEmpty()) "Create your first resume in a minute." else "${resumes.size} saved on this device",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (resumes.isEmpty()) {
                item {
                    EmptyState("Nothing here yet", "Start from a blank page or fill an example to see how the templates look.")
                }
            }
            items(resumes, key = { it.id }) { r ->
                ResumeCard(
                    r,
                    onOpen = { vm.openEditor(r) },
                    onDuplicate = { vm.duplicate(r) },
                    onRename = { toRename = r },
                    onDelete = { toDelete = r },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showNew = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New resume") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        var sample by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New resume") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field("Name (only you see this)", name, { name = it }, placeholder = "e.g. Product designer 2026")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !sample, onClick = { sample = false })
                        Text("Blank resume")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sample, onClick = { sample = true })
                        Text("Fill with an example")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.create(name, sample); showNew = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } },
        )
    }

    toRename?.let { r ->
        var name by remember(r.id) { mutableStateOf(r.name) }
        AlertDialog(
            onDismissRequest = { toRename = null },
            title = { Text("Rename") },
            text = { Field("Name", name, { name = it }) },
            confirmButton = { TextButton(onClick = { vm.rename(r, name); toRename = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { toRename = null }) { Text("Cancel") } },
        )
    }

    toDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this resume?") },
            text = { Text("\"${r.name}\" will be removed from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(r); toDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ResumeCard(r: Resume, onOpen: () -> Unit, onDuplicate: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    SoftCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TemplateThumb(
                ResumeHtmlHelper.normalizeTemplate(r.templateId),
                ResumeAccents.of(r.colorSchemeId),
                Modifier.size(width = 58.dp, height = 78.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(r.name.ifBlank { "Untitled resume" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    r.jobTitle.ifBlank { "No job title yet" },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Edited " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(r.lastModified)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (r.atsScore > 0) {
                    Spacer(Modifier.height(2.dp))
                    Tag("ATS ${r.atsScore}%", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
