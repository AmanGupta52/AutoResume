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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ResumeAccents
import kotlin.math.ceil

@Composable
fun PreviewScreen(vm: ResumeViewModel) {
    val r by vm.draft.collectAsState()
    val resume = r ?: return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var darkPage by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var pageHeight by remember { mutableIntStateOf(0) }
    var measureTick by remember { mutableIntStateOf(0) }
    var fitRequested by remember { mutableStateOf(false) }

    // ONE document: this exact HTML is what the preview shows and what the PDF prints.
    val html = remember(resume, darkPage) { ResumeHtmlHelper.generateHtml(context, resume, darkPage, forPreview = true) }

    val marginPx = ResumeHtmlHelper.marginMmOf(resume.margins) * 3.78
    val usable = (1123 - 2 * marginPx).coerceAtLeast(300.0)
    val pages = if (pageHeight <= 0) 0 else maxOf(1, ceil((pageHeight - 2 * marginPx) / usable - 0.02).toInt())

    LaunchedEffect(measureTick) {
        if (fitRequested && pages > 0) {
            fitRequested = false
            vm.say(if (pages <= 1) "Fits on one page." else "Still about $pages pages. Shorten bullets or hide a section to fit one page.")
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.go(Screen.EDITOR) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to editor") }
            Column(Modifier.weight(1f)) {
                Text("Preview", style = MaterialTheme.typography.titleMedium)
                Text("This is exactly what the PDF will look like", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (pages > 0) Tag(if (pages == 1) "1 page" else "About $pages pages",
                if (pages == 1) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                if (pages == 1) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            ResumeWebPreview(html, Modifier.fillMaxSize(), onContentHeight = { h -> pageHeight = h; measureTick++ })
        }

        Surface(
            color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 12.dp, modifier = Modifier.padding(top = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TemplateNames) { name ->
                        FilterChip(
                            selected = ResumeHtmlHelper.normalizeTemplate(resume.templateId) == name,
                            onClick = { vm.update { it.copy(templateId = name) } },
                            label = { Text(name) },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        ResumeAccents.all.forEach { (id, c) ->
                            Swatch(c, resume.colorSchemeId == id, id) { vm.update { it.copy(colorSchemeId = id) } }
                        }
                    }
                    FilterChip(selected = darkPage, onClick = { darkPage = !darkPage }, label = { Text("Dark page") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            fitRequested = true
                            vm.updateDesign { it.copy(size = "Compact", tight = true) }
                            vm.update { it.copy(margins = "Narrow") }
                        },
                        label = { Text("Fit to 1 page") },
                        leadingIcon = { Icon(Icons.Filled.FitScreen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (busy == null) {
                                busy = "pdf"
                                ResumePdfHelper.exportPdf(context, resume, darkPage) { file ->
                                    busy = null
                                    if (file == null) android.widget.Toast.makeText(context, "Could not create the PDF. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                                    else {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // the PDF is kept on this phone only; it is never sent to the server
                                        val where = LocalFiles.savePdf(context, file)
                                        android.widget.Toast.makeText(
                                            context,
                                            if (where != null) "PDF saved on your phone: $where" else "Could not save a copy on the phone. You can still share it.",
                                            android.widget.Toast.LENGTH_LONG,
                                        ).show()
                                        vm.storePdf(resume.id, file) // also kept in your account (Cloudinary link in MongoDB)
                                        shareFile(context, file, "application/pdf", "Resume - ${resume.fullName}")
                                    }
                                }
                            }
                        },
                        enabled = busy == null,
                        modifier = Modifier.weight(1.4f).height(52.dp), shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy == "pdf") "Creating..." else "PDF")
                    }
                    FilledTonalButton(
                        onClick = {
                            try {
                                val f = DocxExporter.export(context, resume)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                shareFile(context, f, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Resume - ${resume.fullName}")
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Could not create the Word file.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1.2f).height(52.dp), shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Word")
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
