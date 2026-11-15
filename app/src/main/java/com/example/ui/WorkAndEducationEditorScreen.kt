package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EducationEntry
import com.example.data.ExperienceEntry
import com.example.ui.locale.Text
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.delay

enum class WorkEduTab {
    ALL, WORK, EDUCATION
}

@Composable
fun WorkAndEducationEditorScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onGoToPreview: () -> Unit
) {
    val activeResume by viewModel.activeResume.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiMessageFeedback by viewModel.aiMessageFeedback.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    var selectedTab by remember { mutableStateOf(WorkEduTab.ALL) }
    var showSavedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(showSavedBanner) {
        if (showSavedBanner) {
            delay(2000)
            showSavedBanner = false
        }
    }

    BackHandler {
        viewModel.saveActiveResume()
        onNavigateBack()
    }

    val accentBlue = AppTheme.primary
    val accentPurple = AppTheme.accent
    val successGreen = Color(0xFF30D158)

    Scaffold(
        containerColor = AppTheme.background,
        topBar = {
            Surface(
                color = AppTheme.card,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.saveActiveResume()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("work_edu_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AppTheme.text
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeResume.name.ifBlank { "Resume Draft" },
                                color = AppTheme.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Work Experience & Education Editor",
                                color = AppTheme.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.saveActiveResume()
                                onGoToPreview()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("work_edu_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Segmented Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.background)
                            .border(1.dp, AppTheme.border, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EditorSectionTab(
                            label = "All Sections",
                            selected = selectedTab == WorkEduTab.ALL,
                            accentColor = accentBlue,
                            onClick = { selectedTab = WorkEduTab.ALL },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_all_sections")
                        )
                        EditorSectionTab(
                            label = "Work (${activeResume.experience.size})",
                            selected = selectedTab == WorkEduTab.WORK,
                            accentColor = accentBlue,
                            onClick = { selectedTab = WorkEduTab.WORK },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_work_experience")
                        )
                        EditorSectionTab(
                            label = "Education (${activeResume.education.size})",
                            selected = selectedTab == WorkEduTab.EDUCATION,
                            accentColor = accentPurple,
                            onClick = { selectedTab = WorkEduTab.EDUCATION },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_education")
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = AppTheme.card,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeResume.experience.size} Work • ${activeResume.education.size} Education",
                            color = AppTheme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (showSavedBanner) "Saved to resume draft" else "Auto-synced on save",
                            color = if (showSavedBanner) successGreen else AppTheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.saveActiveResume()
                            showSavedBanner = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.testTag("work_edu_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = AppTheme.text,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", color = AppTheme.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            viewModel.saveActiveResume()
                            onGoToPreview()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("work_edu_done_button")
                    ) {
                        Text("Save & View", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AnimatedVisibility(visible = isAiLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentBlue.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accentBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = aiMessageFeedback.ifBlank { "Generating resume bullet points..." },
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showSavedBanner) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = successGreen.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, successGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = successGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Work experience and education details saved.",
                            color = AppTheme.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (selectedTab == WorkEduTab.ALL || selectedTab == WorkEduTab.WORK) {
                WorkExperienceEditorSection(viewModel = viewModel)
            }

            if (selectedTab == WorkEduTab.ALL) {
                HorizontalDivider(color = AppTheme.border)
            }

            if (selectedTab == WorkEduTab.ALL || selectedTab == WorkEduTab.EDUCATION) {
                EducationEditorSection(viewModel = viewModel)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EditorSectionTab(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else AppTheme.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WorkExperienceEditorSection(viewModel: MainViewModel) {
    val resume by viewModel.activeResume.collectAsState()
    var expandedIndex by remember(resume.id) { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = AppTheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Work Experience",
                        color = AppTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Roles, dates, and structured bullet points",
                        color = AppTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = {
                    val newList = resume.experience.toMutableList()
                    newList.add(
                        ExperienceEntry(
                            company = "",
                            role = "",
                            startDate = "",
                            endDate = "",
                            isCurrent = false,
                            description = "• "
                        )
                    )
                    viewModel.activeResume.value = resume.copy(experience = newList)
                    expandedIndex = newList.lastIndex
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_work_experience_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Job", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (resume.experience.isEmpty()) {
            EditorEmptyStateCard(
                icon = Icons.Default.Work,
                title = "No work experience added yet",
                subtitle = "Add internships, full-time roles, or freelance work with quantified impact bullets.",
                buttonLabel = "Add First Position",
                accentColor = AppTheme.primary,
                onClick = {
                    viewModel.activeResume.value = resume.copy(
                        experience = listOf(
                            ExperienceEntry(
                                company = "",
                                role = "",
                                startDate = "",
                                endDate = "",
                                isCurrent = false,
                                description = "• "
                            )
                        )
                    )
                    expandedIndex = 0
                }
            )
        } else {
            resume.experience.forEachIndexed { index, exp ->
                val isExpanded = expandedIndex == index
                WorkExperienceEditorCard(
                    index = index,
                    totalCount = resume.experience.size,
                    entry = exp,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedIndex = if (isExpanded) -1 else index
                    },
                    onUpdate = { updated ->
                        val newList = resume.experience.toMutableList()
                        newList[index] = updated
                        viewModel.activeResume.value = resume.copy(experience = newList)
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val newList = resume.experience.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index - 1, item)
                            viewModel.activeResume.value = resume.copy(experience = newList)
                            expandedIndex = index - 1
                        }
                    },
                    onMoveDown = {
                        if (index < resume.experience.lastIndex) {
                            val newList = resume.experience.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index + 1, item)
                            viewModel.activeResume.value = resume.copy(experience = newList)
                            expandedIndex = index + 1
                        }
                    },
                    onDelete = {
                        val newList = resume.experience.toMutableList()
                        newList.removeAt(index)
                        viewModel.activeResume.value = resume.copy(experience = newList)
                        if (expandedIndex == index) expandedIndex = (index - 1).coerceAtLeast(0)
                    },
                    onGenerateAiBullets = { prompt ->
                        viewModel.triggerAiExpBulletSuggester(index, prompt)
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkExperienceEditorCard(
    index: Int,
    totalCount: Int,
    entry: ExperienceEntry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (ExperienceEntry) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onGenerateAiBullets: (String) -> Unit
) {
    var aiPromptDetails by remember(index) { mutableStateOf("") }
    val fieldColors = editorOutlinedTextFieldColors(AppTheme.primary)

    val titleSummary = when {
        entry.role.isNotBlank() && entry.company.isNotBlank() -> "${entry.role} at ${entry.company}"
        entry.role.isNotBlank() -> entry.role
        entry.company.isNotBlank() -> entry.company
        else -> "Position #${index + 1}"
    }
    val dateSummary = listOf(entry.startDate, if (entry.isCurrent) "Present" else entry.endDate)
        .filter { it.isNotBlank() }
        .joinToString(" – ")

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) AppTheme.primary.copy(alpha = 0.55f) else AppTheme.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("work_entry_card_$index")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AppTheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = AppTheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleSummary,
                        color = AppTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateSummary.ifBlank { "Tap to edit dates and responsibilities" },
                        color = AppTheme.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (index > 0) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move work entry up",
                            tint = AppTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (index < totalCount - 1) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move work entry down",
                            tint = AppTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_work_entry_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete work entry",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = AppTheme.textSecondary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = entry.company,
                        onValueChange = { onUpdate(entry.copy(company = it)) },
                        label = { Text("Company / Organization", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Stripe, Google, Acme Labs", color = AppTheme.textSecondary, fontSize = 12.sp) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("work_company_input_$index")
                    )

                    OutlinedTextField(
                        value = entry.role,
                        onValueChange = { onUpdate(entry.copy(role = it)) },
                        label = { Text("Job Title / Role", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Senior Android Engineer", color = AppTheme.textSecondary, fontSize = 12.sp) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("work_role_input_$index")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val next = !entry.isCurrent
                                onUpdate(entry.copy(isCurrent = next, endDate = if (next) "Present" else ""))
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = entry.isCurrent,
                            onCheckedChange = { checked ->
                                onUpdate(entry.copy(isCurrent = checked, endDate = if (checked) "Present" else ""))
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF30D158))
                        )
                        Text(
                            text = "I currently work in this role",
                            color = AppTheme.text,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = entry.startDate,
                            onValueChange = { onUpdate(entry.copy(startDate = it)) },
                            label = { Text("Start Date", fontSize = 12.sp) },
                            placeholder = { Text("Jun 2022", color = AppTheme.textSecondary, fontSize = 12.sp) },
                            colors = fieldColors,
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("work_start_input_$index")
                        )
                        if (!entry.isCurrent) {
                            OutlinedTextField(
                                value = entry.endDate,
                                onValueChange = { onUpdate(entry.copy(endDate = it)) },
                                label = { Text("End Date", fontSize = 12.sp) },
                                placeholder = { Text("May 2024", color = AppTheme.textSecondary, fontSize = 12.sp) },
                                colors = fieldColors,
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("work_end_input_$index")
                            )
                        }
                    }

                    // Rich Text Editor for Responsibilities & Achievements
                    ResumeRichTextEditor(
                        title = "Responsibilities & Achievements Editor",
                        value = entry.description,
                        onValueChange = { onUpdate(entry.copy(description = it)) },
                        placeholder = "• Engineered Kotlin/Compose features reducing crash rate by 35%.\n• Led sprint planning across 4 engineers.",
                        accentColor = AppTheme.primary,
                        quickSnippets = listOf(
                            "Led ",
                            "Engineered ",
                            "Increased by %",
                            "Reduced latency by %",
                            "Collaborated with "
                        ),
                        testTagPrefix = "work_editor_$index"
                    )

                    // Per-entry AI Bullet Writer helper
                    Surface(
                        color = AppTheme.background,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Bullet Point Assistant",
                                    color = AppTheme.text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedTextField(
                                value = aiPromptDetails,
                                onValueChange = { aiPromptDetails = it },
                                placeholder = {
                                    Text(
                                        text = "Briefly describe your impact (e.g. built payment checkout, improved API latency 30%)",
                                        color = AppTheme.textSecondary,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = editorOutlinedTextFieldColors(Color(0xFFFF9500)),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("work_ai_prompt_$index")
                            )

                            Button(
                                onClick = {
                                    onGenerateAiBullets(aiPromptDetails)
                                    aiPromptDetails = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("work_ai_generate_button_$index")
                            ) {
                                Text("Craft Bullets with AI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EducationEditorSection(viewModel: MainViewModel) {
    val resume by viewModel.activeResume.collectAsState()
    var expandedIndex by remember(resume.id) { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = AppTheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Education Details",
                        color = AppTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Degrees, institutions, grades, and coursework",
                        color = AppTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = {
                    val newList = resume.education.toMutableList()
                    newList.add(
                        EducationEntry(
                            institution = "",
                            degree = "",
                            startYear = "",
                            endYear = "",
                            grade = "",
                            achievements = ""
                        )
                    )
                    viewModel.activeResume.value = resume.copy(education = newList)
                    expandedIndex = newList.lastIndex
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.accent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_education_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Edu", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (resume.education.isEmpty()) {
            EditorEmptyStateCard(
                icon = Icons.Default.School,
                title = "No education entries added yet",
                subtitle = "Include your university degree, graduation years, GPA, honors, and relevant coursework.",
                buttonLabel = "Add Education",
                accentColor = AppTheme.accent,
                onClick = {
                    viewModel.activeResume.value = resume.copy(
                        education = listOf(
                            EducationEntry(
                                institution = "",
                                degree = "",
                                startYear = "",
                                endYear = "",
                                grade = "",
                                achievements = ""
                            )
                        )
                    )
                    expandedIndex = 0
                }
            )
        } else {
            resume.education.forEachIndexed { index, edu ->
                val isExpanded = expandedIndex == index
                EducationEditorCard(
                    index = index,
                    totalCount = resume.education.size,
                    entry = edu,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedIndex = if (isExpanded) -1 else index
                    },
                    onUpdate = { updated ->
                        val newList = resume.education.toMutableList()
                        newList[index] = updated
                        viewModel.activeResume.value = resume.copy(education = newList)
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val newList = resume.education.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index - 1, item)
                            viewModel.activeResume.value = resume.copy(education = newList)
                            expandedIndex = index - 1
                        }
                    },
                    onMoveDown = {
                        if (index < resume.education.lastIndex) {
                            val newList = resume.education.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index + 1, item)
                            viewModel.activeResume.value = resume.copy(education = newList)
                            expandedIndex = index + 1
                        }
                    },
                    onDelete = {
                        val newList = resume.education.toMutableList()
                        newList.removeAt(index)
                        viewModel.activeResume.value = resume.copy(education = newList)
                        if (expandedIndex == index) expandedIndex = (index - 1).coerceAtLeast(0)
                    }
                )
            }
        }
    }
}

@Composable
private fun EducationEditorCard(
    index: Int,
    totalCount: Int,
    entry: EducationEntry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (EducationEntry) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val fieldColors = editorOutlinedTextFieldColors(AppTheme.accent)

    val titleSummary = when {
        entry.degree.isNotBlank() && entry.institution.isNotBlank() -> "${entry.degree} • ${entry.institution}"
        entry.degree.isNotBlank() -> entry.degree
        entry.institution.isNotBlank() -> entry.institution
        else -> "Education #${index + 1}"
    }
    val yearSummary = listOf(entry.startYear, entry.endYear)
        .filter { it.isNotBlank() }
        .joinToString(" – ")

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) AppTheme.accent.copy(alpha = 0.55f) else AppTheme.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("education_entry_card_$index")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AppTheme.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = AppTheme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleSummary,
                        color = AppTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(yearSummary, entry.grade).filter { it.isNotBlank() }.joinToString(" • ")
                            .ifBlank { "Tap to edit degree, years, and honors" },
                        color = AppTheme.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (index > 0) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move education entry up",
                            tint = AppTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (index < totalCount - 1) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move education entry down",
                            tint = AppTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_education_entry_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete education entry",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = AppTheme.textSecondary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = entry.institution,
                        onValueChange = { onUpdate(entry.copy(institution = it)) },
                        label = { Text("School / University", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Stanford University", color = AppTheme.textSecondary, fontSize = 12.sp) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edu_institution_input_$index")
                    )

                    OutlinedTextField(
                        value = entry.degree,
                        onValueChange = { onUpdate(entry.copy(degree = it)) },
                        label = { Text("Degree / Field of Study", fontSize = 12.sp) },
                        placeholder = { Text("e.g. B.S. Computer Science", color = AppTheme.textSecondary, fontSize = 12.sp) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edu_degree_input_$index")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = entry.startYear,
                            onValueChange = { onUpdate(entry.copy(startYear = it)) },
                            label = { Text("Start Year", fontSize = 12.sp) },
                            placeholder = { Text("2019", color = AppTheme.textSecondary, fontSize = 12.sp) },
                            colors = fieldColors,
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edu_start_year_input_$index")
                        )
                        OutlinedTextField(
                            value = entry.endYear,
                            onValueChange = { onUpdate(entry.copy(endYear = it)) },
                            label = { Text("End / Expected", fontSize = 12.sp) },
                            placeholder = { Text("2023", color = AppTheme.textSecondary, fontSize = 12.sp) },
                            colors = fieldColors,
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edu_end_year_input_$index")
                        )
                    }

                    OutlinedTextField(
                        value = entry.grade,
                        onValueChange = { onUpdate(entry.copy(grade = it)) },
                        label = { Text("Grade / CGPA (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 3.85 / 4.0 GPA", color = AppTheme.textSecondary, fontSize = 12.sp) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edu_grade_input_$index")
                    )

                    // Rich Text Editor for Honors, Coursework & Thesis
                    ResumeRichTextEditor(
                        title = "Honors, Coursework & Thesis Editor",
                        value = entry.achievements,
                        onValueChange = { onUpdate(entry.copy(achievements = it)) },
                        placeholder = "• Dean's List (6 semesters)\n• Relevant Coursework: Distributed Systems, Machine Learning\n• Thesis: On-device Neural Compression",
                        accentColor = AppTheme.accent,
                        quickSnippets = listOf(
                            "Dean's List",
                            "Relevant Coursework: ",
                            "Summa Cum Laude",
                            "Thesis: ",
                            "Club Leadership: "
                        ),
                        testTagPrefix = "edu_editor_$index"
                    )
                }
            }
        }
    }
}

/**
 * Reusable structured text editor with a formatting toolbar (bullet insertion, line formatting,
 * quick snippet chips) and live bullet/character metrics for resume sections.
 */
@Composable
fun ResumeRichTextEditor(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accentColor: Color,
    quickSnippets: List<String>,
    testTagPrefix: String
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Keep internal TextFieldValue synchronized when external state (e.g. AI generation) changes `value`.
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    val lines = remember(value) {
        value.lines().filter { it.isNotBlank() }
    }
    val bulletCount = remember(lines) {
        lines.count { it.trimStart().startsWith("•") || it.trimStart().startsWith("-") }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.background)
            .border(1.dp, AppTheme.border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = AppTheme.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$bulletCount bullets • ${value.length} chars",
                color = AppTheme.textSecondary,
                fontSize = 11.sp
            )
        }

        // Formatting toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorToolbarPill(
                label = "+ Bullet",
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                accentColor = accentColor,
                onClick = {
                    val current = textFieldValue.text
                    val cursor = textFieldValue.selection.start.coerceIn(0, current.length)
                    val prefix = if (cursor == 0 || current[cursor - 1] == '\n') "• " else "\n• "
                    val updated = buildString {
                        append(current.substring(0, cursor))
                        append(prefix)
                        append(current.substring(cursor))
                    }
                    val nextCursor = cursor + prefix.length
                    textFieldValue = TextFieldValue(updated, TextRange(nextCursor))
                    onValueChange(updated)
                },
                modifier = Modifier.testTag("${testTagPrefix}_add_bullet")
            )

            EditorToolbarPill(
                label = "Format Lines",
                icon = Icons.Default.FormatListBulleted,
                accentColor = accentColor,
                onClick = {
                    val formatted = textFieldValue.text
                        .lines()
                        .joinToString("\n") { line ->
                            val trimmed = line.trim()
                            when {
                                trimmed.isEmpty() -> ""
                                trimmed.startsWith("•") -> trimmed
                                trimmed.startsWith("- ") -> "• ${trimmed.drop(2)}"
                                trimmed.startsWith("* ") -> "• ${trimmed.drop(2)}"
                                else -> "• $trimmed"
                            }
                        }
                    textFieldValue = TextFieldValue(formatted, TextRange(formatted.length))
                    onValueChange(formatted)
                },
                modifier = Modifier.testTag("${testTagPrefix}_format_lines")
            )

            quickSnippets.forEach { snippet ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppTheme.card)
                        .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                        .clickable {
                            val current = textFieldValue.text
                            val cursor = textFieldValue.selection.start.coerceIn(0, current.length)
                            val updated = buildString {
                                append(current.substring(0, cursor))
                                append(snippet)
                                append(current.substring(cursor))
                            }
                            val nextCursor = cursor + snippet.length
                            textFieldValue = TextFieldValue(updated, TextRange(nextCursor))
                            onValueChange(updated)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ ${snippet.trim()}",
                        color = AppTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Multi-line Text Editor Input
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { next ->
                textFieldValue = next
                onValueChange(next.text)
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = AppTheme.textSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = AppTheme.text,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.SansSerif
            ),
            colors = editorOutlinedTextFieldColors(accentColor),
            shape = RoundedCornerShape(10.dp),
            minLines = 5,
            maxLines = 12,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_textfield")
        )
    }
}

@Composable
private fun EditorToolbarPill(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditorEmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                color = AppTheme.text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = AppTheme.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(buttonLabel, color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun editorOutlinedTextFieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppTheme.text,
    unfocusedTextColor = AppTheme.text,
    focusedLabelColor = accentColor,
    unfocusedLabelColor = AppTheme.textSecondary,
    focusedContainerColor = AppTheme.card,
    unfocusedContainerColor = AppTheme.card,
    focusedBorderColor = accentColor,
    unfocusedBorderColor = AppTheme.border,
    cursorColor = accentColor
)
