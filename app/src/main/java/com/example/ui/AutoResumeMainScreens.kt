package com.example.ui

import com.example.ui.theme.AppTheme
import com.example.ui.locale.LocaleManager
import com.example.ui.locale.tr
import com.example.ui.locale.Text

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
// 8. HOME DASHBOARD SCREEN
// ==========================================
@Composable
fun HomeDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val resumes by viewModel.resumes.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val scope = rememberCoroutineScope()
    var searchKeyword by remember { mutableStateOf("") }
    var selectedFilterByTemplate by remember { mutableStateOf("All") }
    
    val user = viewModel.currentUser.collectAsState().value
    
    // Calculate dashboard statistics from Room Database resumes list
    val totalResumes = resumes.size
    val lastEditedName = if (resumes.isNotEmpty()) resumes.first().name else "Never Started"
    val avgAtsScore = if (resumes.isNotEmpty()) resumes.map { it.atsScore }.average().toInt() else 0
    
    // Theme configurations
    val bgColorStart = if (isDark) AppTheme.background else AppTheme.background
    val bgColorEnd = if (isDark) AppTheme.card else AppTheme.card
    val glassCardBg = if (isDark) AppTheme.card.copy(alpha = 0.85f) else Color.White
    val borderClr = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f)
    
    val textColor = if (isDark) Color.White else AppTheme.background
    val subTextColor = if (isDark) AppTheme.textSecondary else AppTheme.textSecondary

    // Avatar map dictionary matches profile setup
    val avatars = listOf(
        "gradient_blue" to listOf(AppTheme.primary, AppTheme.primary),
        "gradient_purple" to listOf(AppTheme.primary, Color(0xFFD422EF)),
        "gradient_teal" to listOf(AppTheme.accent, Color(0xFF0D9488)),
        "gradient_orange" to listOf(Color(0xFFFF9500), Color(0xFFFF2D55)),
        "gradient_pink" to listOf(Color(0xFFFF2D55), AppTheme.primary)
    )
    val userAvatarColors = avatars.firstOrNull { it.first == user?.profilePhotoUrl }?.second ?: avatars[0].second

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColorStart, bgColorEnd)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp) // Avoid overlap with bottom navigation bar
    ) {
        // Sticky/Top Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateToProfile() }
            ) {
                // Circular profile complete indicator containing miniature avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(58.dp)
                ) {
                    Canvas(modifier = Modifier.size(56.dp)) {
                        drawArc(
                            color = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawArc(
                            color = AppTheme.primary,
                            startAngle = -90f,
                            sweepAngle = 280f, // 78% profile completed
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    
                    // User gradient matching custom avatar choice!
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(userAvatarColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (user?.fullName?.isNotBlank() == true) user.fullName.take(1).uppercase() else "R",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Good morning,",
                        fontSize = 12.sp,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${user?.fullName ?: "Career Builder"} 👋",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = (-0.3).sp
                    )
                }
            }
            
            // Notification icon Badge
            Box {
                IconButton(
                    onClick = { viewModel.bottomTabSelected.value = 4 } // Go to profile tabs alerts
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = textColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = (4).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF2D55))
                )
            }
        }
        
        // Circular profile completion card highlight (Glassmorphic)
        Card(
            colors = CardDefaults.cardColors(containerColor = glassCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clickable { onNavigateToProfile() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF30D158))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROFILE COMPLETION: 78%",
                            color = Color(0xFF30D158),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Update skills and work experience to elevate your automated resume output score dynamically.",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = subTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Action Slider (Material 3 cells)
        Text(
            text = "QUICK ACTIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextColor,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                label = "New Resume",
                icon = Icons.Default.Add,
                colorScheme = AppTheme.primary,
                testTag = "quick_action_new_resume",
                onClick = { viewModel.createNewResume() },
                isDark = isDark
            )
            QuickActionButton(
                label = "Work & Edu",
                icon = Icons.Default.Work,
                colorScheme = Color(0xFF30D158),
                testTag = "quick_action_work_edu",
                onClick = { viewModel.openWorkAndEducationEditor() },
                isDark = isDark
            )
            QuickActionButton(
                label = "ATS Check",
                icon = Icons.Default.InsertChartOutlined,
                colorScheme = AppTheme.primary,
                testTag = "quick_action_ats_check",
                onClick = onNavigateToAts,
                isDark = isDark
            )
            QuickActionButton(
                label = "Workspace",
                icon = Icons.Default.Person,
                colorScheme = AppTheme.accent,
                testTag = "quick_action_profile",
                onClick = onNavigateToProfile,
                isDark = isDark
            )
            QuickActionButton(
                label = "Settings",
                icon = Icons.Default.Settings,
                colorScheme = AppTheme.textSecondary,
                testTag = "quick_action_settings",
                onClick = onNavigateToSettings,
                isDark = isDark
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Resume statistics grid (Linear glow visual theme)
        Text(
            text = "WORKSPACE METRICS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextColor,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Created Resumes",
                    value = totalResumes.toString(),
                    trend = "Total Edited Assets",
                    icon = Icons.Default.ContentPaste,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Avg ATS Match",
                    value = "$avgAtsScore%",
                    trend = "Recruiter Approved Score",
                    icon = Icons.Default.Recommend,
                    color = if (avgAtsScore >= 75) Color(0xFF30D158) else Color(0xFFFF9500),
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Latest Edited",
                    value = lastEditedName,
                    trend = "Ready for export PDF",
                    icon = Icons.Default.EditCalendar,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Cloud Synced",
                    value = if (totalResumes > 0) "Completed" else "Idle",
                    trend = "Encrypted in storage",
                    icon = Icons.Default.CloudQueue,
                    color = AppTheme.accent,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recently Edited Resumes list (Modern scroll cells)
        Text(
            text = "RECENTLY MODIFIED",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextColor,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        if (resumes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassCardBg)
                    .border(1.dp, borderClr, RoundedCornerShape(24.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = subTextColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Resumes Yet",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Launch your first professional standardized resume document draft!",
                        color = subTextColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.createNewResume() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .width(180.dp)
                            .background(
                                Brush.linearGradient(colors = listOf(AppTheme.primary, AppTheme.primary)),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Create Dream Resume", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                resumes.take(3).forEach { res ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = glassCardBg),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent),
                        modifier = Modifier
                            .width(260.dp)
                            .clickable { viewModel.selectResumeForEditing(res) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppTheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = res.templateId.uppercase(),
                                        color = AppTheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (res.atsScore >= 75) Color(0xFF30D158).copy(alpha = 0.15f)
                                            else Color(0xFFFF9500).copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${res.atsScore}",
                                        color = if (res.atsScore >= 75) Color(0xFF30D158) else Color(0xFFFF9500),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = res.name,
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = res.jobTitle.ifBlank { "Standard CV Template" },
                                color = subTextColor,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ready to share",
                                    color = Color(0xFF30D158),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = subTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search & Filters bar
        Text(
            text = "SEARCH YOUR RESUMES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextColor,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = { searchKeyword = it },
            placeholder = { Text("Filter resume documents...", color = Color.Gray) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = glassCardBg,
                unfocusedContainerColor = glassCardBg,
                focusedBorderColor = AppTheme.primary,
                unfocusedBorderColor = borderClr
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        // Filter options tags section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL DRAFTS (${resumes.size})",
                color = subTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All", "Modern", "Minimal", "Developer").forEach { tag ->
                    val isSelected = selectedFilterByTemplate == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) AppTheme.primary
                                else if (isDark) AppTheme.border
                                else AppTheme.shadowDark.copy(alpha = 0.25f)
                            )
                            .clickable { selectedFilterByTemplate = tag }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            color = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color.DarkGray),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        val filteredResumes = resumes.filter {
            it.name.contains(searchKeyword, ignoreCase = true) &&
            (selectedFilterByTemplate == "All" || it.templateId.contains(selectedFilterByTemplate, ignoreCase = true))
        }
        
        filteredResumes.forEach { res ->
            RowResumeItem(
                resume = res,
                onSelectToEdit = { viewModel.selectResumeForEditing(res) },
                onOpenWorkAndEdu = { viewModel.openWorkAndEducationEditor(res) },
                onDuplicate = { viewModel.duplicateResume(res) },
                onDelete = { viewModel.deleteResume(res.id) },
                isDark = isDark
            )
        }
        
        // Daily Career Tip Card widget
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "DAILY RECRUITER TIP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = subTextColor,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = glassCardBg),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFF9500))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "The Action-Metric Formula",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Always format your experience bullet points as: [Action Verb] + [Quantified Metric Result] + [Context Methodology]. Using exact percentages makes resumes stand out to premium tech recruiters directly.",
                    color = subTextColor,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    colorScheme: Color,
    testTag: String,
    onClick: () -> Unit,
    isDark: Boolean = true
) {
    val btnBg = if (isDark) AppTheme.card else AppTheme.card
    val txtColor = if (isDark) Color.LightGray else AppTheme.textSecondary
    val borderStroke = if (isDark) null else BorderStroke(1.dp, AppTheme.shadowDark.copy(alpha = 0.25f))

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = btnBg),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        modifier = Modifier
            .height(72.dp)
            .width(132.dp)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colorScheme,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = txtColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    trend: String,
    icon: ImageVector,
    color: Color = AppTheme.primary,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) AppTheme.card else AppTheme.card
    val strokeColor = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f)
    val txtColor = if (isDark) Color.White else AppTheme.text
    val subTxtColor = if (isDark) Color.Gray else AppTheme.textSecondary

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, strokeColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = txtColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trend,
                color = subTxtColor,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun AllResumesScreen(
    viewModel: MainViewModel,
    onNavigateToAts: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val resumes by viewModel.resumes.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }
    var selectedFilterByTemplate by remember { mutableStateOf("All") }
    
    val bgColorStart = if (isDark) AppTheme.background else AppTheme.background
    val bgColorEnd = if (isDark) AppTheme.card else AppTheme.card
    val glassCardBg = if (isDark) AppTheme.card.copy(alpha = 0.85f) else Color.White
    val borderClr = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f)
    
    val textColor = if (isDark) Color.White else AppTheme.background
    val subTextColor = if (isDark) AppTheme.textSecondary else AppTheme.textSecondary
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColorStart, bgColorEnd)
                )
            )
            .statusBarsPadding()
            .padding(bottom = 90.dp) // Avoid overlap with bottom nav
    ) {
        // Sticky/Top Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Resume CV Vault",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage your standardized ATS templates",
                    fontSize = 12.sp,
                    color = subTextColor
                )
            }
            
            // FAB integrated in the header
            Button(
                onClick = { viewModel.createNewResume() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(40.dp)
                    .width(110.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(Color(0xFF30D158), Color(0xFF0D9488))),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Search text field
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = { searchKeyword = it },
            placeholder = { Text("Filter resume documents...", color = Color.Gray) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = glassCardBg,
                unfocusedContainerColor = glassCardBg,
                focusedBorderColor = AppTheme.primary,
                unfocusedBorderColor = borderClr
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        // Templates quick tags list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TEMPLATES FILTER",
                color = subTextColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All", "Modern", "Minimal", "Developer").forEach { tag ->
                    val isSelected = selectedFilterByTemplate == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) AppTheme.primary
                                else if (isDark) AppTheme.border
                                else AppTheme.shadowDark.copy(alpha = 0.25f)
                            )
                            .clickable { selectedFilterByTemplate = tag }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            color = if (isSelected) Color.White else (if (isDark) Color.LightGray else Color.DarkGray),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        val filteredResumes = resumes.filter {
            it.name.contains(searchKeyword, ignoreCase = true) &&
            (selectedFilterByTemplate == "All" || it.templateId.contains(selectedFilterByTemplate, ignoreCase = true))
        }
        
        if (filteredResumes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassCardBg)
                    .border(1.dp, borderClr, RoundedCornerShape(24.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Resumes Found", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Try modern keywords or create a new curriculum vitae draft above!", color = subTextColor, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredResumes) { res ->
                    RowResumeItem(
                        resume = res,
                        onSelectToEdit = { viewModel.selectResumeForEditing(res) },
                        onOpenWorkAndEdu = { viewModel.openWorkAndEducationEditor(res) },
                        onDuplicate = { viewModel.duplicateResume(res) },
                        onDelete = { viewModel.deleteResume(res.id) },
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
fun RowResumeItem(
    resume: Resume,
    onSelectToEdit: () -> Unit,
    onOpenWorkAndEdu: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    isDark: Boolean = true
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    val cardBg = if (isDark) AppTheme.card else AppTheme.card
    val txtColor = if (isDark) Color.White else AppTheme.text
    val subTxtColor = if (isDark) Color.Gray else AppTheme.textSecondary
    val menuBg = if (isDark) AppTheme.card else AppTheme.card
    val menuTxt = if (isDark) Color.White else AppTheme.text
    val borderClr = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderClr),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onSelectToEdit() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = if (isDark) Color.LightGray else AppTheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(resume.name, color = txtColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = (resume.jobTitle.ifBlank { "Unassigned Title" }) + " | " + resume.templateId.uppercase(),
                    color = subTxtColor,
                    fontSize = 11.sp
                )
                
                // Show indicators of inputs filled!
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val entriesCount = listOf(
                        resume.experience.size to "Jobs",
                        resume.skills.size to "Skills",
                        resume.projects.size to "Projects",
                        resume.education.size to "Edu",
                        resume.certifications.size to "Certs",
                        resume.languages.size to "Langs"
                    ).filter { it.first > 0 }
                    
                    if (entriesCount.isEmpty()) {
                        Text("Draft Empty", color = Color.Gray, fontSize = 9.sp)
                    } else {
                        entriesCount.take(4).forEach { (count, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) AppTheme.border else AppTheme.card)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$count $label",
                                    color = if (isDark) Color.LightGray else Color.DarkGray,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF30D158).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${resume.atsScore} ATS",
                    color = Color(0xFF30D158),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box {
                IconButton(onClick = { dropdownExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = subTxtColor)
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .background(menuBg)
                        .border(1.dp, if (isDark) Color.Transparent else AppTheme.shadowDark.copy(alpha = 0.25f))
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Draft", color = menuTxt, fontSize = 12.sp) },
                        onClick = { dropdownExpanded = false; onSelectToEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = if (isDark) Color.White else Color.Black) }
                    )
                    DropdownMenuItem(
                        text = { Text("Work & Education", color = menuTxt, fontSize = 12.sp) },
                        onClick = { dropdownExpanded = false; onOpenWorkAndEdu() },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = AppTheme.primary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate Clone", color = menuTxt, fontSize = 12.sp) },
                        onClick = { dropdownExpanded = false; onDuplicate() },
                        leadingIcon = { Icon(Icons.Default.CopyAll, contentDescription = null, tint = if (isDark) Color.White else Color.Black) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Permanent", color = Color.Red, fontSize = 12.sp) },
                        onClick = { dropdownExpanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}
