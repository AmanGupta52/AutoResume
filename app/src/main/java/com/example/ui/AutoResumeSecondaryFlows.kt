package com.example.ui

import com.example.ui.theme.AppTheme
import com.example.ui.locale.LocaleManager
import com.example.ui.locale.tr
import com.example.ui.locale.Text

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.launch

// ==========================================
// 12. RESUME PREVIEW SCREEN (6 RENDERERS)
// ==========================================
@Composable
fun ResumePreviewScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onGoToAts: () -> Unit
) {
    val activeResume by viewModel.activeResume.collectAsState()
    val context = LocalContext.current
    var isDarkExport by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    val templatesList = listOf("Modern Professional", "Minimal ATS", "Premium Creative", "Developer Resume", "Executive Premium")
    val colorsList = listOf("Blue", "Green", "Cyan", "Slate", "Dark")

    // ONE document: this exact HTML is what the preview shows and what the PDF prints.
    val html = remember(activeResume, isDarkExport) {
        ResumeHtmlHelper.generateHtml(context, activeResume, isDarkExport, forPreview = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateBack,
                shape = CircleShape,
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close preview", tint = AppTheme.text)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Preview", color = AppTheme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Exactly what the PDF will look like", color = AppTheme.textSecondary, fontSize = 12.sp)
            }
        }

        // The page itself, sunk into the surface like a tray
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .neuInset(RoundedCornerShape(20.dp))
                .padding(6.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            ResumeWebPreview(html = html, modifier = Modifier.fillMaxSize())
        }

        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                    items(templatesList) { tmp ->
                        NeuChoice(
                            label = tmp,
                            selected = ResumeHtmlHelper.normalizeTemplate(activeResume.templateId) == tmp,
                            onClick = {
                                viewModel.activeResume.value = activeResume.copy(templateId = tmp)
                                viewModel.saveActiveResume()
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(4.dp)) {
                        colorsList.forEach { col ->
                            val dot = when (col) {
                                "Blue" -> AppTheme.primary
                                "Green" -> Color(0xFF30D158)
                                "Cyan" -> Color(0xFF5AC8FA)
                                "Slate" -> Color(0xFF8E8E93)
                                else -> Color(0xFF2C2C2E)
                            }
                            NeuSwatch(color = dot, selected = activeResume.colorSchemeId == col, onClick = {
                                viewModel.activeResume.value = activeResume.copy(colorSchemeId = col)
                                viewModel.saveActiveResume()
                            })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(4.dp)) {
                        NeuChoice("Light page", !isDarkExport, { isDarkExport = false })
                        NeuChoice("Dark page", isDarkExport, { isDarkExport = true })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!exporting) {
                            exporting = true
                            ResumePdfHelper.exportPdf(context, activeResume, isDarkExport) { file ->
                                exporting = false
                                if (file == null) {
                                    android.widget.Toast.makeText(context, "Could not create the PDF. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                                } else try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Resume - ${activeResume.fullName}")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share or save PDF"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Cannot share PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary, contentColor = Color.White),
                        modifier = Modifier.weight(1.4f).testTag("preview_print_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (exporting) "Creating PDF..." else "Save / Share PDF", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            try { ResumeHtmlHelper.printResume(context, activeResume, isDarkExport) }
                            catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Cannot open print dialog: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Print")
                    }
                    Button(
                        onClick = {
                            viewModel.atsSelectedResumeId.value = activeResume.id
                            onGoToAts()
                        },
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppTheme.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ATS")
                    }
                }
            }
        }
    }
}

// --- UNIFIED HIGH-FIDELITY 30 TEMPLATES RENDERER SYSTEM ---

data class ResumeTemplateConfig(
    val id: String,
    val name: String,
    val layoutStyle: String, // TwoColumnLeft, TwoColumnRight, SingleColumn, DeveloperTerminal
    val fontFamily: FontFamily,
    val primaryColor: Color,
    val backgroundColor: Color = Color.White,
    val sectionHeaderStyle: String = "underlined", // underlined, filledBlock, accentBorder, doubleLine, minimalClean
    val dividerType: String = "solid" // solid, dashed, double, none
)

fun mapOldTemplateId(old: String): String {
    return when {
        old.contains("Modern Professional") || old.contains("Creative Columnist") || old.contains("Modern Classic") || old == "Modern" -> "Modern Professional"
        old.contains("Minimal ATS") || old.contains("Sheets") || old.contains("Standard") || old.contains("Minimal") -> "Minimal ATS"
        old.contains("Premium Creative") || old.contains("Creative") -> "Premium Creative"
        old.contains("Developer") || old.contains("Aman Tech") || old.contains("Console") || old.contains("Terminal") -> "Developer Resume"
        old.contains("Executive") || old.contains("Corporate") -> "Executive Premium"
        else -> "Modern Professional"
    }
}

fun getTemplateConfig(id: String, defaultBrandHue: Color): ResumeTemplateConfig {
    val sans = FontFamily.SansSerif
    val serif = FontFamily.Serif
    val mono = FontFamily.Monospace

    val mappedId = mapOldTemplateId(id)

    return when (mappedId) {
        "Modern Professional" -> ResumeTemplateConfig(
            id = "Modern Professional",
            name = "Modern Professional",
            layoutStyle = "TwoColumnLeft",
            fontFamily = sans,
            primaryColor = defaultBrandHue,
            sectionHeaderStyle = "accentBorder"
        )
        "Minimal ATS" -> ResumeTemplateConfig(
            id = "Minimal ATS",
            name = "Minimal ATS",
            layoutStyle = "SingleColumn",
            fontFamily = sans,
            primaryColor = Color.Black, // Monochrome ATS spacing
            sectionHeaderStyle = "underlined"
        )
        "Premium Creative" -> ResumeTemplateConfig(
            id = "Premium Creative",
            name = "Premium Creative",
            layoutStyle = "TwoColumnLeft",
            fontFamily = sans,
            primaryColor = defaultBrandHue,
            sectionHeaderStyle = "circleLetterBadge"
        )
        "Developer Resume" -> ResumeTemplateConfig(
            id = "Developer Resume",
            name = "Developer Resume",
            layoutStyle = "SingleColumn",
            fontFamily = mono,
            primaryColor = defaultBrandHue,
            sectionHeaderStyle = "underlined"
        )
        "Executive Premium" -> ResumeTemplateConfig(
            id = "Executive Premium",
            name = "Executive Premium",
            layoutStyle = "SingleColumn",
            fontFamily = serif,
            primaryColor = defaultBrandHue,
            sectionHeaderStyle = "filledBlock"
        )
        else -> ResumeTemplateConfig(
            id = "Modern Professional",
            name = "Modern Professional",
            layoutStyle = "TwoColumnLeft",
            fontFamily = sans,
            primaryColor = defaultBrandHue,
            sectionHeaderStyle = "accentBorder"
        )
    }
}

@Composable
fun SectionHeader(title: String, config: ResumeTemplateConfig, headingSize: androidx.compose.ui.unit.TextUnit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 3.dp)) {
        when (config.sectionHeaderStyle) {
            "circleLetterBadge" -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    val letter = title.firstOrNull()?.toString()?.uppercase() ?: ""
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(config.primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            color = Color.White,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = config.fontFamily
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(title.uppercase(), color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                }
            }
            "underlined" -> {
                Text(title.uppercase(), color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                Divider(color = config.primaryColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
            }
            "filledBlock" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(config.primaryColor)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(title.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                }
            }
            "accentBorder" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(3.dp).height(10.dp).background(config.primaryColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(title.uppercase(), color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                }
                Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
            }
            "doubleLine" -> {
                Divider(color = config.primaryColor, thickness = 0.5.dp)
                Text(title.uppercase(), color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily, modifier = Modifier.padding(vertical = 1.dp))
                Divider(color = config.primaryColor, thickness = 0.5.dp)
            }
            else -> {
                Text(title.uppercase(), color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun UnifiedResumeRenderer(resume: Resume, templateId: String, defaultBrandHue: Color, pageIndex: Int = 1) {
    val mappedId = mapOldTemplateId(templateId)
    val config = getTemplateConfig(mappedId, defaultBrandHue)
    
    // Exact proportional text units for layout fitting
    val titleSize = 13.sp
    val headingSize = 9.sp
    val bodySize = 7.5.sp
    val subBodySize = 7.2.sp
    val metaSize = 6.8.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(config.backgroundColor)
            .padding(10.dp)
    ) {
        when (config.layoutStyle) {
            "DeveloperTerminal" -> TerminalRenderer(resume, config, titleSize, headingSize, subBodySize, pageIndex)
            "TwoColumnLeft", "TwoColumnRight" -> TwoColumnRenderer(resume, config, titleSize, headingSize, subBodySize, metaSize, pageIndex)
            else -> SingleColumnRenderer(resume, config, titleSize, headingSize, subBodySize, metaSize, pageIndex)
        }
    }
}

@Composable
fun TwoColumnRenderer(
    resume: Resume,
    config: ResumeTemplateConfig,
    titleSize: androidx.compose.ui.unit.TextUnit,
    headingSize: androidx.compose.ui.unit.TextUnit,
    subBodySize: androidx.compose.ui.unit.TextUnit,
    metaSize: androidx.compose.ui.unit.TextUnit,
    pageIndex: Int = 1
) {
    if (pageIndex == 1) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(resume.fullName.uppercase(), color = config.primaryColor, fontWeight = FontWeight.Black, fontSize = titleSize, fontFamily = config.fontFamily, letterSpacing = 1.sp)
                Text(resume.jobTitle.uppercase(), color = Color.Gray, fontSize = metaSize, fontWeight = FontWeight.SemiBold, fontFamily = config.fontFamily)
            }
        }
        Divider(color = config.primaryColor, thickness = 1.5.dp, modifier = Modifier.padding(bottom = 6.dp))
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(resume.fullName.uppercase() + " — PAGE 2", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = config.fontFamily, letterSpacing = 1.sp)
                Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        val sidebarContent = @Composable {
            Column(modifier = Modifier.fillMaxHeight().padding(end = 6.dp)) {
                if (pageIndex == 1) {
                    // Profile picture at the top of sidebar!
                    if (resume.avatarUri.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (resume.avatarUri == "initials" || resume.avatarUri.startsWith("gradient")) {
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape)
                                        .background(config.primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = resume.fullName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = config.fontFamily
                                    )
                                }
                            } else {
                                coil.compose.AsyncImage(
                                    model = resume.avatarUri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, config.primaryColor, CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    // Contact Details
                    if (resume.email.isNotBlank() || resume.phone.isNotBlank() || resume.location.isNotBlank()) {
                        SectionHeader("Contact Details", config, headingSize)
                        if (resume.email.isNotBlank()) Text(resume.email, color = Color.Black, fontSize = metaSize, fontFamily = config.fontFamily)
                        if (resume.phone.isNotBlank()) Text(resume.phone, color = Color.Black, fontSize = metaSize, fontFamily = config.fontFamily)
                        if (resume.location.isNotBlank()) Text(resume.location, color = Color.Black, fontSize = metaSize, fontFamily = config.fontFamily)
                    }
                    if (resume.githubUrl.isNotBlank()) Text(resume.githubUrl, color = Color.Black, fontSize = metaSize, fontFamily = config.fontFamily)
                    if (resume.linkedinUrl.isNotBlank()) Text(resume.linkedinUrl, color = Color.Black, fontSize = metaSize, fontFamily = config.fontFamily)

                    // Technical Skills (grouped by category)
                    if (resume.skills.isNotEmpty()) {
                        SectionHeader("Skills", config, headingSize)
                        if (config.id == "Premium Creative") {
                            resume.skills.forEach { skill ->
                                Column(modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(skill.name, color = Color.Black, fontSize = metaSize, fontFamily = config.fontFamily, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text(skill.level, color = Color.Gray, fontSize = metaSize * 0.9f, fontFamily = config.fontFamily)
                                    }
                                    val pct = when (skill.level.lowercase()) {
                                        "beginner" -> 0.33f
                                        "intermediate" -> 0.66f
                                        "expert", "advanced", "native" -> 1f
                                        else -> 0.75f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .background(Color.LightGray.copy(0.3f), CircleShape)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(pct)
                                                .height(3.dp)
                                                .background(config.primaryColor, CircleShape)
                                        )
                                    }
                                }
                            }
                        } else {
                            val grouped = resume.skills.groupBy { it.category.ifBlank { "Core" } }
                            grouped.forEach { (cat, list) ->
                                Text(cat, fontWeight = FontWeight.Bold, color = config.primaryColor, fontSize = metaSize, fontFamily = config.fontFamily, modifier = Modifier.padding(top = 2.dp))
                                Text(list.joinToString { it.name }, color = Color.DarkGray, fontSize = metaSize, fontFamily = config.fontFamily)
                            }
                        }
                    }
                } else {
                    // Page 2 Sidebar details
                    if (resume.languages.isNotEmpty()) {
                        SectionHeader("Languages", config, headingSize)
                        resume.languages.forEach { lang ->
                            Text("${lang.name} (${lang.proficiency})", color = Color.DarkGray, fontSize = metaSize, fontFamily = config.fontFamily)
                        }
                    }
                    SectionHeader("Social Links", config, headingSize)
                    if (resume.linkedinUrl.isNotBlank()) Text("LinkedIn: " + resume.linkedinUrl.take(20) + "...", color = Color.DarkGray, fontSize = metaSize, fontFamily = config.fontFamily)
                    if (resume.githubUrl.isNotBlank()) Text("GitHub: " + resume.githubUrl.take(20) + "...", color = Color.DarkGray, fontSize = metaSize, fontFamily = config.fontFamily)
                }
            }
        }

        val mainContent = @Composable {
            Column(modifier = Modifier.fillMaxHeight().padding(start = 6.dp)) {
                if (pageIndex == 1) {
                    // Summary profile
                    if (resume.summary.isNotBlank()) {
                        SectionHeader("Summary Profile", config, headingSize)
                        Text(resume.summary, color = Color.Black, fontSize = subBodySize, lineHeight = 9.sp, fontFamily = config.fontFamily)
                    }

                    // Experience
                    if (resume.experience.isNotEmpty()) {
                        SectionHeader("Work Experience", config, headingSize)
                        resume.experience.forEach { exp ->
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${exp.role} at ${exp.company}", fontWeight = FontWeight.Bold, color = config.primaryColor, fontSize = subBodySize, fontFamily = config.fontFamily)
                                    Text("${exp.startDate} - ${exp.endDate}", color = Color.Gray, fontSize = metaSize, fontFamily = config.fontFamily)
                                }
                                val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                                bullets.forEach { b ->
                                    val formatted = if (b.trim().startsWith("•")) b.trim() else "• " + b.trim()
                                    Text(formatted, color = Color.DarkGray, fontSize = subBodySize, lineHeight = 9.sp, fontFamily = config.fontFamily)
                                }
                            }
                        }
                    }
                } else {
                    // Projects
                    if (resume.projects.isNotEmpty()) {
                        SectionHeader("Projects", config, headingSize)
                        resume.projects.forEach { prj ->
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${prj.title} (${prj.techStack})", fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = subBodySize, fontFamily = config.fontFamily)
                                    if (prj.githubLink.isNotBlank()) {
                                        Text(prj.githubLink.take(18) + "..", color = config.primaryColor, fontSize = metaSize, fontFamily = config.fontFamily)
                                    }
                                }
                                Text(prj.description, color = Color.DarkGray, fontSize = subBodySize, lineHeight = 9.sp, fontFamily = config.fontFamily)
                            }
                        }
                    }

                    // Education
                    if (resume.education.isNotEmpty()) {
                        SectionHeader("Education", config, headingSize)
                        resume.education.forEach { edu ->
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${edu.degree} — ${edu.institution}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = subBodySize, fontFamily = config.fontFamily)
                                    Text("${edu.startYear} - ${edu.endYear}", color = Color.Gray, fontSize = metaSize, fontFamily = config.fontFamily)
                                }
                                if (edu.grade.isNotBlank() || edu.achievements.isNotBlank()) {
                                    Text("Grade: ${edu.grade} | ${edu.achievements}", color = Color.DarkGray, fontSize = metaSize, fontFamily = config.fontFamily)
                                }
                            }
                        }
                    }

                    // Certifications & Achievements
                    if (resume.certifications.isNotEmpty()) {
                        SectionHeader("Certifications", config, headingSize)
                        resume.certifications.forEach { cert ->
                            Text("${cert.name} — ${cert.issuer} (${cert.date})", color = Color.Black, fontSize = subBodySize, fontFamily = config.fontFamily)
                        }
                    }

                    if (resume.achievements.isNotBlank()) {
                        SectionHeader("Achievements / Details", config, headingSize)
                        Text(resume.achievements, color = Color.DarkGray, fontSize = subBodySize, fontFamily = config.fontFamily)
                    }
                }
            }
        }

        if (config.id.contains("Right") || config.id.contains("Emerald Pro") || config.id.contains("Bold Impact") || config.id.contains("Steel Dynamic") || config.id.contains("Accent Right")) {
            Box(modifier = Modifier.weight(0.65f)) { mainContent() }
            Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.fillMaxHeight().width(1.dp).padding(horizontal = 4.dp))
            Box(modifier = Modifier.weight(0.35f)) { sidebarContent() }
        } else {
            Box(modifier = Modifier.weight(0.35f)) { sidebarContent() }
            Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.fillMaxHeight().width(1.dp).padding(horizontal = 4.dp))
            Box(modifier = Modifier.weight(0.65f)) { mainContent() }
        }
    }
}

