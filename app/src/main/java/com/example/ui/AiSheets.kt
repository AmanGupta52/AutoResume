@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.api.AiError
import com.example.data.Resume

// ---------------------------------------------------------------- target job

@Composable
fun TargetJobDialog(initial: TargetJob, onSave: (TargetJob) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var desc by remember { mutableStateOf(initial.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Which job are you applying for?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Job title", title, { title = it }, placeholder = "e.g. Marketing Manager")
                Field("Job description (optional)", desc, { desc = it }, singleLine = false, minLines = 5,
                    placeholder = "Paste the job post here")
                Text(
                    "The AI, the suggestions and the ATS check all use this, so the advice fits the job.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(TargetJob(title.trim(), desc.trim())) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------- suggestion strip

/** 2-3 tappable cards at the top of a tab. Made offline, so they are free; AI runs only when one is tapped. */
@Composable
fun SuggestionStrip(vm: ResumeViewModel, tab: Int, r: Resume, onGoTab: (Int) -> Unit, onEditTarget: () -> Unit) {
    val aiOn by vm.aiAvailableFlow.collectAsState()
    val busy by vm.aiBusy.collectAsState()
    val items = remember(tab, r, aiOn) { ResumeCoach.suggestions(tab, r, aiOn) }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Suggestions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        items.forEach { s ->
            Surface(
                onClick = {
                    when (val a = s.action) {
                        is SuggestionAction.Ai -> vm.ask(a.req)
                        is SuggestionAction.Tip -> vm.say(a.text)
                        is SuggestionAction.GoTab -> onGoTab(a.tab)
                        is SuggestionAction.SetTitle -> vm.update { it.copy(jobTitle = a.title) }
                        is SuggestionAction.UseTemplate -> {
                            vm.update { it.copy(templateId = a.name) }
                            vm.say("Template changed to ${a.name}. ${a.why}")
                        }
                        SuggestionAction.EditTarget -> onEditTarget()
                    }
                },
                enabled = !(s.usesAi && busy != null),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(if (s.usesAi) Icons.Filled.AutoAwesome else Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(s.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (s.usesAi) Tag("AI", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- errors

@Composable
fun AiErrorCard(error: AiError, detail: String?, onRetry: () -> Unit, onSettings: () -> Unit) {
    val keyProblem = error == AiError.NO_KEY || error == AiError.BAD_KEY
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(error.message, style = MaterialTheme.typography.bodyMedium)
            if (error == AiError.NO_KEY)
                Text("It is free: get a key at build.nvidia.com, open Settings and paste it there.", style = MaterialTheme.typography.bodySmall)
            if (error == AiError.OTHER && !detail.isNullOrBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (keyProblem) Button(onClick = onSettings, shape = MaterialTheme.shapes.small) { Text("Open Settings") }
                if (error != AiError.NO_KEY) OutlinedButton(onClick = onRetry, shape = MaterialTheme.shapes.small) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- preview before applying

@Composable
fun AiProposalSheet(vm: ResumeViewModel) {
    val p by vm.proposal.collectAsState()
    val prop = p ?: return
    val resume by vm.draft.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { vm.closeProposal() }, sheetState = sheetState) {
        ProposalContent(vm, prop, resume)
    }
}

@Composable
private fun ProposalContent(vm: ResumeViewModel, prop: AiProposal, resume: Resume?) {
    val kind = prop.request.kind
    val clipboard = LocalClipboardManager.current
    val have = remember(resume) { resume?.skills?.map { it.name.lowercase() }?.toSet() ?: emptySet() }
    val skillOptions = remember(prop.text, prop.loading) {
        if (kind == ProposalKind.SKILLS && !prop.loading) vm.proposalSkills(prop.text).filter { it.lowercase() !in have } else emptyList()
    }
    var chosen by remember(skillOptions) { mutableStateOf(skillOptions.toSet()) }

    val textKind = kind == ProposalKind.SUMMARY || kind == ProposalKind.BULLETS
    val ready = !prop.loading && prop.text.isNotBlank()
    val placeholders = ready && textKind && AiGuard.hasPlaceholders(prop.text)
    val newNumbers = if (ready && textKind && resume != null) AiGuard.newNumbers(prop.text, resume) else emptyList()
    var confirmed by remember(newNumbers) { mutableStateOf(false) }
    val canUse = ready && prop.error == null && when (kind) {
        ProposalKind.SKILLS -> chosen.isNotEmpty()
        ProposalKind.ADVICE -> false
        else -> !placeholders && (newNumbers.isEmpty() || confirmed)
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()
            .padding(horizontal = 20.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(prop.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f, fill = false))
            Tag("AI draft", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        }

        if (textKind && prop.before.isNotBlank()) {
            Text("Your current text", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
                Text(prop.before, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }

        if (prop.loading && prop.text.isBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Text(
                    when (kind) {
                        ProposalKind.SUMMARY -> "Writing your summary..."
                        ProposalKind.BULLETS -> "Writing your bullets..."
                        ProposalKind.SKILLS -> "Finding skills..."
                        ProposalKind.ADVICE -> "Preparing fixes..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (prop.loading) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface) {
                Text(prop.text, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        } else if (kind == ProposalKind.SKILLS && skillOptions.isNotEmpty()) {
            Text("Tap the skills that are true for you", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                skillOptions.forEach { s ->
                    FilterChip(
                        selected = s in chosen,
                        onClick = { chosen = if (s in chosen) chosen - s else chosen + s },
                        label = { Text(s) },
                    )
                }
            }
        } else if (kind == ProposalKind.ADVICE && prop.text.isNotBlank()) {
            SelectionContainer {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface) {
                    Text(prop.text, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("Copy a fix and paste it into the right place in your resume.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (prop.text.isNotBlank()) {
            Field("Suggested text (you can edit it)", prop.text, { vm.editProposalText(it) }, singleLine = false, minLines = 4)
        } else if (kind == ProposalKind.SKILLS && ready) {
            Text("Nothing new to add. You already have these skills.", style = MaterialTheme.typography.bodyMedium)
        }

        if (prop.error != null) {
            AiErrorCard(prop.error, prop.errorDetail, onRetry = { vm.retryProposal() }, onSettings = { vm.closeProposal(); vm.go(Screen.SETTINGS) })
            if (prop.text.isNotBlank()) Text("Your last result is kept above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (placeholders) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                Text(
                    "Replace each [bracket] with your real number before using this text. The AI cannot know your real results.",
                    Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                )
            }
        } else if (newNumbers.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "These numbers are not in your resume yet: ${newNumbers.take(6).joinToString(", ")}. The AI cannot know your real results.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                        Text("These numbers are true", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (textKind && ready) {
            Text("Change it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tweaks = buildList {
                    add(Tweak.SHORTER); add(Tweak.STRONGER); add(Tweak.FORMAL)
                    if (kind == ProposalKind.BULLETS) add(Tweak.NUMBERS)
                }
                tweaks.forEach { t ->
                    AssistChip(onClick = { vm.tweakProposal(t) }, label = { Text(t.label) })
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.closeProposal() }) { Text(if (kind == ProposalKind.ADVICE) "Close" else "Cancel") }
            OutlinedButton(
                onClick = { vm.retryProposal() }, enabled = !prop.loading, shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f),
            ) { Text(if (prop.error != null) "Retry" else "Try again") }
            if (kind == ProposalKind.ADVICE) {
                Button(
                    onClick = { clipboard.setText(AnnotatedString(prop.text)); vm.say("Copied.") },
                    enabled = ready, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f),
                ) { Text("Copy") }
            } else {
                Button(
                    onClick = { vm.applyProposal(if (kind == ProposalKind.SKILLS) skillOptions.filter { it in chosen } else null) },
                    enabled = canUse, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f),
                ) { Text("Use this") }
            }
        }
    }
}

// ---------------------------------------------------------------- chat helper

private fun chatPrompts(tab: Int, guided: Boolean): List<String> =
    if (guided) listOf("What should I write in this step?", "Give me an example", "What is the most important part?")
    else when (tab) {
        0 -> listOf("Is my job title clear?", "What contact details should I add?")
        1 -> listOf("Make my summary shorter", "Does my summary fit my target job?")
        2 -> listOf("Make my second job sound stronger", "Which bullets need numbers?")
        3 -> listOf("How should I list my degree?", "Should I add my grades?")
        4 -> listOf("Which skills am I missing for my target job?", "Which skills should I remove?")
        5 -> listOf("How do I describe my project?", "Is a project worth adding?")
        6 -> listOf("What else could I add?", "Are languages worth listing?")
        7 -> listOf("Which template fits my job?", "How do I fit it on one page?")
        else -> listOf("What should I fix first?", "Rate my resume for my target job")
    }

@Composable
fun AiChatSheet(vm: ResumeViewModel) {
    val show by vm.showChat.collectAsState()
    if (!show) return
    val chat by vm.chat.collectAsState()
    val stream by vm.chatStream.collectAsState()
    val err by vm.chatError.collectAsState()
    val tab by vm.editorTab.collectAsState()
    val screen by vm.screen.collectAsState()
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val guided = screen == Screen.GUIDED
    val tabName = if (guided) "Guided setup" else EditorTabNames.getOrElse(tab) { "Basics" }
    val total = chat.size + (if (stream != null) 1 else 0) + (if (err != null) 1 else 0)

    LaunchedEffect(total, stream?.length) {
        if (total > 0) listState.scrollToItem(total - 1)
    }

    ModalBottomSheet(onDismissRequest = { vm.closeChat() }, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.85f).navigationBarsPadding().imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ask AI", style = MaterialTheme.typography.titleLarge)
                    Text("It knows you are on: $tabName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (chat.isNotEmpty()) TextButton(onClick = { vm.clearChat() }) { Text("Clear") }
            }

            if (chat.isEmpty() && stream == null) {
                Text("Try one of these, or type your own question.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    chatPrompts(tab, guided).forEach { q -> AssistChip(onClick = { vm.sendChat(q) }, label = { Text(q) }) }
                }
            }

            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chat) { m ->
                    val mine = m.role == "user"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(max = 320.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                SelectionContainer { Text(m.content, style = MaterialTheme.typography.bodyMedium) }
                                if (!mine) TextButton(onClick = { clipboard.setText(AnnotatedString(m.content)); vm.say("Copied.") }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy")
                                }
                            }
                        }
                    }
                }
                stream?.let { live ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 320.dp),
                        ) {
                            if (live.isBlank()) Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Thinking...", style = MaterialTheme.typography.bodyMedium)
                            } else Text(live, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                err?.let { e ->
                    item {
                        AiErrorCard(e, null, onRetry = { vm.retryChat() }, onSettings = { vm.closeChat(); vm.go(Screen.SETTINGS) })
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Ask about your resume", input, { input = it }, Modifier.weight(1f))
                if (stream != null) {
                    FilledIconButton(onClick = { vm.stopChat() }, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Stop, contentDescription = "Stop") }
                } else {
                    FilledIconButton(
                        onClick = { vm.sendChat(input); input = "" },
                        enabled = input.isNotBlank(), modifier = Modifier.size(52.dp),
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- first-launch setup message

@Composable
fun AiSetupDialog(vm: ResumeViewModel) {
    val show by vm.showAiSetup.collectAsState()
    if (!show) return
    AlertDialog(
        onDismissRequest = { vm.dismissAiSetup() },
        title = { Text("Turn on AI help") },
        text = {
            Text(
                "AI writing help is off because no NVIDIA key is set.\n\n" +
                    "1. Get a free key at build.nvidia.com (it starts with nvapi-).\n" +
                    "2. Open Settings and paste it.\n\n" +
                    "Everything else in the app works without AI.",
            )
        },
        confirmButton = { TextButton(onClick = { vm.dismissAiSetup(); vm.go(Screen.SETTINGS) }) { Text("Open Settings") } },
        dismissButton = { TextButton(onClick = { vm.dismissAiSetup() }) { Text("Not now") } },
    )
}
