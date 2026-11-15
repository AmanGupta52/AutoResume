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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(viewModel: MainViewModel, onNavigateNext: () -> Unit) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    
    // Smooth fade-in logo animation
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f, 
                animationSpec = tween(durationMillis = 1000, easing = EaseInOutCubic)
            )
        }
    }

    val bgColorStart = if (isDark) AppTheme.background else AppTheme.background
    val bgColorEnd = if (isDark) AppTheme.card else AppTheme.card
    val textClr = AppTheme.text
    val subTextClr = AppTheme.textSecondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColorStart, bgColorEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Premium Gradient/Glass Badge with Scale/Alpha transitions
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        alpha = alpha.value
                    )
                    .shadow(16.dp, shape = RoundedCornerShape(28.dp), clip = false)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppTheme.primary, AppTheme.primary)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "AutoResume",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = textClr,
                letterSpacing = 1.5.sp,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Create Professional Resumes Instantly",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = subTextClr,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer(alpha = alpha.value)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Pulsing Loader indicator
            val pulse = rememberInfiniteTransition(label = "pulse")
            val pScale by pulse.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_animation"
            )

            Button(
                onClick = onNavigateNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer(scaleX = pScale, scaleY = pScale)
                    .shadow(12.dp, shape = RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppTheme.primary, AppTheme.primary)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("splash_start_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Get Started", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. ONBOARDING (3 Slides)
// ==========================================
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onNavigateNext: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) AppTheme.background else AppTheme.background
    val textClr = AppTheme.text
    val subTextClr = AppTheme.textSecondary
    val dotInactiveClr = if (isDark) AppTheme.border else AppTheme.shadowDark
    
    val onboardingData = listOf(
        OnboardingData(
            title = "Build Professional Resumes",
            description = "Choose from or generate selected modern templates optimized for applicant tracking systems (ATS).",
            icon = Icons.Default.Style,
            gradientColors = listOf(AppTheme.primary, AppTheme.primary)
        ),
        OnboardingData(
            title = "Choose Premium Templates",
            description = "Generate robust professional summaries and experience Work records optimized with metrics automatically.",
            icon = Icons.Default.Psychology,
            gradientColors = listOf(AppTheme.primary, AppTheme.accent)
        ),
        OnboardingData(
            title = "Download & Share Anywhere",
            description = "Download pristine, modern PDFs with crisp formatting or share portable resume links instantly.",
            icon = Icons.Default.QueryStats,
            gradientColors = listOf(AppTheme.accent, AppTheme.primary)
        )
    )
    
    val activeSlide = onboardingData[currentPage]
    
    // Smooth transition between slides
    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }.using(
                SizeTransform(clip = false)
            )
        },
        label = "onboarding_animation"
    ) { page ->
        val slide = onboardingData[page]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Skip Button at top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateNext) {
                        Text("Skip", color = subTextClr, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                // Center Illustration section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .shadow(24.dp, shape = CircleShape, clip = false)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        slide.gradientColors[0].copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .background(
                                    Brush.linearGradient(slide.gradientColors),
                                    shape = CircleShape
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Text(
                        text = slide.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textClr,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = slide.description,
                        fontSize = 15.sp,
                        color = subTextClr,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Controls at bottom
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Smooth pagination dots using animates
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..2) {
                            val isActive = i == page
                            val width by animateDpAsState(
                                targetValue = if (isActive) 24.dp else 8.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dot_width"
                            )
                            val color = if (isActive) slide.gradientColors[0] else dotInactiveClr
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(8.dp)
                                    .width(width)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Call to Action
                    Button(
                        onClick = {
                            if (currentPage < 2) {
                                currentPage++
                            } else {
                                onNavigateNext()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(12.dp, shape = RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(slide.gradientColors),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentPage == 2) "Finish & Start" else "Next",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

data class OnboardingData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

// ==========================================
// 3. LOGIN SCREEN
// ==========================================
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("amangupta032005@gmail.com") }
    var password by remember { mutableStateOf("pass1234") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var toggleBio by remember { mutableStateOf(true) }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColorStart = if (isDark) AppTheme.background else AppTheme.background
    val bgColorEnd = if (isDark) AppTheme.card else AppTheme.card
    val textClr = AppTheme.text
    val subTextClr = AppTheme.textSecondary
    val borderClr = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f)
    val glassCardBg = if (isDark) AppTheme.card.copy(alpha = 0.85f) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColorStart, bgColorEnd)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = glassCardBg),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Elegant branding circle badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppTheme.primary, AppTheme.primary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Welcome Back",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textClr,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Sign in to manage your professional assets",
                    fontSize = 14.sp,
                    color = subTextClr,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Outlined Input with Custom Rounded Border & Icon
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = "" },
                    label = { Text("Email Address", color = subTextClr) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = AppTheme.primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr,
                        focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                        unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = "" },
                    label = { Text("Password", color = subTextClr) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.primary)
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = subTextClr
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr,
                        focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                        unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = toggleBio,
                            onCheckedChange = { toggleBio = it },
                            colors = CheckboxDefaults.colors(checkedColor = AppTheme.primary)
                        )
                        Text("Biometric Unlock", color = subTextClr, fontSize = 12.sp)
                    }
                    
                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text("Forgot Password?", color = AppTheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                if (errorMsg.isNotBlank()) {
                    Text(text = errorMsg, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // CTA Button - Custom Gradient & rounded corners
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMsg = "Please fill in all details."
                        } else {
                            viewModel.login(email, password)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, shape = RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppTheme.primary, AppTheme.primary)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .testTag("login_submit_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Or connect with", color = subTextClr, fontSize = 12.sp)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.login("google@gmail.com", "pass123") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) AppTheme.card else AppTheme.card),
                        shape = RoundedCornerShape(14.dp),
                        border = if (!isDark) BorderStroke(1.dp, borderClr) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = subTextClr, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google", color = textClr, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { viewModel.login("linkedin@gmail.com", "pass123") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) AppTheme.card else AppTheme.card),
                        shape = RoundedCornerShape(14.dp),
                        border = if (!isDark) BorderStroke(1.dp, borderClr) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContactMail, contentDescription = null, tint = AppTheme.accent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LinkedIn", color = textClr, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account?", color = subTextClr, fontSize = 13.sp)
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Register", color = AppTheme.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. REGISTER SCREEN
// ==========================================
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf(viewModel.tempRegisterName) }
    var email by remember { mutableStateOf(viewModel.tempRegisterEmail) }
    var phone by remember { mutableStateOf(viewModel.tempRegisterPhone) }
    var pass by remember { mutableStateOf(viewModel.tempRegisterPass) }
    var confirmPass by remember { mutableStateOf(viewModel.tempRegisterConfirmPass) }
    var termsChecked by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf(0) } // Rating score 0 to 4

    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColorStart = if (isDark) AppTheme.background else AppTheme.background
    val bgColorEnd = if (isDark) AppTheme.card else AppTheme.card
    val textClr = AppTheme.text
    val subTextClr = AppTheme.textSecondary
    val borderClr = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f)
    val glassCardBg = if (isDark) AppTheme.card.copy(alpha = 0.85f) else Color.White

    val strengthLabel = when (strength) {
        0 -> "Extremely Fragile"
        1 -> "Poor"
        2 -> "Medium"
        3 -> "Robust Password"
        else -> "Military Encrypted"
    }
    
    val strengthColor = when (strength) {
        0 -> Color(0xFFFF453A)
        1 -> Color(0xFFFF9F0A)
        2 -> Color(0xFFFFD60A)
        3 -> Color(0xFF64D2FF)
        else -> Color(0xFF30D158)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColorStart, bgColorEnd)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = glassCardBg),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textClr,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Join over 10,000+ job search builders",
                    fontSize = 14.sp,
                    color = subTextClr,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; viewModel.tempRegisterName = it },
                    label = { Text("Full Name", color = subTextClr) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppTheme.primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr,
                        focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                        unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.tempRegisterEmail = it },
                    label = { Text("Email Address", color = subTextClr) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = AppTheme.primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr,
                        focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                        unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; viewModel.tempRegisterPhone = it },
                    label = { Text("Phone Number", color = subTextClr) },
                    leadingIcon = {
                        Icon(Icons.Default.Call, contentDescription = null, tint = AppTheme.primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr,
                        focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                        unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = pass,
                    onValueChange = { 
                        pass = it
                        viewModel.tempRegisterPass = it
                        // Calculate pwd complexity strength
                        var rating = 0
                        if (it.length >= 6) rating++
                        if (it.any { ch -> ch.isUpperCase() }) rating++
                        if (it.any { ch -> ch.isDigit() }) rating++
                        if (it.any { ch -> !ch.isLetterOrDigit() }) rating++
                        strength = rating
                    },
                    label = { Text("Password", color = subTextClr) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.primary)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr,
                        focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                        unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )

                // Strength indicator
                if (pass.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 1..4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (i <= strength) strengthColor else borderClr)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Strength: $strengthLabel",
                            color = strengthColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it; viewModel.tempRegisterConfirmPass = it },
                    label = { Text("Confirm Password", color = subTextClr) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.primary)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr,
                        focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                        unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = termsChecked,
                        onCheckedChange = { termsChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = AppTheme.primary)
                    )
                    Text("I accept terms & safety guidelines.", color = subTextClr, fontSize = 12.sp)
                }
                
                if (errorMsg.isNotBlank()) {
                    Text(text = errorMsg, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // CTA Register Button
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                            errorMsg = "Please fill in all fields."
                        } else if (pass != confirmPass) {
                            errorMsg = "Passwords do not match."
                        } else if (!termsChecked) {
                            errorMsg = "Please accept terms."
                        } else {
                            viewModel.register()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, shape = RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppTheme.primary, AppTheme.accent)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Register Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Already registered?", color = subTextClr, fontSize = 13.sp)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Sign In", color = AppTheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. OTP VERIFICATION SCREEN
// ==========================================
@Composable
fun OtpScreen(
    viewModel: MainViewModel,
    onCancel: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val remainingSeconds by viewModel.otpTimerSeconds.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AppTheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AppTheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Verify Code",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We sent a 6-digit OTP verification code to " + (viewModel.currentUser.collectAsState().value?.email ?: "your email"),
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = code,
                onValueChange = { 
                    if (it.length <= 6) {
                        code = it
                        viewModel.tempOtpCode = it
                    }
                },
                placeholder = { Text("------", color = Color.Gray, letterSpacing = 8.sp, fontSize = 20.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AppTheme.primary,
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp, fontSize = 22.sp),
                modifier = Modifier.width(180.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (remainingSeconds > 0) "Resend code in ${remainingSeconds}s" else "Didn't receive code?",
                fontSize = 13.sp,
                color = Color.Gray
            )
            
            if (remainingSeconds == 0) {
                TextButton(onClick = { viewModel.otpTimerSeconds.value = 60 }) {
                    Text("Resend Code", color = AppTheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (code.length == 6) {
                        viewModel.verifyOtp()
                    }
                },
                enabled = code.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Verify & Continue", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.Gray)
            }
        }
    }
}

// ==========================================
// 6. FORGOT PASSWORD SCREEN
// ==========================================
@Composable
fun ForgotPasswordScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf(viewModel.tempResetEmail) }
    var sentLink by remember { mutableStateOf(false) }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) AppTheme.background else AppTheme.card
    val textClr = AppTheme.text
    val subTextClr = if (isDark) Color.Gray else AppTheme.textSecondary
    val borderClr = if (isDark) Color.DarkGray else AppTheme.shadowDark
    val cardColor = if (isDark) AppTheme.background else AppTheme.card

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = AppTheme.primary,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Forgot Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textClr
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Provide your corporate/personal email. We will dispatch a secure reset OTP authorization key.",
                fontSize = 14.sp,
                color = subTextClr,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!sentLink) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.tempResetEmail = it },
                    label = { Text("Account Registered Email", color = subTextClr) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textClr,
                        unfocusedTextColor = textClr,
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = borderClr
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (email.isNotBlank()) {
                            sentLink = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Send Recovery Code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    border = if (!isDark) BorderStroke(1.dp, borderClr) else null,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recovery Code Despatched!", color = textClr, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Check your spam folder if it doesn't arrive within 60 seconds.", color = subTextClr, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.navState.value = "reset_password" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go to Reset Password", color = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onNavigateBack) {
                Text("Back to Sign In", color = subTextClr)
            }
        }
    }
}

