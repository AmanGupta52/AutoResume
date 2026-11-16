@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.example.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.theme.ResumeAccents

private fun <T> List<T>.replaceAt(i: Int, v: T) = toMutableList().also { if (i in it.indices) it[i] = v }
private fun <T> List<T>.removeAtIdx(i: Int) = toMutableList().also { if (i in it.indices) it.removeAt(i) }

private val Tabs = listOf("Basics", "Summary", "Experience", "Education", "Skills", "Projects", "More", "Design", "Check")

private fun tabDone(i: Int, r: Resume): Boolean = when (i) {
    0 -> r.fullName.isNotBlank() && r.jobTitle.isNotBlank() && r.email.isNotBlank()
    1 -> r.summary.length >= 60
    2 -> r.experience.isNotEmpty() && r.experience.all { it.description.length >= 40 }
    3 -> r.education.isNotEmpty()
    4 -> r.skills.size >= 5
    5 -> r.projects.isNotEmpty()
    6 -> r.certifications.isNotEmpty() || r.languages.isNotEmpty() || r.achievements.isNotBlank()
    else -> false
}

@Composable
fun EditorScreen(vm: ResumeViewModel) {
    val r by vm.draft.collectAsState()
    val resume = r ?: return
    val pager = rememberPagerState { Tabs.size }
    val scope = rememberCoroutineScope()
    val (strength, nextStep) = vm.strength(resume)
    val ok = Color(0xFF16A34A)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.closeEditor() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Column(Modifier.weight(1f)) {
                    Text(resume.name.ifBlank { "Untitled resume" }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("Saved automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { strength / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                Text(
                    if (nextStep == null) "Resume strength $strength% - looking complete" else "Resume strength $strength% - $nextStep",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ScrollableTabRow(
                selectedTabIndex = pager.currentPage, edgePadding = 12.dp, containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary, divider = {},
            ) {
                Tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = pager.currentPage == i,
                        onClick = { scope.launch { pager.animateScrollToPage(i) } },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (tabDone(i, resume)) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Section complete", tint = ok, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(t)
                            }
                        },
                    )
                }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (page) {
                        0 -> BasicsTab(vm, resume)
                        1 -> SummaryTab(vm, resume)
                        2 -> ExperienceTab(vm, resume)
                        3 -> EducationTab(vm, resume)
                        4 -> SkillsTab(vm, resume)
                        5 -> ProjectsTab(vm, resume)
                        6 -> MoreTab(vm, resume)
                        7 -> DesignTab(vm, resume)
                        else -> CheckTab(vm, resume)
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { vm.go(Screen.PREVIEW) },
            icon = { Icon(Icons.Filled.Visibility, contentDescription = null) },
            text = { Text("Preview") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

@Composable
private fun EntryCard(title: String, onRemove: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) }
            }
            content()
        }
    }
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

// ---------------- Basics ----------------
@Composable
private fun BasicsTab(vm: ResumeViewModel, r: Resume) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { vm.setAvatar(it) } }
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val bmp = remember(r.avatarUri) {
                if (r.avatarUri.isBlank() || r.avatarUri == "initials") null
                else runCatching { BitmapFactory.decodeFile(Uri.parse(r.avatarUri).path) }.getOrNull()?.asImageBitmap()
            }
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (bmp != null) Image(bmp, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Profile photo (optional)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { picker.launch("image/*") }, shape = MaterialTheme.shapes.small) { Text("Choose") }
                    if (bmp != null) TextButton(onClick = { vm.update { it.copy(avatarUri = "") } }) { Text("Remove") }
                }
                Text("Many ATS systems ignore photos. Leave empty for the safest result.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Field("Full name", r.fullName, { v -> vm.update { it.copy(fullName = v) } })
    Field("Job title", r.jobTitle, { v -> vm.update { it.copy(jobTitle = v) } })
    Field("Email", r.email, { v -> vm.update { it.copy(email = v) } }, keyboard = KeyboardType.Email)
    Field("Phone", r.phone, { v -> vm.update { it.copy(phone = v) } }, keyboard = KeyboardType.Phone)
    Field("Location", r.location, { v -> vm.update { it.copy(location = v) } }, placeholder = "City, Country")
    Field("LinkedIn", r.linkedinUrl, { v -> vm.update { it.copy(linkedinUrl = v) } }, keyboard = KeyboardType.Uri)
    Field("GitHub", r.githubUrl, { v -> vm.update { it.copy(githubUrl = v) } }, keyboard = KeyboardType.Uri)
    Field("Website / portfolio", r.portfolioUrl, { v -> vm.update { it.copy(portfolioUrl = v) } }, keyboard = KeyboardType.Uri)
}

// ---------------- Summary ----------------
@Composable
private fun SummaryTab(vm: ResumeViewModel, r: Resume) {
    val busy by vm.aiBusy.collectAsState()
    Field("Professional summary", r.summary, { v -> vm.update { it.copy(summary = v) } }, singleLine = false, minLines = 7,
        placeholder = "2-4 sentences: who you are, what you are best at, one measurable result.")
    Text("${r.summary.length} characters. 200-400 reads best.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FilledTonalButton(onClick = { vm.aiSummary() }, enabled = busy == null, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (busy == "summary") "Writing..." else "Write it for me")
    }
}

