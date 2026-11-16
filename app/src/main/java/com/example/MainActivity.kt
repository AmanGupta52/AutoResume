package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.ui.*
import com.example.ui.Text
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
            val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            MyApplicationTheme(darkTheme = dark) { AppRoot(vm) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: ResumeViewModel) {
    val screen by vm.screen.collectAsState()
    val onboarding by vm.showOnboarding.collectAsState()
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
    BackHandler(enabled = screen == Screen.EDITOR || screen == Screen.PREVIEW) {
        if (screen == Screen.PREVIEW) vm.go(Screen.EDITOR) else vm.closeEditor()
    }

    if (onboarding) {
        OnboardingScreen(vm)
        return
    }
    val isTab = screen == Screen.RESUMES || screen == Screen.ATS || screen == Screen.LETTER || screen == Screen.SETTINGS
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (isTab) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(screen == Screen.RESUMES, { vm.go(Screen.RESUMES) }, { Icon(Icons.Filled.Description, null) }, label = { Text("Resumes") })
                NavigationBarItem(screen == Screen.ATS, { vm.go(Screen.ATS) }, { Icon(Icons.Filled.QueryStats, null) }, label = { Text("ATS check") })
                NavigationBarItem(screen == Screen.LETTER, { vm.go(Screen.LETTER) }, { Icon(Icons.Filled.Edit, null) }, label = { Text("Cover letter") })
                NavigationBarItem(screen == Screen.SETTINGS, { vm.go(Screen.SETTINGS) }, { Icon(Icons.Filled.Settings, null) }, label = { Text("Settings") })
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
            }
        }
    }
}