// ==========================================
// 7. RESET PASSWORD SCREEN
// ==========================================
@Composable
fun ResetPasswordScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf(0) } // 0 to 4
    
    // Calculate simple dynamic security credentials rating
    val strengthLabel = when (strength) {
        0 -> "Extremely Fragile"
        1 -> "Poor"
        2 -> "Medium"
        3 -> "Robust Password"
        else -> "Military Encrypted"
    }
    
    val strengthColor = when (strength) {
        0 -> Color.Red
        1 -> Color(0xFFFF5252)
        2 -> Color(0xFFFFC107)
        3 -> Color(0xFF2196F3)
        else -> Color(0xFF4CAF50)
    }

    val isDark by viewModel.isDarkTheme.collectAsState()
    val bgColor = if (isDark) AppTheme.background else AppTheme.card
    val textClr = AppTheme.text
    val subTextClr = if (isDark) Color.Gray else AppTheme.textSecondary
    val borderClr = if (isDark) Color.DarkGray else AppTheme.shadowDark

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = Color(0xFF30D158),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Reset Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textClr
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = pass,
                onValueChange = { 
                    pass = it
                    // calculate strength metrics
                    var rating = 0
                    if (it.length >= 6) rating++
                    if (it.any { ch -> ch.isUpperCase() }) rating++
                    if (it.any { ch -> ch.isDigit() }) rating++
                    if (it.any { ch -> !ch.isLetterOrDigit() }) rating++
                    strength = rating
                },
                label = { Text("New Secure Password", color = subTextClr) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textClr,
                    unfocusedTextColor = textClr,
                    focusedBorderColor = AppTheme.primary,
                    unfocusedBorderColor = borderClr
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Password strength bar indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i <= strength) strengthColor else borderClr)
                    )
                }
            }
            Text("Rating: $strengthLabel", color = strengthColor, fontSize = 11.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPass,
                onValueChange = { confirmPass = it },
                label = { Text("Confirm New Password", color = subTextClr) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textClr,
                    unfocusedTextColor = textClr,
                    focusedBorderColor = AppTheme.primary,
                    unfocusedBorderColor = borderClr
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Button(
                onClick = {
                    if (pass.length >= 6 && pass == confirmPass) {
                        viewModel.currentUser.value = UserSession("Alex Mercer", "alex@company.com", "+12345", true)
                        viewModel.navState.value = "dashboard"
                    }
                },
                enabled = pass.length >= 6 && pass == confirmPass,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Confirm Password", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onNavigateBack) {
                Text("Cancel", color = Color.Gray)
            }
        }
    }
}