// ---------------- Experience ----------------
@Composable
private fun ExperienceTab(vm: ResumeViewModel, r: Resume) {
    val busy by vm.aiBusy.collectAsState()
    r.experience.forEachIndexed { i, e ->
        fun upd(n: ExperienceEntry) = vm.update { it.copy(experience = it.experience.replaceAt(i, n)) }
        EntryCard("Job ${i + 1}", onRemove = { vm.removeEntry("Job") { it.copy(experience = it.experience.removeAtIdx(i)) } }) {
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
            Field("Achievements (one per line)", e.description, { upd(e.copy(description = it)) }, singleLine = false, minLines = 5,
                placeholder = "Start with a verb and add a number: Cut report time by 40%")
            TextButton(onClick = { vm.aiBullets(i) }, enabled = busy == null && e.role.isNotBlank()) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (busy == "exp$i") "Writing..." else "Suggest bullets")
            }
        }
    }
    AddButton("Add a job") { vm.update { it.copy(experience = it.experience + ExperienceEntry()) } }
}

// ---------------- Education ----------------
@Composable
private fun EducationTab(vm: ResumeViewModel, r: Resume) {
    r.education.forEachIndexed { i, e ->
        fun upd(n: EducationEntry) = vm.update { it.copy(education = it.education.replaceAt(i, n)) }
        EntryCard("Education ${i + 1}", onRemove = { vm.removeEntry("Education") { it.copy(education = it.education.removeAtIdx(i)) } }) {
            Field("School", e.institution, { upd(e.copy(institution = it)) })
            Field("Degree / course", e.degree, { upd(e.copy(degree = it)) })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("From", e.startYear, { upd(e.copy(startYear = it)) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
                Field("To", e.endYear, { upd(e.copy(endYear = it)) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
            }
            Field("Grade (optional)", e.grade, { upd(e.copy(grade = it)) })
            Field("Highlights (optional)", e.achievements, { upd(e.copy(achievements = it)) }, singleLine = false, minLines = 2)
        }
    }
    AddButton("Add education") { vm.update { it.copy(education = it.education + EducationEntry()) } }
}

// ---------------- Skills ----------------
@Composable
private fun SkillsTab(vm: ResumeViewModel, r: Resume) {
    val busy by vm.aiBusy.collectAsState()
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
    if (r.skills.isEmpty()) {
        Text("No skills yet. Aim for 6-12 that match the jobs you want.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            r.skills.forEachIndexed { i, s ->
                InputChip(
                    selected = false, onClick = { vm.removeEntry("Skill") { it.copy(skills = it.skills.removeAtIdx(i)) } },
                    label = { Text(s.name) },
                    trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove ${s.name}", modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
    FilledTonalButton(onClick = { vm.aiSkills() }, enabled = busy == null, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.small) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (busy == "skills") "Finding skills..." else "Suggest skills for my job title")
    }
}

// ---------------- Projects ----------------
@Composable
private fun ProjectsTab(vm: ResumeViewModel, r: Resume) {
    r.projects.forEachIndexed { i, p ->
        fun upd(n: ProjectEntry) = vm.update { it.copy(projects = it.projects.replaceAt(i, n)) }
        EntryCard("Project ${i + 1}", onRemove = { vm.removeEntry("Project") { it.copy(projects = it.projects.removeAtIdx(i)) } }) {
            Field("Project name", p.title, { upd(p.copy(title = it)) })
            Field("Tools used", p.techStack, { upd(p.copy(techStack = it)) })
            Field("What it does / your result", p.description, { upd(p.copy(description = it)) }, singleLine = false, minLines = 3)
            Field("Code link", p.githubLink, { upd(p.copy(githubLink = it)) }, keyboard = KeyboardType.Uri)
            Field("Live link", p.liveLink, { upd(p.copy(liveLink = it)) }, keyboard = KeyboardType.Uri)
        }
    }
    AddButton("Add a project") { vm.update { it.copy(projects = it.projects + ProjectEntry()) } }
}

// ---------------- More ----------------
@Composable
private fun MoreTab(vm: ResumeViewModel, r: Resume) {
    SectionTitle("Certifications")
    r.certifications.forEachIndexed { i, c ->
        fun upd(n: CertificationEntry) = vm.update { it.copy(certifications = it.certifications.replaceAt(i, n)) }
        EntryCard("Certification ${i + 1}", onRemove = { vm.removeEntry("Certification") { it.copy(certifications = it.certifications.removeAtIdx(i)) } }) {
            Field("Name", c.name, { upd(c.copy(name = it)) })
            Field("Issued by", c.issuer, { upd(c.copy(issuer = it)) })
            Field("Date", c.date, { upd(c.copy(date = it)) })
        }
    }
    AddButton("Add certification") { vm.update { it.copy(certifications = it.certifications + CertificationEntry()) } }

    SectionTitle("Languages")
    r.languages.forEachIndexed { i, l ->
        fun upd(n: LanguageEntry) = vm.update { it.copy(languages = it.languages.replaceAt(i, n)) }
        EntryCard("Language ${i + 1}", onRemove = { vm.removeEntry("Language") { it.copy(languages = it.languages.removeAtIdx(i)) } }) {
            Field("Language", l.name, { upd(l.copy(name = it)) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Native", "Fluent", "Intermediate", "Basic").forEach { p ->
                    FilterChip(selected = l.proficiency == p, onClick = { upd(l.copy(proficiency = p)) }, label = { Text(p) })
                }
            }
        }
    }
    AddButton("Add language") { vm.update { it.copy(languages = it.languages + LanguageEntry()) } }

    SectionTitle("Awards, volunteering, interests")
    Field("Anything else worth a line", r.achievements, { v -> vm.update { it.copy(achievements = v) } }, singleLine = false, minLines = 4)
}

// ---------------- Design ----------------
@Composable
private fun DesignTab(vm: ResumeViewModel, r: Resume) {
    val accent = ResumeAccents.of(r.colorSchemeId)
    val current = ResumeHtmlHelper.normalizeTemplate(r.templateId)
    val opts = DesignOptions.parse(r)
    SectionTitle("Template")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(TemplateNames) { name ->
            val sel = name == current
            SoftCard(Modifier.width(150.dp), onClick = { vm.update { it.copy(templateId = name) } }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemplateThumb(
                        name, accent,
                        Modifier.fillMaxWidth().height(150.dp).border(
                            if (sel) 2.dp else 1.dp,
                            if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(6.dp),
                        ),
                    )
                    Text(name, style = MaterialTheme.typography.labelLarge, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Text(TemplateBlurbs[name].orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    SectionTitle("Accent colour")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ResumeAccents.all.forEach { (id, c) ->
            Swatch(c, r.colorSchemeId == id, id) { vm.update { it.copy(colorSchemeId = id) } }
        }
    }

    SectionTitle("Font")
    Segmented(DesignOptions.FONTS, opts.font) { v -> vm.updateDesign { it.copy(font = v) } }
    SectionTitle("Text size")
    Segmented(listOf("Compact", "Standard", "Large"), opts.size) { v -> vm.updateDesign { it.copy(size = v) } }
    SectionTitle("Page margins")
    Segmented(listOf("Narrow", "Standard", "Wide"), r.margins) { v -> vm.update { it.copy(margins = v) } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Tighter line spacing", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = opts.tight, onCheckedChange = { v -> vm.updateDesign { it.copy(tight = v) } })
    }

    SectionTitle("Section order") {
        TextButton(onClick = { vm.updateDesign { it.copy(order = DesignOptions.DEFAULT_ORDER) } }) { Text("Reset") }
    }
    Text(
        "Applies to the newer templates (Compact, Academic, Classic, Bold Header, Timeline). On Reset each template uses its own best order.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp)) {
            opts.order.forEachIndexed { i, key ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(DesignOptions.SECTION_TITLES.getValue(key), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(
                        enabled = i > 0,
                        onClick = { vm.updateDesign { o -> o.copy(order = o.order.toMutableList().also { java.util.Collections.swap(it, i, i - 1) }) } },
                    ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move ${DesignOptions.SECTION_TITLES.getValue(key)} up") }
                    IconButton(
                        enabled = i < opts.order.lastIndex,
                        onClick = { vm.updateDesign { o -> o.copy(order = o.order.toMutableList().also { java.util.Collections.swap(it, i, i + 1) }) } },
                    ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move ${DesignOptions.SECTION_TITLES.getValue(key)} down") }
                }
            }
        }
    }
    Text("The preview and the PDF use exactly these settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------- Writing check ----------------
@Composable
private fun CheckTab(vm: ResumeViewModel, r: Resume) {
    val issues = remember(r) { vm.writingIssues(r) }
    if (issues.isEmpty()) {
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("No problems found", style = MaterialTheme.typography.titleMedium)
                Text("No weak phrases, misspellings or missing numbers detected. Read it once more out loud before you send it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    val high = issues.count { it.level == IssueLevel.HIGH }
    val med = issues.count { it.level == IssueLevel.MEDIUM }
    val low = issues.count { it.level == IssueLevel.LOW }
    Text("${issues.size} things to look at: $high important, $med medium, $low minor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    issues.groupBy { it.section }.forEach { (section, list) ->
        SectionTitle(section)
        list.forEach { i ->
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (i.level) {
                            IssueLevel.HIGH -> Tag("Important", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                            IssueLevel.MEDIUM -> Tag("Medium", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            IssueLevel.LOW -> Tag("Minor", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(i.where, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, fill = false))
                    }
                    Text(i.problem, style = MaterialTheme.typography.bodyMedium)
                    Text(i.fix, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            SegmentedButton(selected = selected == o, onClick = { onSelect(o) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) { Text(o) }
        }
    }
}
