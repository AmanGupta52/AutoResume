@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.*

private const val LAST_STEP = 6

private val StepTitles = listOf(
    "About you" to "Recruiters need to reach you. Add the basics.",
    "The job you want" to "Everything the AI writes will be matched to this job.",
    "Your summary" to "Two to four sentences: who you are and what you are best at.",
    "Your latest job" to "Start with the most recent. You can add older jobs later.",
    "Your education" to "School, college or course. Ongoing studies count too.",
    "Your skills" to "List 6 to 12 skills that match the job you want.",
    "All set" to "Your first version is ready.",
)

private fun isBlankJob(e: ExperienceEntry) = e.role.isBlank() && e.company.isBlank() && e.description.isBlank()
private fun isBlankEdu(e: EducationEntry) = e.institution.isBlank() && e.degree.isBlank()

/** One question per screen. The normal tabbed editor stays available for later changes. */
@Composable
fun GuidedScreen(vm: ResumeViewModel) {
    val r by vm.draft.collectAsState()
    val resume = r ?: return
    val step by vm.guidedStep.collectAsState()
    val aiOn by vm.aiAvailableFlow.collectAsState()
    val busy by vm.aiBusy.collectAsState()

    // make sure the entry being edited exists, and drop empty ones when leaving
    LaunchedEffect(step) {
        if (step == 3 && vm.draft.value?.experience?.isEmpty() == true) vm.update { it.copy(experience = listOf(ExperienceEntry())) }
        if (step == 4 && vm.draft.value?.education?.isEmpty() == true) vm.update { it.copy(education = listOf(EducationEntry())) }
        if (step == LAST_STEP) {
            vm.update { cur ->
                cur.copy(
                    experience = cur.experience.filterNot { isBlankJob(it) },
                    education = cur.education.filterNot { isBlankEdu(it) },
                )
            }
            vm.applySmartTemplateIfDefault()
        }
    }

    fun goTo(n: Int) {
        val t = TargetJob.parse(resume)
        if (step == 1 && resume.jobTitle.isBlank() && t.title.isNotBlank()) vm.update { it.copy(jobTitle = t.title) }
        vm.guidedStep.value = n.coerceIn(0, LAST_STEP)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step == 0) vm.go(Screen.EDITOR) else goTo(step - 1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("Step ${step + 1} of ${LAST_STEP + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (aiOn) IconButton(onClick = { vm.openChat() }) { Icon(Icons.Filled.AutoAwesome, contentDescription = "Ask AI") }
            TextButton(onClick = { vm.go(Screen.EDITOR) }) { Text("Skip setup") }
        }
        LinearProgressIndicator(
            progress = { (step + 1) / (LAST_STEP + 1f) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        )

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(StepTitles[step].first, style = MaterialTheme.typography.headlineSmall)
            Text(StepTitles[step].second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (step) {
                0 -> {
                    Field("Full name", resume.fullName, { v -> vm.update { it.copy(fullName = v) } })
                    Field("Email", resume.email, { v -> vm.update { it.copy(email = v) } }, keyboard = KeyboardType.Email)
                    Field("Phone", resume.phone, { v -> vm.update { it.copy(phone = v) } }, keyboard = KeyboardType.Phone)
                    Field("Location", resume.location, { v -> vm.update { it.copy(location = v) } }, placeholder = "City, Country")
                }
                1 -> {
                    val t = TargetJob.parse(resume)
                    Field("Job title", t.title, { v -> vm.setTarget(t.copy(title = v)) }, placeholder = "e.g. Marketing Manager")
                    Field("Job description (optional)", t.description, { v -> vm.setTarget(t.copy(description = v)) },
                        singleLine = false, minLines = 6, placeholder = "Paste the job post to get matching keywords and skills")
                }
                2 -> {
                    Field("Professional summary", resume.summary, { v -> vm.update { it.copy(summary = v) } }, singleLine = false, minLines = 6)
                    if (aiOn) FilledTonalButton(
                        onClick = { vm.aiSummary() }, enabled = busy == null,
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy == "summary") "Writing your summary..." else "Write it for me")
                    }
                }
                3 -> {
                    val e = resume.experience.firstOrNull() ?: ExperienceEntry()
                    fun upd(n: ExperienceEntry) = vm.update { cur ->
                        cur.copy(experience = if (cur.experience.isEmpty()) listOf(n) else cur.experience.toMutableList().also { it[0] = n })
                    }
                    Field("Job title", e.role, { upd(e.copy(role = it)) })
                    Field("Company", e.company, { upd(e.copy(company = it)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field("Start", e.startDate, { upd(e.copy(startDate = it)) }, Modifier.weight(1f), placeholder = "Jun 2022")
                        if (!e.isCurrent) Field("End", e.endDate, { upd(e.copy(endDate = it)) }, Modifier.weight(1f), placeholder = "Aug 2024")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = e.isCurrent, onCheckedChange = { upd(e.copy(isCurrent = it)) })
                        Text("I currently work here")
                    }
                    Field("What did you achieve? (one per line)", e.description, { upd(e.copy(description = it)) }, singleLine = false, minLines = 5,
                        placeholder = "Start with a verb and add a number: Cut report time by 40%")
                    if (aiOn) FilledTonalButton(
                        onClick = { vm.aiBullets(0) }, enabled = busy == null && e.role.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy == "exp0") "Writing your bullets..." else "Suggest bullets")
                    }
                }
                4 -> {
                    val e = resume.education.firstOrNull() ?: EducationEntry()
                    fun upd(n: EducationEntry) = vm.update { cur ->
                        cur.copy(education = if (cur.education.isEmpty()) listOf(n) else cur.education.toMutableList().also { it[0] = n })
                    }
                    Field("School", e.institution, { upd(e.copy(institution = it)) })
                    Field("Degree / course", e.degree, { upd(e.copy(degree = it)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field("From", e.startYear, { upd(e.copy(startYear = it)) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
                        Field("To", e.endYear, { upd(e.copy(endYear = it)) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
                    }
                }
                5 -> GuidedSkills(vm, resume, aiOn, busy)
                else -> {
                    val (strength, next) = vm.strength(resume)
                    SoftCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Resume strength $strength%", style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(
                                progress = { strength / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            )
                            Text(next ?: "Looking complete. Check the preview and the writing check.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val t = TargetJob.parse(resume)
                    val (name, why) = ResumeCoach.templateFor(t.title.ifBlank { resume.jobTitle }, t.description)
                    SoftCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Template: ${ResumeHtmlHelper.normalizeTemplate(resume.templateId)}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (ResumeHtmlHelper.normalizeTemplate(resume.templateId) == name) "Chosen for your job. $why"
                                else "You can change it in the Design tab.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step < LAST_STEP) {
                    if (step > 0) OutlinedButton(onClick = { goTo(step - 1) }, shape = MaterialTheme.shapes.small, modifier = Modifier.height(50.dp)) { Text("Back") }
                    TextButton(onClick = { goTo(step + 1) }) { Text("Skip") }
                    Button(onClick = { goTo(step + 1) }, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f).height(50.dp)) { Text("Next") }
                } else {
                    OutlinedButton(onClick = { vm.go(Screen.EDITOR) }, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f).height(50.dp)) { Text("Edit more") }
                    Button(onClick = { vm.go(Screen.PREVIEW) }, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f).height(50.dp)) { Text("See my resume") }
                }
            }
        }
    }
}

@Composable
private fun GuidedSkills(vm: ResumeViewModel, r: Resume, aiOn: Boolean, busy: String?) {
    var input by remember { mutableStateOf("") }
    fun add() {
        val names = input.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (names.isNotEmpty()) vm.update { cur ->
            val have = cur.skills.map { it.name.lowercase() }.toSet()
            cur.copy(skills = cur.skills + names.filter { it.lowercase() !in have }.map { SkillEntry(it, "Intermediate", "Technical") })
        }
        input = ""
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Field("Add skills", input, { input = it }, Modifier.weight(1f), placeholder = "Separate with commas")
        FilledIconButton(onClick = { add() }, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Add, contentDescription = "Add") }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        r.skills.forEachIndexed { i, s ->
            InputChip(
                selected = false,
                onClick = { vm.update { cur -> cur.copy(skills = cur.skills.toMutableList().also { if (i in it.indices) it.removeAt(i) }) } },
                label = { Text(s.name) },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove ${s.name}", modifier = Modifier.size(16.dp)) },
            )
        }
    }
    if (aiOn) FilledTonalButton(
        onClick = { vm.aiSkills() }, enabled = busy == null,
        modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (busy == "skills") "Finding skills..." else "Suggest skills for my job")
    }
}
