@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun CoverLetterScreen(vm: ResumeViewModel) {
    val resumes by vm.resumes.collectAsState()
    val text by vm.letterText.collectAsState()
    val sharedJd by vm.jobDescription.collectAsState()
    val busy by vm.aiBusy.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    var selectedId by remember { mutableStateOf<Int?>(null) }
    var role by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var manager by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("Professional") }
    var jd by remember(sharedJd) { mutableStateOf(sharedJd) }
    var exporting by remember { mutableStateOf(false) }
    val selected = resumes.firstOrNull { it.id == selectedId } ?: resumes.firstOrNull()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cover letter", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                if (vm.aiAvailable) "Written from your resume and the job posting. Always read and edit it before sending."
                else "AI is off, so you get a fill-in draft built from your resume. Edit it before sending.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (resumes.isEmpty()) {
            EmptyState("No resume yet", "Create a resume first. The letter uses its details.")
            return@Column
        }

        SectionTitle("Resume to use")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            resumes.forEach { r -> FilterChip(selected = selected?.id == r.id, onClick = { selectedId = r.id }, label = { Text(r.name.ifBlank { "Untitled" }) }) }
        }

        Field("Role you are applying for", role, { role = it }, placeholder = selected?.jobTitle?.ifBlank { null } ?: "e.g. Marketing Manager")
        Field("Company", company, { company = it })
        Field("Hiring manager (optional)", manager, { manager = it }, placeholder = "Leave empty for \"Hiring Manager\"")
        Field("Job description (optional)", jd, { jd = it }, singleLine = false, minLines = 4)

        SectionTitle("Tone")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Professional", "Friendly", "Confident").forEach { t -> FilterChip(selected = tone == t, onClick = { tone = t }, label = { Text(t) }) }
        }

        Button(
            onClick = { selected?.let { vm.generateLetter(it, role, company, manager, tone, jd) } },
            enabled = selected != null && busy == null,
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.small,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (busy == "letter") "Writing..." else "Write the letter")
        }

        if (text.isNotBlank()) {
            Field("Your letter (editable)", text, { vm.letterText.value = it }, singleLine = false, minLines = 12)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val r = selected
                        if (r != null && !exporting) {
                            exporting = true
                            val safe = r.fullName.ifBlank { "Cover" }.replace(Regex("[^A-Za-z0-9_-]+"), "_")
                            ResumePdfHelper.exportHtml(context, CoverLetter.html(r, text, company), "${safe}_Cover_Letter") { f ->
                                exporting = false
                                if (f == null) android.widget.Toast.makeText(context, "Could not create the PDF.", android.widget.Toast.LENGTH_LONG).show()
                                else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    shareFile(context, f, "application/pdf", "Cover letter - ${r.fullName}")
                                }
                            }
                        }
                    },
                    enabled = !exporting,
                    modifier = Modifier.weight(1f).height(52.dp), shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (exporting) "Creating..." else "PDF")
                }
                OutlinedButton(
                    onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(text)); vm.say("Copied") },
                    modifier = Modifier.weight(1f).height(52.dp), shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
