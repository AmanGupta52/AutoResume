package com.example

import com.example.ui.theme.AppTheme
import com.example.ui.locale.LocaleManager
import com.example.ui.locale.tr
import com.example.ui.locale.Text

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
// import androidx.compose.material3.Text
import com.example.ui.locale.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.SplashScreen
import com.example.ui.OnboardingScreen
import com.example.ui.LoginScreen
import com.example.ui.RegisterScreen
import com.example.ui.OtpScreen
import com.example.ui.ForgotPasswordScreen
import com.example.ui.ResetPasswordScreen
import com.example.ui.HomeDashboardScreen
import com.example.ui.AllResumesScreen
import com.example.ui.ResumeWizardScreen
import com.example.ui.WorkAndEducationEditorScreen
import com.example.ui.ResumePreviewScreen
import com.example.ui.AtsCheckerScreen
import com.example.ui.ProfileAndSettingsScreen
import com.example.ui.SetupProfileScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Setup Room Database locally
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "autoresume-sqlite3-db"
        ).fallbackToDestructiveMigration().build()
        
        val resumeDao = db.resumeDao()
        
        // Setup central VM Factor
        val factory = MainViewModelFactory(resumeDao, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme(darkTheme = viewModel.isDarkTheme.collectAsState().value) {
                AutoResumeAppContent(viewModel)
            }
        }
    }
}

@Composable
fun AutoResumeAppContent(viewModel: MainViewModel) {
    val navState by viewModel.navState.collectAsState()
    val activeTab by viewModel.bottomTabSelected.collectAsState()

    when (navState) {
        "splash" -> {
            SplashScreen(viewModel = viewModel) {
                viewModel.navState.value = "onboarding"
            }
        }
        "setup_profile" -> {
            SetupProfileScreen(
                viewModel = viewModel,
                onNavigateBack = if (viewModel.currentUser.value != null) { { viewModel.navState.value = "dashboard" } } else null
            )
        }
        "onboarding" -> {
            OnboardingScreen(viewModel = viewModel) {
                viewModel.setOnboardingCompleted()
                if (viewModel.currentUser.value != null) {
                    viewModel.navState.value = "dashboard"
                } else {
                    viewModel.navState.value = "setup_profile"
                }
            }
        }
        "login" -> {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { viewModel.navState.value = "register" },
                onNavigateToForgotPassword = { viewModel.navState.value = "forgot_password" }
            )
        }
        "register" -> {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { viewModel.navState.value = "login" }
            )
        }
        "otp" -> {
            OtpScreen(
                viewModel = viewModel,
                onCancel = { viewModel.navState.value = "register" }
            )
        }
        "forgot_password" -> {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navState.value = "login" }
            )
        }
        "reset_password" -> {
            ResetPasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navState.value = "login" }
            )
        }
        "editor" -> {
            ResumeWizardScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navState.value = "dashboard" },
                onGoToPreview = { viewModel.navState.value = "preview" }
            )
        }
        "work_edu_editor" -> {
            WorkAndEducationEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navState.value = "dashboard" },
                onGoToPreview = { viewModel.navState.value = "preview" }
            )
        }
        "preview" -> {
            ResumePreviewScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navState.value = "editor" },
                onGoToAts = { viewModel.navState.value = "dashboard"; viewModel.bottomTabSelected.value = 2 }
            )
        }
        else -> {
            // "dashboard" screen hosting core modular tabs inside navigation bar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = AppTheme.card,
                        modifier = Modifier.testTag("app_bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { viewModel.bottomTabSelected.value = 0 },
                            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0A84FF),
                                selectedTextColor = Color(0xFF0A84FF),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = AppTheme.card
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { viewModel.bottomTabSelected.value = 1 },
                            icon = { Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Resumes") },
                            label = { Text("Resumes", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0A84FF),
                                selectedTextColor = Color(0xFF0A84FF),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = AppTheme.card
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == 2,
                            onClick = { viewModel.bottomTabSelected.value = 2 },
                            icon = { Icon(imageVector = Icons.Default.InsertChart, contentDescription = "ATS") },
                            label = { Text("ATS Score", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0A84FF),
                                selectedTextColor = Color(0xFF0A84FF),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = AppTheme.card
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == 3,
                            onClick = { viewModel.bottomTabSelected.value = 3 },
                            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0A84FF),
                                selectedTextColor = Color(0xFF0A84FF),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = AppTheme.card
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (activeTab) {
                        0 -> HomeDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAts = { viewModel.bottomTabSelected.value = 2 },
                            onNavigateToProfile = { viewModel.bottomTabSelected.value = 3 },
                            onNavigateToSettings = { viewModel.bottomTabSelected.value = 3 }
                        )
                        1 -> AllResumesScreen(
                            viewModel = viewModel,
                            onNavigateToAts = { viewModel.bottomTabSelected.value = 2 },
                            onNavigateToProfile = { viewModel.bottomTabSelected.value = 3 }
                        )
                        2 -> AtsCheckerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.bottomTabSelected.value = 0 }
                        )
                        else -> ProfileAndSettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.bottomTabSelected.value = 0 }
                        )
                    }
                }
            }
        }
    }
}
