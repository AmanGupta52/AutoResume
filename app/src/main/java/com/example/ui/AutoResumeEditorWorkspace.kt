package com.example.ui

import com.example.ui.theme.AppTheme
import com.example.ui.locale.LocaleManager
import com.example.ui.locale.tr
import com.example.ui.locale.Text

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.launch

@Composable
fun ResumeWizardScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onGoToPreview: () -> Unit
) {
    val activeStep by viewModel.activeStep.collectAsState()
    val activeResume by viewModel.activeResume.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiMessageFeedback by viewModel.aiMessageFeedback.collectAsState()
    val scope = rememberCoroutineScope()

    BackHandler {
        viewModel.saveActiveResume()
        onNavigateBack()
    }

    // Clean wizard wrapper with scrollable fields
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.background)
            .navigationBarsPadding()
    ) {
        // Step Tracker Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                viewModel.saveActiveResume()
                onNavigateBack() 
            }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTheme.text)
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeResume.name,
                    color = AppTheme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Step $activeStep of 10: " + getStepTitle(activeStep),
                    color = AppTheme.textSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    viewModel.saveActiveResume()
                    onGoToPreview()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("wizard_preview_button")
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview", color = Color.White, fontSize = 12.sp)
            }
        }

        // Linear Progress bar indicator
        LinearProgressIndicator(
            progress = { activeStep.toFloat() / 10f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = AppTheme.primary,
            trackColor = AppTheme.border
        )

        // Quick Section Step Jump Bar + Combined Work & Education Editor shortcut
        val stepLabels = listOf(
            1 to "Info",
            2 to "Summary",
            3 to "Education",
            4 to "Experience",
            5 to "Skills",
            6 to "Projects",
            7 to "Certs",
            8 to "Languages",
            9 to "Extras",
            10 to "Style"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppTheme.accent.copy(alpha = 0.14f))
                    .border(1.dp, AppTheme.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.saveActiveResume()
                        viewModel.navState.value = "work_edu_editor"
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("wizard_open_work_edu_editor")
            ) {
                Text(
                    text = "Work & Edu Editor",
                    color = AppTheme.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            stepLabels.forEach { (stepNum, label) ->
                val isSelected = activeStep == stepNum
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AppTheme.primary else AppTheme.card)
                        .border(1.dp, if (isSelected) AppTheme.primary else AppTheme.border, RoundedCornerShape(10.dp))
                        .clickable {
                            viewModel.saveActiveResume()
                            viewModel.activeStep.value = stepNum
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("wizard_step_pill_$stepNum")
                ) {
                    Text(
                        text = "$stepNum. $label",
                        color = if (isSelected) Color.White else AppTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Gemini loading info banner
        AnimatedVisibility(visible = isAiLoading) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.primary.copy(0.12f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppTheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(aiMessageFeedback, color = AppTheme.primary, fontSize = 12.sp)
                }
            }
        }

        // Step Contents container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (activeStep) {
                1 -> WizardStepPersonalInfo(viewModel)
                2 -> WizardStepSummary(viewModel)
                3 -> WizardStepEducation(viewModel)
                4 -> WizardStepExperience(viewModel)
                5 -> WizardStepSkills(viewModel)
                6 -> WizardStepProjects(viewModel)
                7 -> WizardStepCertifications(viewModel)
                8 -> WizardStepLanguages(viewModel)
                9 -> WizardStepAchievements(viewModel)
                10 -> WizardStepTemplateSelection(viewModel)
            }
        }

        // Wizard Bottom bar Buttons
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.card),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, AppTheme.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (activeStep > 1) {
                            viewModel.saveActiveResume()
                            viewModel.activeStep.value -= 1
                        }
                    },
                    enabled = activeStep > 1
                ) {
                    Text("Previous", color = if (activeStep > 1) AppTheme.text else AppTheme.textSecondary.copy(alpha = 0.4f))
                }

                Button(
                    onClick = {
                        viewModel.saveActiveResume()
                        if (activeStep < 10) {
                            viewModel.activeStep.value += 1
                        } else {
                            onGoToPreview()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("wizard_next_button")
                ) {
                    Text(if (activeStep == 10) "Finish Builder" else "Next Step", color = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.NavigateNext, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

private fun getStepTitle(step: Int): String {
    return when (step) {
        1 -> "Personal Contacts"
        2 -> "AI Summary Profile"
        3 -> "Education Background"
        4 -> "Work History Entries"
        5 -> "Skill Taxonomy"
        6 -> "Project Portfolio"
        7 -> "Certifications"
        8 -> "Language Proficiency"
        9 -> "Achievements & Extras"
        else -> "Template Accent Styling"
    }
}

// --- WIZARD COMPONENTS STEP 1-10 ---

@Composable
fun WizardStepPersonalInfo(viewModel: MainViewModel) {
    var resume by remember { mutableStateOf(viewModel.activeResume.value) }
    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            resume = resume.copy(avatarUri = uri.toString())
            viewModel.activeResume.value = resume
            viewModel.saveActiveResume()
        }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Provide contact details below. Recruiters rely on accurate links to reach out.", color = Color.Gray, fontSize = 13.sp)

        Text("Profile Picture (Optional)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AppTheme.card)
                    .border(2.dp, AppTheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (resume.avatarUri.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = resume.avatarUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile picture placeholder",
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { imageLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Upload Photo", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (resume.avatarUri.isNotBlank()) {
                        Button(
                            onClick = {
                                resume = resume.copy(avatarUri = "")
                                viewModel.activeResume.value = resume
                                viewModel.saveActiveResume()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Remove", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                // Fine-tuned avatar presets for quick UI previews
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Presets:", color = Color.Gray, fontSize = 9.sp)
                    listOf(
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", 
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", 
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", 
                        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"
                    ).forEachIndexed { _, url ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    resume = resume.copy(avatarUri = url)
                                    viewModel.activeResume.value = resume
                                    viewModel.saveActiveResume()
                                }
                                .border(
                                    width = if (resume.avatarUri == url) 1.5.dp else 0.dp,
                                    color = Color(0xFF30D158),
                                    shape = CircleShape
                                )
                        ) {
                            coil.compose.AsyncImage(
                                model = url,
                                contentDescription = "Avatar Preset",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
        
        OutlinedTextField(
            value = resume.fullName,
            onValueChange = { resume = resume.copy(fullName = it); viewModel.activeResume.value = resume },
            label = { Text("Full Name", color = Color.Gray) },
            colors = editorOutlinedTextFieldColors(AppTheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = resume.jobTitle,
            onValueChange = { resume = resume.copy(jobTitle = it); viewModel.activeResume.value = resume },
            label = { Text("Target Job Title (Role)", color = Color.Gray) },
            colors = editorOutlinedTextFieldColors(AppTheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = resume.phone,
                onValueChange = { resume = resume.copy(phone = it); viewModel.activeResume.value = resume },
                label = { Text("Phone Number", color = Color.Gray) },
                colors = editorOutlinedTextFieldColors(AppTheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = resume.email,
                onValueChange = { resume = resume.copy(email = it); viewModel.activeResume.value = resume },
                label = { Text("Email", color = Color.Gray) },
                colors = editorOutlinedTextFieldColors(AppTheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.2f)
            )
        }

        OutlinedTextField(
            value = resume.location,
            onValueChange = { resume = resume.copy(location = it); viewModel.activeResume.value = resume },
            label = { Text("Location (City, State)", color = Color.Gray) },
            colors = editorOutlinedTextFieldColors(AppTheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = resume.linkedinUrl,
            onValueChange = { resume = resume.copy(linkedinUrl = it); viewModel.activeResume.value = resume },
            label = { Text("LinkedIn URL Link", color = Color.Gray) },
            colors = editorOutlinedTextFieldColors(AppTheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = resume.githubUrl,
                onValueChange = { resume = resume.copy(githubUrl = it); viewModel.activeResume.value = resume },
                label = { Text("GitHub Link", color = Color.Gray) },
                colors = editorOutlinedTextFieldColors(AppTheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = resume.portfolioUrl,
                onValueChange = { resume = resume.copy(portfolioUrl = it); viewModel.activeResume.value = resume },
                label = { Text("Portfolio Website", color = Color.Gray) },
                colors = editorOutlinedTextFieldColors(AppTheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun WizardStepSummary(viewModel: MainViewModel) {
    var resume by remember { mutableStateOf(viewModel.activeResume.value) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Describe your career strengths in 2-3 sentences. Generate standard narrative using Gemini AI assistant.", color = Color.Gray, fontSize = 13.sp)
        
        OutlinedTextField(
            value = resume.summary,
            onValueChange = { resume = resume.copy(summary = it); viewModel.activeResume.value = resume },
            label = { Text("Professional Profile Summary", color = Color.Gray) },
            minLines = 5,
            maxLines = 8,
            colors = editorOutlinedTextFieldColors(AppTheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${resume.summary.length} characters used", color = Color.DarkGray, fontSize = 11.sp)
            Button(
                onClick = { viewModel.triggerAiSummaryGeneration() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Draft summary ✨", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WizardStepEducation(viewModel: MainViewModel) {
    EducationEditorSection(viewModel = viewModel)
}

@Composable
fun WizardStepExperience(viewModel: MainViewModel) {
    WorkExperienceEditorSection(viewModel = viewModel)
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun WizardStepSkills(viewModel: MainViewModel) {
    val resume by viewModel.activeResume.collectAsState()
    var newSkillName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Technical") }
    var selectedLevel by remember { mutableStateOf("Expert") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Skills Taxonomy. Tag and categorize technical capabilities.", color = Color.Gray, fontSize = 13.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.card),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Skill Tag", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = newSkillName,
                        onValueChange = { newSkillName = it },
                        placeholder = { Text("e.g. Kotlin, Agile, Figma") },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                        modifier = Modifier.weight(1.2f)
                    )
                    
                    Button(
                        onClick = {
                            if (newSkillName.isNotBlank()) {
                                val newList = resume.skills.toMutableList()
                                newList.add(SkillEntry(newSkillName.trim(), selectedLevel, selectedCategory))
                                viewModel.activeResume.value = resume.copy(skills = newList)
                                newSkillName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Add Tag")
                    }
                }

                // Category selection
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Technical", "Soft", "Tools & Technologies").forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedCategory == cat) AppTheme.primary else Color.DarkGray)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(cat, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Level Selection
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Skill Level:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("Beginner", "Intermediate", "Expert").forEach { lvl ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedLevel == lvl) Color(0xFF30D158) else AppTheme.card)
                                .clickable { selectedLevel = lvl }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(lvl, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.triggerAiSkillsSuggester() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("AI Auto-Suggest standard Skills for ${resume.jobTitle} ✨", color = Color.White, fontSize = 12.sp)
        }

        // Display current skills grouped by categories
        listOf("Technical", "Soft", "Tools & Technologies").forEach { cat ->
            val categorized = resume.skills.filter { it.category == cat }
            if (categorized.isNotEmpty()) {
                Text(cat.uppercase(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 5
                ) {
                    categorized.forEach { skill ->
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppTheme.card)
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(skill.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (skill.level) {
                                                "Expert" -> Color(0xFF30D158)
                                                "Intermediate" -> AppTheme.primary
                                                else -> Color.Gray
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            val newList = resume.skills.toMutableList()
                                            newList.remove(skill)
                                            viewModel.activeResume.value = resume.copy(skills = newList)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WizardStepProjects(viewModel: MainViewModel) {
    val resume by viewModel.activeResume.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Academic & Personal Projects", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            TextButton(onClick = {
                val newList = resume.projects.toMutableList()
                newList.add(ProjectEntry("Online Storefront", "React, Node.js", "Integrated Stripe checkout and multi sessions login.", "github.com/shop", "shop.live"))
                viewModel.activeResume.value = resume.copy(projects = newList)
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Project")
            }
        }

        if (resume.projects.isEmpty()) {
            Text("No projects configured yet.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
        }

        resume.projects.forEachIndexed { index, proj ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Project #${index + 1}", color = AppTheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            val newList = resume.projects.toMutableList()
                            newList.removeAt(index)
                            viewModel.activeResume.value = resume.copy(projects = newList)
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedTextField(
                        value = proj.title,
                        onValueChange = {
                            val newList = resume.projects.toMutableList()
                            newList[index] = proj.copy(title = it)
                            viewModel.activeResume.value = resume.copy(projects = newList)
                        },
                        label = { Text("Project Title", color = Color.Gray, fontSize = 11.sp) },
                        colors = editorOutlinedTextFieldColors(AppTheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = proj.techStack,
                        onValueChange = {
                            val newList = resume.projects.toMutableList()
                            newList[index] = proj.copy(techStack = it)
                            viewModel.activeResume.value = resume.copy(projects = newList)
                        },
                        label = { Text("Technologies Tags (use commas)", color = Color.Gray, fontSize = 11.sp) },
                        colors = editorOutlinedTextFieldColors(AppTheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = proj.description,
                        onValueChange = {
                            val newList = resume.projects.toMutableList()
                            newList[index] = proj.copy(description = it)
                            viewModel.activeResume.value = resume.copy(projects = newList)
                        },
                        label = { Text("Project Brief Summary description", color = Color.Gray, fontSize = 11.sp) },
                        colors = editorOutlinedTextFieldColors(AppTheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proj.githubLink,
                            onValueChange = {
                                val newList = resume.projects.toMutableList()
                                newList[index] = proj.copy(githubLink = it)
                                viewModel.activeResume.value = resume.copy(projects = newList)
                            },
                            label = { Text("GitHub Link", color = Color.Gray, fontSize = 11.sp) },
                            colors = editorOutlinedTextFieldColors(AppTheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = proj.liveLink,
                            onValueChange = {
                                val newList = resume.projects.toMutableList()
                                newList[index] = proj.copy(liveLink = it)
                                viewModel.activeResume.value = resume.copy(projects = newList)
                            },
                            label = { Text("Live Demo App URL", color = Color.Gray, fontSize = 11.sp) },
                            colors = editorOutlinedTextFieldColors(AppTheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WizardStepCertifications(viewModel: MainViewModel) {
    val resume by viewModel.activeResume.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Credentials & Certifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            TextButton(onClick = {
                val newList = resume.certifications.toMutableList()
                newList.add(CertificationEntry("Associate Android Developer", "Google Developers", "May 2024", "ID10329A", "verification.google.com/cert"))
                viewModel.activeResume.value = resume.copy(certifications = newList)
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Certificate")
            }
        }

        if (resume.certifications.isEmpty()) {
            Text("No certifications added yet.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
        }

        resume.certifications.forEachIndexed { index, cert ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cert #${index + 1}", color = AppTheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            val newList = resume.certifications.toMutableList()
                            newList.removeAt(index)
                            viewModel.activeResume.value = resume.copy(certifications = newList)
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedTextField(
                        value = cert.name,
                        onValueChange = {
                            val newList = resume.certifications.toMutableList()
                            newList[index] = cert.copy(name = it)
                            viewModel.activeResume.value = resume.copy(certifications = newList)
                        },
                        label = { Text("Certificate Name", color = Color.Gray, fontSize = 11.sp) },
                        colors = editorOutlinedTextFieldColors(AppTheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cert.issuer,
                            onValueChange = {
                                val newList = resume.certifications.toMutableList()
                                newList[index] = cert.copy(issuer = it)
                                viewModel.activeResume.value = resume.copy(certifications = newList)
                            },
                            label = { Text("Issuer / Provider", color = Color.Gray, fontSize = 11.sp) },
                            colors = editorOutlinedTextFieldColors(AppTheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = cert.date,
                            onValueChange = {
                                val newList = resume.certifications.toMutableList()
                                newList[index] = cert.copy(date = it)
                                viewModel.activeResume.value = resume.copy(certifications = newList)
                            },
                            label = { Text("Issue Date", color = Color.Gray, fontSize = 11.sp) },
                            colors = editorOutlinedTextFieldColors(AppTheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cert.credentialId,
                            onValueChange = {
                                val newList = resume.certifications.toMutableList()
                                newList[index] = cert.copy(credentialId = it)
                                viewModel.activeResume.value = resume.copy(certifications = newList)
                            },
                            label = { Text("Credential ID Link", color = Color.Gray, fontSize = 11.sp) },
                            colors = editorOutlinedTextFieldColors(AppTheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cert.url,
                            onValueChange = {
                                val newList = resume.certifications.toMutableList()
                                newList[index] = cert.copy(url = it)
                                viewModel.activeResume.value = resume.copy(certifications = newList)
                            },
                            label = { Text("Verification URL Link", color = Color.Gray, fontSize = 11.sp) },
                            colors = editorOutlinedTextFieldColors(AppTheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WizardStepLanguages(viewModel: MainViewModel) {
    val resume by viewModel.activeResume.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Known Languages & Fluency", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            TextButton(onClick = {
                val newList = resume.languages.toMutableList()
                newList.add(LanguageEntry("German", "Intermediate"))
                viewModel.activeResume.value = resume.copy(languages = newList)
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Language")
            }
        }

        if (resume.languages.isEmpty()) {
            Text("No languages entry listed.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
        }

        resume.languages.forEachIndexed { index, lang ->
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = lang.name,
                        onValueChange = {
                            val newList = resume.languages.toMutableList()
                            newList[index] = lang.copy(name = it)
                            viewModel.activeResume.value = resume.copy(languages = newList)
                        },
                        label = { Text("Language", color = Color.Gray, fontSize = 11.sp) },
                        colors = editorOutlinedTextFieldColors(AppTheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Simple proficiency selection click
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Fluency Proficient:", color = Color.Gray, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Native", "Fluent", "Basic").forEach { prof ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (lang.proficiency == prof) Color(0xFF30D158) else AppTheme.card)
                                        .clickable {
                                            val newList = resume.languages.toMutableList()
                                            newList[index] = lang.copy(proficiency = prof)
                                            viewModel.activeResume.value = resume.copy(languages = newList)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(prof, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    IconButton(onClick = {
                        val newList = resume.languages.toMutableList()
                        newList.removeAt(index)
                        viewModel.activeResume.value = resume.copy(languages = newList)
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WizardStepAchievements(viewModel: MainViewModel) {
    var resume by remember { mutableStateOf(viewModel.activeResume.value) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Highlight professional honors, peer publications, volunteerism or hobbies standard elements.", color = Color.Gray, fontSize = 13.sp)

        OutlinedTextField(
            value = resume.achievements,
            onValueChange = { resume = resume.copy(achievements = it); viewModel.activeResume.value = resume },
            label = { Text("Achievements & Volunteering Descriptions", color = Color.Gray) },
            minLines = 6,
            colors = editorOutlinedTextFieldColors(AppTheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WizardStepTemplateSelection(viewModel: MainViewModel) {
    val resume by viewModel.activeResume.collectAsState()
    
    val templates = listOf(
        "Modern Professional",
        "Minimal ATS",
        "Premium Creative",
        "Developer Resume",
        "Executive Premium"
    )
    val colors = listOf("Blue", "Green", "Cyan", "Slate", "Dark")
    val sizes = listOf("Compact", "Standard", "Large")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Select Visual Layout template rules:", color = AppTheme.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        
        // Horizontal Scroll Grid of 30 templates choices
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(templates) { tmp ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (resume.templateId == tmp) AppTheme.primary.copy(0.12f) else AppTheme.card
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (resume.templateId == tmp) 2.dp else 1.dp,
                        color = if (resume.templateId == tmp) AppTheme.primary else Color.DarkGray
                    ),
                    modifier = Modifier
                        .width(145.dp)
                        .clickable { viewModel.activeResume.value = resume.copy(templateId = tmp) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    tmp.contains("Classic") || tmp.contains("Border") -> Icons.Default.Style
                                    tmp.contains("Minimal") || tmp.contains("Mist") || tmp.contains("Sleek") -> Icons.Default.AutoAwesome
                                    tmp.contains("Executive") || tmp.contains("Traditional") || tmp.contains("Corporate") || tmp.contains("Ivy") -> Icons.Default.Work
                                    tmp.contains("Creative") || tmp.contains("Timeline") || tmp.contains("Oasis") || tmp.contains("Sunset") -> Icons.Default.Palette
                                    tmp.contains("Elite") || tmp.contains("Academic") -> Icons.Default.School
                                    tmp.contains("Console") || tmp.contains("Terminal") || tmp.contains("Cyberpunk") -> Icons.Default.Terminal
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(tmp, color = AppTheme.text, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        
        Text("Color Schemes picker:", color = AppTheme.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEach { col ->
                val hexColor = when (col) {
                    "Blue" -> AppTheme.primary
                    "Green" -> Color(0xFF30D158)
                    "Cyan" -> Color(0xFF5AC8FA)
                    "Slate" -> Color(0xFF8E8E93)
                    else -> AppTheme.background
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(hexColor)
                        .border(
                            width = if (resume.colorSchemeId == col) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { viewModel.activeResume.value = resume.copy(colorSchemeId = col) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Typography size slider:", color = AppTheme.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            sizes.forEach { sz ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (resume.fontSize == sz) AppTheme.primary else Color.DarkGray)
                        .clickable { viewModel.activeResume.value = resume.copy(fontSize = sz) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(sz, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