@Composable
fun SingleColumnRenderer(
    resume: Resume,
    config: ResumeTemplateConfig,
    titleSize: androidx.compose.ui.unit.TextUnit,
    headingSize: androidx.compose.ui.unit.TextUnit,
    subBodySize: androidx.compose.ui.unit.TextUnit,
    metaSize: androidx.compose.ui.unit.TextUnit,
    pageIndex: Int = 1
) {
    Column(modifier = Modifier.fillMaxSize().background(config.backgroundColor)) {
        if (pageIndex == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(resume.fullName.uppercase(), color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = titleSize, fontFamily = config.fontFamily, letterSpacing = 1.sp)
                    Text(resume.jobTitle, color = Color.Gray, fontSize = subBodySize, fontFamily = config.fontFamily)
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val contacts = mutableListOf<String>()
                    if (resume.email.isNotBlank()) contacts.add(resume.email)
                    if (resume.phone.isNotBlank()) contacts.add(resume.phone)
                    if (resume.location.isNotBlank()) contacts.add(resume.location)
                    Text(contacts.joinToString(" • "), color = Color.DarkGray, fontSize = metaSize, fontFamily = config.fontFamily)

                    val social = mutableListOf<String>()
                    if (resume.githubUrl.isNotBlank()) social.add(resume.githubUrl)
                    if (resume.linkedinUrl.isNotBlank()) social.add(resume.linkedinUrl)
                    if (social.isNotEmpty()) {
                        Text(social.joinToString(" | "), color = config.primaryColor, fontSize = metaSize, fontFamily = config.fontFamily, modifier = Modifier.padding(top = 1.dp))
                    }
                }
                
                if (resume.avatarUri.isNotBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    if (resume.avatarUri == "initials" || resume.avatarUri.startsWith("gradient")) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .background(config.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = resume.fullName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = config.fontFamily
                            )
                        }
                    } else {
                        coil.compose.AsyncImage(
                            model = resume.avatarUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(45.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, config.primaryColor, CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
            Divider(color = config.primaryColor, thickness = 1.5.dp, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))

            if (resume.summary.isNotBlank()) {
                SectionHeader("Professional Summary", config, headingSize)
                Text(resume.summary, color = Color.Black, fontSize = subBodySize, lineHeight = 9.sp, fontFamily = config.fontFamily)
            }

            if (resume.skills.isNotEmpty()) {
                SectionHeader("Technical Skills", config, headingSize)
                val grouped = resume.skills.groupBy { it.category.ifBlank { "Core" } }
                grouped.forEach { (cat, list) ->
                    Text(
                        text = "$cat: " + list.joinToString { it.name },
                        color = Color.Black,
                        fontSize = subBodySize,
                        fontFamily = config.fontFamily,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            if (resume.experience.isNotEmpty()) {
                SectionHeader("Work Experience", config, headingSize)
                resume.experience.forEach { exp ->
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${exp.role} at ${exp.company}", fontWeight = FontWeight.Bold, color = config.primaryColor, fontSize = subBodySize, fontFamily = config.fontFamily)
                            Text("${exp.startDate} - ${exp.endDate}", color = Color.Gray, fontSize = metaSize, fontFamily = config.fontFamily)
                        }
                        val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                        bullets.forEach { b ->
                            val formatted = if (b.trim().startsWith("•")) b.trim() else "• " + b.trim()
                            Text(formatted, color = Color.DarkGray, fontSize = subBodySize, lineHeight = 9.sp, fontFamily = config.fontFamily)
                        }
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(resume.fullName.uppercase() + " — PAGE 2", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = config.fontFamily, letterSpacing = 1.sp)
                Divider(color = config.primaryColor, thickness = 1.dp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            }

            if (resume.projects.isNotEmpty()) {
                SectionHeader("Selected Projects", config, headingSize)
                resume.projects.forEach { prj ->
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${prj.title} — ${prj.techStack}", fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = subBodySize, fontFamily = config.fontFamily)
                            if (prj.githubLink.isNotBlank()) {
                                Text(prj.githubLink, color = config.primaryColor, fontSize = metaSize, fontFamily = config.fontFamily)
                            }
                        }
                        Text(prj.description, color = Color.DarkGray, fontSize = subBodySize, lineHeight = 9.sp, fontFamily = config.fontFamily)
                    }
                }
            }

            if (resume.education.isNotEmpty()) {
                SectionHeader("Education & Qualifications", config, headingSize)
                resume.education.forEach { edu ->
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${edu.degree}, ${edu.institution}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = subBodySize, fontFamily = config.fontFamily)
                            Text("${edu.startYear} - ${edu.endYear}", color = Color.Gray, fontSize = metaSize, fontFamily = config.fontFamily)
                        }
                        if (edu.grade.isNotBlank() || edu.achievements.isNotBlank()) {
                            Text("Grade: ${edu.grade} • Details: ${edu.achievements}", color = Color.DarkGray, fontSize = metaSize, fontFamily = config.fontFamily)
                        }
                    }
                }
            }

            if (resume.certifications.isNotEmpty()) {
                SectionHeader("Certifications", config, headingSize)
                resume.certifications.forEach { cert ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${cert.name} — ${cert.issuer}", color = Color.Black, fontSize = subBodySize, fontFamily = config.fontFamily)
                        Text(cert.date, color = Color.Gray, fontSize = metaSize, fontFamily = config.fontFamily)
                    }
                }
            }

            if (resume.languages.isNotEmpty()) {
                SectionHeader("Languages", config, headingSize)
                Text(resume.languages.joinToString { "${it.name} (${it.proficiency})" }, color = Color.DarkGray, fontSize = subBodySize, fontFamily = config.fontFamily)
            }

            if (resume.achievements.isNotBlank()) {
                SectionHeader("Achievements, Open Source & Interests", config, headingSize)
                Text(resume.achievements, color = Color.DarkGray, fontSize = subBodySize, fontFamily = config.fontFamily)
            }
        }
    }
}

