@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private fun scoreColor(s: Int) = when {
    s >= 80 -> Color(0xFF16A34A)
    s >= 60 -> Color(0xFFD97706)
    else -> Color(0xFFDC2626)
}

@Composable
fun AtsScreen(vm: ResumeViewModel) {
    val resumes by vm.resumes.collectAsState()
    val result by vm.atsResult.collectAsState()
    val aiText by vm.atsAiText.collectAsState()
    val aiBusy by vm.aiBusy.collectAsState()
    val presets = AtsAnalyzerEngine.commonJobDescriptions
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    var selectedId by remember { mutableStateOf<Int?>(null) }
    var jd by remember { mutableStateOf("") }
    var presetKeywords by remember { mutableStateOf(emptyList<String>()) }
    val selected = resumes.firstOrNull { it.id == selectedId } ?: resumes.firstOrNull()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ATS check", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "See how well a resume matches a job posting before you apply.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (resumes.isEmpty()) {
            EmptyState("No resume to check", "Create a resume first, then come back here.")
            return@Column
        }

        SectionTitle("1. Choose a resume")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            resumes.forEach { r ->
                FilterChip(selected = selected?.id == r.id, onClick = { selectedId = r.id }, label = { Text(r.name.ifBlank { "Untitled" }) })
            }
        }

        SectionTitle("2. Paste the job description")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { p ->
                AssistChip(onClick = { jd = p.description; presetKeywords = p.keywords }, label = { Text(p.label) })
            }
        }
        Field("Job description", jd, { jd = it; presetKeywords = emptyList() }, singleLine = false, minLines = 6, placeholder = "Paste the full posting, or tap an example above")

        Button(
            onClick = { selected?.let { vm.analyze(it, jd, presetKeywords) } },
            enabled = selected != null && jd.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(Icons.Filled.QueryStats, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Analyze")
        }

        result?.let { r ->
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        val track = MaterialTheme.colorScheme.surfaceVariant
                        val col = scoreColor(r.overallScore)
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 16.dp.toPx()
                            val inset = stroke / 2
                            val s = Size(size.width - stroke, size.height - stroke)
                            drawArc(track, 135f, 270f, false, Offset(inset, inset), s, style = Stroke(stroke, cap = StrokeCap.Round))
                            drawArc(col, 135f, 270f * r.overallScore / 100f, false, Offset(inset, inset), s, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${r.overallScore}", style = MaterialTheme.typography.headlineMedium)
                            Text("out of 100", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    listOf(
                        "Keyword match" to r.keywordsMatchPercent,
                        "Completeness" to r.completenessScore,
                        "Formatting" to r.formattingScore,
                        "Readability" to r.readabilityScore,
                        "Contact info" to r.contactInfoScore,
                    ).forEach { (label, v) ->
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text("$v%", style = MaterialTheme.typography.labelLarge)
                            }
                            LinearProgressIndicator(progress = { v.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = scoreColor(v), trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round)
                        }
                    }
                }
            }

            if (r.foundKeywords.isNotEmpty() || r.missingKeywords.isNotEmpty()) {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Keywords", style = MaterialTheme.typography.titleMedium)
                        if (r.foundKeywords.isNotEmpty()) {
                            Text("Found in your resume", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.foundKeywords.forEach { Tag(it, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) }
                            }
                        }
                        if (r.missingKeywords.isNotEmpty()) {
                            Text("Missing - add them if they are true for you", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.missingKeywords.forEach { Tag(it, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer) }
                            }
                        }
                    }
                }
            }

            if (r.suggestions.isNotEmpty()) {
                SectionTitle("What to improve")
                r.suggestions.forEach { s ->
                    SoftCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(s.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                                Tag(s.severity, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(s.instruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            FilledTonalButton(
                onClick = { selected?.let { vm.aiImprove(it, jd) } },
                enabled = aiBusy == null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (aiBusy == "ats") "Thinking..." else "Get AI suggestions for this job")
            }
            if (aiText.isNotBlank()) {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(aiText, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(aiText)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
