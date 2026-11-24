package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.api.BackendClient
import com.example.data.AppDatabase
import com.example.ui.*
import com.example.ui.Text
import com.example.ui.theme.LocalGlass
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "autoresume-sqlite3-db")
            .fallbackToDestructiveMigration()
            .build()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ResumeViewModel(db.resumeDao(), applicationContext) as T
        }
        val vm = ViewModelProvider(this, factory)[ResumeViewModel::class.java]

        setContent {
            val mode by vm.themeMode.collectAsState()
            // "oled" is the dark theme on pure black; "system" follows the phone (normal dark, never OLED)
            val dark = when (mode) { "dark", "oled" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            MyApplicationTheme(darkTheme = dark, oled = mode == "oled") {
                // status and navigation bar icons must follow the app theme, not only the phone theme
                val view = LocalView.current
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !dark
                    controller.isAppearanceLightNavigationBars = !dark
                }
                AppRoot(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: ResumeViewModel) {
    val screen by vm.screen.collectAsState()
    val lang by vm.language.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { m ->
            vm.message.value = null
            val res = snackbar.showSnackbar(m.text, m.actionLabel, duration = if (m.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (res == SnackbarResult.ActionPerformed) m.onAction?.invoke()
        }
    }
    BackHandler(enabled = screen == Screen.EDITOR || screen == Screen.PREVIEW || screen == Screen.GUIDED) {
        when (screen) {
            Screen.PREVIEW -> vm.go(Screen.EDITOR)
            Screen.GUIDED -> if (vm.guidedStep.value > 0) vm.guidedStep.value -= 1 else vm.go(Screen.EDITOR)
            else -> vm.closeEditor()
        }
    }

    // with a backend configured, the app needs an account; without one it keeps working offline as before
    val account by vm.auth.collectAsState()
    val parallax = rememberParallax()
    if (BackendClient.isConfigured && account == null) {
        AuroraBackground(parallax = parallax) { AuthScreen(vm) }
        return
    }
    val isTab = screen == Screen.RESUMES || screen == Screen.ATS || screen == Screen.LETTER || screen == Screen.SETTINGS
    val glass = LocalGlass.current
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = if (glass.dark) .28f else .18f),
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Aurora mesh behind everything; screens stay transparent so their glass cards sit on it
    AuroraBackground(parallax = parallax) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (isTab) NavigationBar(
                    containerColor = glass.barFill,
                    tonalElevation = 0.dp,
                    modifier = Modifier.glassBarEdge(),
                ) {
                    NavigationBarItem(screen == Screen.RESUMES, { vm.go(Screen.RESUMES) }, { Icon(Icons.Filled.Description, null) }, label = { Text("Resumes") }, colors = navColors)
                    NavigationBarItem(screen == Screen.ATS, { vm.go(Screen.ATS) }, { Icon(Icons.Filled.QueryStats, null) }, label = { Text("ATS check") }, colors = navColors)
                    NavigationBarItem(screen == Screen.LETTER, { vm.go(Screen.LETTER) }, { Icon(Icons.Filled.Edit, null) }, label = { Text("Cover letter") }, colors = navColors)
                    NavigationBarItem(screen == Screen.SETTINGS, { vm.go(Screen.SETTINGS) }, { Icon(Icons.Filled.Settings, null) }, label = { Text("Settings") }, colors = navColors)
                }
            },
        ) { pad ->
            Box(Modifier.padding(pad).imePadding()) {
                when (screen) {
                    Screen.RESUMES -> ResumesScreen(vm)
                    Screen.ATS -> AtsScreen(vm)
                    Screen.LETTER -> CoverLetterScreen(vm)
                    Screen.SETTINGS -> SettingsScreen(vm)
                    Screen.EDITOR -> EditorScreen(vm)
                    Screen.PREVIEW -> PreviewScreen(vm)
                    Screen.GUIDED -> GuidedScreen(vm)
                }
            }
        }
    }

    // AI layers that can appear over any screen
    AiProposalSheet(vm)
    AiChatSheet(vm)
}