@Composable
fun TerminalRenderer(
    resume: Resume,
    config: ResumeTemplateConfig,
    titleSize: androidx.compose.ui.unit.TextUnit,
    headingSize: androidx.compose.ui.unit.TextUnit,
    subBodySize: androidx.compose.ui.unit.TextUnit,
    pageIndex: Int = 1
) {
    Column(modifier = Modifier.fillMaxSize().background(config.backgroundColor).padding(4.dp)) {
        if (pageIndex == 1) {
            Text("SYSTEM_INIT_DEV_PORTAL: Parsing record for '${resume.fullName}'...", color = Color.Gray, fontFamily = config.fontFamily, fontSize = 6.sp)
            Text("> NAME: ${resume.fullName}", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = titleSize, fontFamily = config.fontFamily)
            Text("> ROLE: ${resume.jobTitle}", color = Color.White, fontSize = subBodySize, fontFamily = config.fontFamily)
            Text("> COMM: ${resume.email} | ${resume.phone} | ${resume.location}", color = Color.LightGray, fontSize = 6.sp, fontFamily = config.fontFamily)
            Divider(color = config.primaryColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 3.dp))

            if (resume.summary.isNotBlank()) {
                Text(":: executive_profile", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                Text(resume.summary, color = Color.White, fontSize = subBodySize, lineHeight = 8.sp, fontFamily = config.fontFamily)
            }

            if (resume.skills.isNotEmpty()) {
                Text(":: tech_stacks", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                val grouped = resume.skills.groupBy { it.category.ifBlank { "Core" } }
                grouped.forEach { (cat, list) ->
                    Text("  [$cat] -> " + list.joinToString { it.name }, color = Color.LightGray, fontSize = subBodySize, fontFamily = config.fontFamily)
                }
            }

            if (resume.experience.isNotEmpty()) {
                Text(":: experience_history", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                resume.experience.forEach { exp ->
                    Text("* ${exp.role} @ ${exp.company} (${exp.startDate} - ${exp.endDate})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = subBodySize, fontFamily = config.fontFamily)
                    val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                    bullets.forEach { b ->
                        val formatted = "  - " + (if (b.trim().startsWith("•")) b.trim().substring(1).trim() else b.trim())
                        Text(formatted, color = Color.LightGray, fontSize = 6.sp, lineHeight = 8.sp, fontFamily = config.fontFamily)
                    }
                }
            }
        } else {
            Text("> SESSION_RESTORED: Terminal console sequence page 02", color = Color.Gray, fontFamily = config.fontFamily, fontSize = 6.sp)
            Text("> CANDIDATE: ${resume.fullName.uppercase()} (PAGE 2)", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
            Divider(color = config.primaryColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 3.dp))

            if (resume.projects.isNotEmpty()) {
                Text(":: deploy_manifests", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                resume.projects.forEach { prj ->
                    Text("* ${prj.title} (stack: ${prj.techStack})", color = Color.White, fontSize = subBodySize, fontFamily = config.fontFamily)
                    Text("  desc: ${prj.description}", color = Color.LightGray, fontSize = 6.sp, fontFamily = config.fontFamily)
                    if (prj.githubLink.isNotBlank()) {
                        Text("  repo: ${prj.githubLink}", color = config.primaryColor, fontSize = 6.sp, fontFamily = config.fontFamily)
                    }
                }
            }

            if (resume.education.isNotEmpty()) {
                Text(":: qualifications", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                resume.education.forEach { edu ->
                    Text("* ${edu.degree} - ${edu.institution} [${edu.startYear} - ${edu.endYear}]", color = Color.White, fontSize = subBodySize, fontFamily = config.fontFamily)
                }
            }

            if (resume.certifications.isNotEmpty() || resume.languages.isNotEmpty() || resume.achievements.isNotBlank()) {
                Text(":: extras_config", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = headingSize, fontFamily = config.fontFamily)
                if (resume.certifications.isNotEmpty()) {
                    Text("  certs = " + resume.certifications.joinToString { "${it.name} (${it.issuer})" }, color = Color.White, fontSize = 6.sp, fontFamily = config.fontFamily)
                }
                if (resume.languages.isNotEmpty()) {
                    Text("  langs = " + resume.languages.joinToString { "${it.name} (${it.proficiency})" }, color = Color.White, fontSize = 6.sp, fontFamily = config.fontFamily)
                }
                if (resume.achievements.isNotBlank()) {
                    Text("  achievements = ${resume.achievements}", color = Color.LightGray, fontSize = 6.sp, fontFamily = config.fontFamily)
                }
            }
        }
    }
}

// ==========================================
// 14. ATS CHECKER SCREEN
// ==========================================
@Composable
fun AtsCheckerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    AtsAnalyzerScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun AtsBreakdownRow(label: String, score: Int, indicatorColor: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = AppTheme.textSecondary, fontSize = 11.sp)
            Text("$score%", color = AppTheme.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score.toFloat() / 100f },
            color = indicatorColor,
            trackColor = if (AppTheme.isDarkTheme) Color.Black else AppTheme.shadowDark.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
    }
}

// ==========================================
// 16. PORTFOLIO GENERATOR SCREEN
// ==========================================
@Composable
fun PortfolioGeneratorScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var activeThemeSelection by remember { mutableStateOf("Cosmic Gradient") }
    var userSubdomainText by remember { mutableStateOf("alexmercer") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.background)
            .navigationBarsPadding()
    ) {
        // Toolbar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Landing Portfolio Builder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AutoResume leverages details in saved resume drafts to index a beautiful digital landing showcase webpage securely.", color = Color.Gray, fontSize = 13.sp)

            Text("CHOOSE PORTFOLIO THEME ACCENTS", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Cosmic Gradient", "Classic Dark", "Warm Daylight").forEach { th ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeThemeSelection == th) AppTheme.primary else AppTheme.card)
                            .border(1.dp, if (activeThemeSelection == th) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { activeThemeSelection = th }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(th, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Custom Subdomain configuration card
            OutlinedTextField(
                value = userSubdomainText,
                onValueChange = { userSubdomainText = it },
                label = { Text("Custom Web URL Portal", color = Color.Gray) },
                prefix = { Text("autoresume.app/portfolio/", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AppTheme.primary,
                    unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // CTA Connect Custom Subdomain button
            Button(
                onClick = {
                    Toast.makeText(context, "Draft landing url locked: autoresume.app/portfolio/$userSubdomainText! Live syncing synced portfolios.", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lock Portfolio Web Endpoint")
            }

            // Live preview simulator frames mimicking CSS mockup
            Text("LIVE SCREEN SHOWCASE SIMULATOR", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (activeThemeSelection) {
                        "Classic Dark" -> AppTheme.background
                        "Warm Daylight" -> Color(0xFFFAF6F0)
                        else -> Color(0xFF1B0B3C)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                val accentText = if (activeThemeSelection == "Warm Daylight") Color.DarkGray else Color.White
                val mutedText = if (activeThemeSelection == "Warm Daylight") Color.Gray else Color.LightGray

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (activeThemeSelection == "Cosmic Gradient") {
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2E095A), Color(0xFF0A1E5C))
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                )
                            }
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Avatar bubble
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = accentText)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("ALEX MERCER", color = accentText, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 2.sp)
                    Text("Senior Lead Android Engineer", color = mutedText, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.12f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("About Me summary profile:", color = accentText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Developing reliable reactive mobile designs utilizing Android Jetpack libraries. Open for collaboration opportunities.", color = mutedText, fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("EDUCATION", color = brandAccentColor(activeThemeSelection), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text("EXPERIENCE", color = brandAccentColor(activeThemeSelection), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text("PROJECTS", color = brandAccentColor(activeThemeSelection), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun brandAccentColor(theme: String): Color {
    return when (theme) {
        "Warm Daylight" -> AppTheme.primary
        "Classic Dark" -> Color(0xFF30D158)
        else -> Color(0xFF5AC8FA)
    }
}

// ==========================================
// 18. PROFILE & SETTINGS SCREENS
// ==========================================
@Composable
fun ProfileAndSettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val accent by viewModel.appAccentId.collectAsState()
    val notificationsAlerts by viewModel.notifications.collectAsState()

    var showLanguagePicker by remember { mutableStateOf(false) }

    val textClr = if (isDark) Color.White else AppTheme.text
    val subTextClr = if (isDark) AppTheme.textSecondary else AppTheme.textSecondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 72.dp)
    ) {
        // Toolbar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Career Hub Account", color = textClr, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton(onClick = { viewModel.logout() }) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = "Sign Out", tint = Color.Red)
            }
        }

        // Mini Avatar credentials card representation
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.card),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clickable { viewModel.navState.value = "setup_profile" }
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user?.fullName ?: "Guest Builder", color = textClr, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(if (user?.jobTitle?.isNotBlank() == true) user!!.jobTitle else "Job Seeker account status", color = Color(0xFF30D158), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(user?.email ?: "guest@autoresume.com", color = subTextClr, fontSize = 11.sp)
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = subTextClr,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Application Preferences Category
        Text(
            text = "PREFERENCES & THEME",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextClr,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )

        PreferenceToggleRow("Dark Atmosphere Palette", isDark) {
            viewModel.isDarkTheme.value = it
        }

        PreferenceValueSelectorRow(
            label = "App Language Support",
            displayValue = language,
            onClick = { showLanguagePicker = !showLanguagePicker }
        )

        if (showLanguagePicker) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    listOf("English", "Hindi", "Spanish", "French", "Arabic").forEach { lang ->
                        Text(
                            text = lang,
                            color = textClr,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.appLanguage.value = lang
                                    showLanguagePicker = false
                                }
                                .padding(12.dp)
                        )
                    }
                }
            }
        }

        PreferenceValueSelectorRow(
            label = "Accent Primary Identifier",
            displayValue = accent,
            onClick = {
                val list = listOf("Blue", "Green", "Cyan", "Slate", "Dark")
                val curIndex = list.indexOf(accent)
                viewModel.appAccentId.value = list[(curIndex + 1) % list.size]
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // System Alerts and Notifications Listing
        Text(
            text = "SYSTEM CAREER ALERTS FEED",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextClr,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )

        notificationsAlerts.forEach { not ->
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF111114) else AppTheme.card),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(not.title, color = textClr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(not.timeAgo, color = subTextClr, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(not.message, color = subTextClr, fontSize = 12.sp)
                }
            }
        }

         // Storage metrics details
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "LOCAL DISK AND RECOVERY SYNC",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextClr,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.card),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Room SQLite Local usage: 780 KB", color = textClr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Version: v1.0.2 • Connected with Face ID unlock config.", color = subTextClr, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PreferenceToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AppTheme.text, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF30D158))
        )
    }
}

