@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ResumeAccents

@Composable
fun PreviewScreen(vm: ResumeViewModel) {
    val r by vm.draft.collectAsState()
    val resume = r ?: return
    val context = LocalContext.current
    var darkPage by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    // ONE document: this exact HTML is what the preview shows and what the PDF prints.
    val html = remember(resume, darkPage) { ResumeHtmlHelper.generateHtml(context, resume, darkPage, forPreview = true) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.go(Screen.EDITOR) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to editor") }
            Column(Modifier.weight(1f)) {
                Text("Preview", style = MaterialTheme.typography.titleMedium)
                Text("This is exactly what the PDF will look like", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            ResumeWebPreview(html, Modifier.fillMaxSize())
        }

        Surface(
            color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 12.dp, modifier = Modifier.padding(top = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TemplateNames) { name ->
                        FilterChip(
                            selected = ResumeHtmlHelper.normalizeTemplate(resume.templateId) == name,
                            onClick = { vm.update { it.copy(templateId = name) } },
                            label = { Text(name) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResumeAccents.all.forEach { (id, c) ->
                            Swatch(c, resume.colorSchemeId == id, id) { vm.update { it.copy(colorSchemeId = id) } }
                        }
                    }
                    FilterChip(selected = darkPage, onClick = { darkPage = !darkPage }, label = { Text("Dark page") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (!exporting) {
                                exporting = true
                                ResumePdfHelper.exportPdf(context, resume, darkPage) { file ->
                                    exporting = false
                                    if (file == null) {
                                        android.widget.Toast.makeText(context, "Could not create the PDF. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                                    } else try {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Resume - ${resume.fullName}")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Save or share PDF"))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Cannot share PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !exporting,
                        modifier = Modifier.weight(1.5f).height(52.dp), shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (exporting) "Creating PDF..." else "Save / share PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            try { ResumeHtmlHelper.printResume(context, resume, darkPage) }
                            catch (e: Exception) { android.widget.Toast.makeText(context, "Cannot open print dialog", android.widget.Toast.LENGTH_SHORT).show() }
                        },
                        modifier = Modifier.weight(1f).height(52.dp), shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Print")
                    }
                }
            }
        }
    }
}