// ==========================================
// SETUP USER PROFILE SCREEN (FIRST TIME INITIATOR)
// ==========================================
// ==========================================
// SETUP USER PROFILE SCREEN (FIRST TIME INITIATOR)
// ==========================================
@Composable
fun SetupProfileScreen(
    viewModel: MainViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val textClr = AppTheme.text
    val subTextClr = AppTheme.textSecondary
    val borderClr = if (isDark) AppTheme.border else AppTheme.shadowDark.copy(alpha = 0.25f)
    val currentSession = viewModel.currentUser.collectAsState().value

    var fullName by remember { mutableStateOf(currentSession?.fullName ?: "") }
    var jobTitle by remember { mutableStateOf(currentSession?.jobTitle ?: "") }
    var email by remember { mutableStateOf(currentSession?.email ?: "") }
    var phone by remember { mutableStateOf(currentSession?.phone ?: "") }
    var location by remember { mutableStateOf(currentSession?.location ?: "") }
    
    // Choose profile gradient avatar index
    val avatars = listOf(
        "gradient_blue" to listOf(AppTheme.primary, AppTheme.primary),
        "gradient_purple" to listOf(AppTheme.primary, Color(0xFFD422EF)),
        "gradient_teal" to listOf(AppTheme.accent, Color(0xFF0D9488)),
        "gradient_orange" to listOf(Color(0xFFFF9500), Color(0xFFFF2D55)),
        "gradient_pink" to listOf(Color(0xFFFF2D55), AppTheme.primary)
    )
    
    var selectedAvatarKey by remember { mutableStateOf(currentSession?.profilePhotoUrl?.ifBlank { "gradient_blue" } ?: "gradient_blue") }
    var showErrors by remember { mutableStateOf(false) }

    val bgColorStart = if (isDark) AppTheme.background else AppTheme.background
    val bgColorEnd = if (isDark) AppTheme.card else AppTheme.card
    val glassCardBg = if (isDark) AppTheme.card.copy(alpha = 0.85f) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColorStart, bgColorEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Text(
                text = "Let's Build Your Workspace",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textClr,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "These details serve as the base core metadata for instantly generating and updating your resumes.",
                fontSize = 14.sp,
                color = subTextClr,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Profile Picture Option Container
            Card(
                colors = CardDefaults.cardColors(containerColor = glassCardBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Customize Profile Badge",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textClr,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Selected avatar preview circle
                    val activeColors = avatars.firstOrNull { it.first == selectedAvatarKey }?.second ?: avatars[0].second
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .shadow(12.dp, shape = CircleShape)
                            .background(Brush.linearGradient(activeColors), shape = CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (fullName.isNotBlank()) fullName.take(1).uppercase() else "R",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Choose Visual Theme Accent",
                        fontSize = 12.sp,
                        color = subTextClr,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatars.forEach { pair ->
                            val keys = pair.first
                            val colors = pair.second
                            val isSelected = keys == selectedAvatarKey
                            val animatedScale by animateFloatAsState(if (isSelected) 1.15f else 0.95f, label = "avatar_pref")

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                                    .background(Brush.linearGradient(colors), CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) textClr else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatarKey = keys }
                            )
                        }
                    }
                }
            }
            
            // Standard text input cards with custom premium paddings
            Card(
                colors = CardDefaults.cardColors(containerColor = glassCardBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Full Name Input
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name", color = subTextClr) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AppTheme.primary) },
                        isError = showErrors && fullName.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textClr,
                            unfocusedTextColor = textClr,
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = borderClr,
                            errorBorderColor = Color.Red,
                            focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                            unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp).testTag("setup_full_name_input")
                    )
                    if (showErrors && fullName.isBlank()) {
                        Text("Full name is required", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    // Job Title Input
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title (e.g. Lead Developer)", color = subTextClr) },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = AppTheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textClr,
                            unfocusedTextColor = textClr,
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = borderClr,
                            focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                            unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp).testTag("setup_job_title_input")
                    )
                    
                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = subTextClr) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AppTheme.primary) },
                        isError = showErrors && (email.isBlank() || !email.contains("@")),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textClr,
                            unfocusedTextColor = textClr,
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = borderClr,
                            errorBorderColor = Color.Red,
                            focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                            unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp).testTag("setup_email_input")
                    )
                    if (showErrors && email.isBlank()) {
                        Text("Email is required", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                    } else if (showErrors && !email.contains("@")) {
                        Text("Please enter a valid email address", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    // Phone Input
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number", color = subTextClr) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AppTheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textClr,
                            unfocusedTextColor = textClr,
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = borderClr,
                            focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                            unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp).testTag("setup_phone_input")
                    )
                    
                    // Location Input
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (e.g. SF, CA)", color = subTextClr) },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = AppTheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textClr,
                            unfocusedTextColor = textClr,
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = borderClr,
                            focusedContainerColor = if (isDark) AppTheme.card else AppTheme.card,
                            unfocusedContainerColor = if (isDark) AppTheme.card else AppTheme.card
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp).testTag("setup_location_input")
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Submit Button
            Button(
                onClick = {
                    if (fullName.isNotBlank() && email.isNotBlank() && email.contains("@")) {
                        viewModel.saveUserSession(
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            jobTitle = jobTitle,
                            location = location,
                            profilePhotoUrl = selectedAvatarKey
                        )
                        viewModel.navState.value = "dashboard"
                    } else {
                        showErrors = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppTheme.primary, AppTheme.primary)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("setup_profile_submit_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            if (onNavigateBack != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateBack) {
                    Text("Go Back to Dashboard", color = subTextClr, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