@Composable
fun PreferenceValueSelectorRow(label: String, displayValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AppTheme.text, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(displayValue, color = AppTheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(imageVector = Icons.Default.NavigateNext, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun UnifiedResumeDetailReaderView(resume: Resume, colorSchemeId: String, brandHue: Color, isDark: Boolean) {
    val mappedId = mapOldTemplateId(resume.templateId)
    val config = getTemplateConfig(mappedId, brandHue)
    val txtClr = if (isDark) Color.White else AppTheme.text
    val secondaryText = if (isDark) Color.LightGray else AppTheme.textSecondary
    val bgClr = if (isDark) AppTheme.background else AppTheme.card
    val borderClr = if (isDark) AppTheme.card else AppTheme.shadowDark.copy(alpha = 0.25f)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgClr),
        border = BorderStroke(1.dp, borderClr),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Info
            Text(
                text = resume.fullName.uppercase(),
                color = config.primaryColor,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                fontFamily = config.fontFamily,
                letterSpacing = 1.sp
            )
            Text(
                text = resume.jobTitle,
                color = secondaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = config.fontFamily
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = config.primaryColor, thickness = 2.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Contacts Detail Block
            Text("CONTACT DETAILS", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (resume.email.isNotBlank()) Text("📧 Email: ${resume.email}", color = txtClr, fontSize = 13.sp)
            if (resume.phone.isNotBlank()) Text("📱 Phone: ${resume.phone}", color = txtClr, fontSize = 13.sp)
            if (resume.location.isNotBlank()) Text("📍 Location: ${resume.location}", color = txtClr, fontSize = 13.sp)
            if (resume.linkedinUrl.isNotBlank()) Text("🔗 LinkedIn: ${resume.linkedinUrl}", color = config.primaryColor, fontSize = 13.sp)
            if (resume.githubUrl.isNotBlank()) Text("💻 GitHub: ${resume.githubUrl}", color = config.primaryColor, fontSize = 13.sp)
            if (resume.portfolioUrl.isNotBlank()) Text("🌐 Portfolio: ${resume.portfolioUrl}", color = config.primaryColor, fontSize = 13.sp)

            // Summary
            if (resume.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("PROFESSIONAL SUMMARY", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(resume.summary, color = txtClr, fontSize = 13.sp, lineHeight = 18.sp, fontFamily = config.fontFamily)
            }

            // Experience history list - every detail fully visible!
            if (resume.experience.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("WORK EXPERIENCE HISTORY", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                resume.experience.forEach { exp ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${exp.role} — ${exp.company}", fontWeight = FontWeight.Bold, color = config.primaryColor, fontSize = 14.sp)
                            Text("${exp.startDate} - ${exp.endDate}", color = secondaryText, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                        bullets.forEach { b ->
                            val formatted = if (b.trim().startsWith("•")) b.trim() else "• " + b.trim()
                            Text(formatted, color = txtClr, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // Projects Detailed View
            if (resume.projects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("KEY PROJECT PORTFOLIO", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                resume.projects.forEach { prj ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(prj.title, fontWeight = FontWeight.Bold, color = txtClr, fontSize = 14.sp)
                            Text(prj.techStack, color = config.primaryColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Text(prj.description, color = txtClr, fontSize = 13.sp, lineHeight = 18.sp)
                        if (prj.githubLink.isNotBlank()) Text("Repository: ${prj.githubLink}", color = config.primaryColor, fontSize = 12.sp)
                        if (prj.liveLink.isNotBlank()) Text("Live Site: ${prj.liveLink}", color = config.primaryColor, fontSize = 12.sp)
                    }
                }
            }

            // Educational Degrees
            if (resume.education.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("EDUCATION & ACADEMICS", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                resume.education.forEach { edu ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${edu.degree} — ${edu.institution}", fontWeight = FontWeight.Bold, color = txtClr, fontSize = 13.sp)
                            Text("${edu.startYear} - ${edu.endYear}", color = secondaryText, fontSize = 12.sp)
                        }
                        if (edu.grade.isNotBlank() || edu.achievements.isNotBlank()) {
                            Text("GPA/Grade: ${edu.grade} • Honors: ${edu.achievements}", color = secondaryText, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Skill Taxonomy
            if (resume.skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("TECHNICAL SKILLS & TAXONOMY", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val grouped = resume.skills.groupBy { it.category.ifBlank { "General" } }
                grouped.forEach { (cat, list) ->
                    Text(cat, fontWeight = FontWeight.Bold, color = config.primaryColor, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(list.joinToString { "${it.name} (${it.level})" }, color = txtClr, fontSize = 12.sp)
                }
            }

            // Certifications
            if (resume.certifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("CERTIFICATIONS", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                resume.certifications.forEach { cert ->
                    Text("• ${cert.name} — Issuer: ${cert.issuer} (${cert.date})", color = txtClr, fontSize = 13.sp)
                }
            }

            // Languages
            if (resume.languages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("LANGUAGES", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                resume.languages.forEach { lang ->
                    Text("• ${lang.name} [Proficiency: ${lang.proficiency}]", color = txtClr, fontSize = 13.sp)
                }
            }

            // Additional achievements
            if (resume.achievements.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("ACHIEVEMENTS, AWARDS & EXTRA ACTIVITIES", color = config.primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(resume.achievements, color = txtClr, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

