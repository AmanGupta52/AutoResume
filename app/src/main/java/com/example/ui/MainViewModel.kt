package com.example.ui

import com.example.ui.theme.AppTheme
import com.example.ui.locale.LocaleManager
import com.example.ui.locale.tr
import com.example.ui.locale.Text

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.EducationEntry
import com.example.data.ExperienceEntry
import com.example.data.LanguageEntry
import com.example.data.ProjectEntry
import com.example.data.Resume
import com.example.data.ResumeDao
import com.example.data.SkillEntry
import com.example.data.CertificationEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room

class MainViewModel(private val resumeDao: ResumeDao, private val context: Context) : ViewModel() {

    // Auth state parameters
    var tempRegisterName = ""
    var tempRegisterEmail = ""
    var tempRegisterPhone = ""
    var tempRegisterPass = ""
    var tempRegisterConfirmPass = ""
    var tempOtpCode = ""
    var tempResetEmail = ""
    var tempResetPass = ""
    var otpTimerSeconds = MutableStateFlow(60)

    // Current navigation screens
    // "splash", "onboarding", "login", "register", "otp", "forgot_password", "reset_password", "dashboard"
    val navState = MutableStateFlow("splash")
    
    // Bottom navigation index selector (0: Home, 1: Resumes, 2: ATS, 3: Portfolio, 4: Profile)
    val bottomTabSelected = MutableStateFlow(0)

    // Current logged in user model state
    val currentUser = MutableStateFlow<UserSession?>(null)

    // Room Database Resumes
    val resumes: StateFlow<List<Resume>> = resumeDao.getAllResumes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Editing Resume Wizard states
    val activeResume = MutableStateFlow(Resume())
    val activeStep = MutableStateFlow(1) // step 1 to 10
    
    // ATS checker parameters
    val atsSelectedResumeId = MutableStateFlow<Int?>(null)
    val atsUserInputText = MutableStateFlow("")
    val atsJobDescription = MutableStateFlow("")
    val isAtsAnalyzing = MutableStateFlow(false)
    val atsScanResult = MutableStateFlow<AtsScanData?>(null)

    // Alerts/Notifications List
    val notifications = MutableStateFlow(listOf(
        NotificationData("Resume Optimized!", "Your resume 'Product Designer' ATS score increased to 87%.", "Just now"),
        NotificationData("New Career Tip 💡", "Adding quantifiable metrics to your experience bullets can boost recruiter callbacks by 40%.", "2 hours ago"),
        NotificationData("Cloud Synced Completed ☁️", "All your resumes are safely encrypted and synchronized with your account profile.", "1 day ago")
    ))

    // Preferences & Settings
    val isDarkTheme = MutableStateFlow(false)
    val appLanguage = MutableStateFlow("English")
    val appAccentId = MutableStateFlow("Blue") // Blue, Green, Cyan, Slate, Dark
    val appFontSize = MutableStateFlow("Standard") // Compact, Standard, Large
    
    // AI Loading Indicators
    val isAiLoading = MutableStateFlow(false)
    val aiMessageFeedback = MutableStateFlow("")

    init {
        loadUserSession()
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        isDarkTheme.value = prefs.getBoolean("is_dark_theme", false)
        appLanguage.value = prefs.getString("app_language", "English") ?: "English"

        viewModelScope.launch {
            isDarkTheme.collect {
                AppTheme.isDarkTheme = it
                prefs.edit().putBoolean("is_dark_theme", it).apply()
            }
        }
        viewModelScope.launch {
            appLanguage.collect {
                LocaleManager.currentLang = it
                prefs.edit().putString("app_language", it).apply()
            }
        }

        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        if (onboardingCompleted) {
            if (currentUser.value != null) {
                navState.value = "dashboard"
            } else {
                navState.value = "setup_profile"
            }
        } else {
            navState.value = "splash"
        }
        
        // Run countdown timer for OTP screen simulation
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (otpTimerSeconds.value > 0) {
                    otpTimerSeconds.value -= 1
                }
            }
        }
    }

    fun setOnboardingCompleted() {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    fun loadUserSession() {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val fullName = prefs.getString("user_full_name", null)
        val email = prefs.getString("user_email", null)
        val phone = prefs.getString("user_phone", "")
        val jobTitle = prefs.getString("user_job_title", "")
        val location = prefs.getString("user_location", "")
        val isBiometricEnabled = prefs.getBoolean("user_biometric", false)
        val profilePhotoUrl = prefs.getString("user_profile_photo", "")

        if (fullName != null && email != null) {
            currentUser.value = UserSession(
                fullName = fullName,
                email = email,
                phone = phone ?: "",
                isBiometricEnabled = isBiometricEnabled,
                jobTitle = jobTitle ?: "",
                location = location ?: "",
                profilePhotoUrl = profilePhotoUrl ?: ""
            )
        } else {
            currentUser.value = null
        }
    }

    fun saveUserSession(fullName: String, email: String, phone: String, jobTitle: String, location: String = "", profilePhotoUrl: String = "") {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_full_name", fullName)
            putString("user_email", email)
            putString("user_phone", phone)
            putString("user_job_title", jobTitle)
            putString("user_location", location)
            putString("user_profile_photo", profilePhotoUrl)
            apply()
        }
        currentUser.value = UserSession(
            fullName = fullName,
            email = email,
            phone = phone,
            isBiometricEnabled = false,
            jobTitle = jobTitle,
            location = location,
            profilePhotoUrl = profilePhotoUrl
        )
    }

    // Auth functions
    fun login(email: String, pass: String): Boolean {
        if (email.isNotBlank() && pass.isNotBlank()) {
            currentUser.value = UserSession(
                fullName = "Alex Mercer",
                email = email,
                phone = "+1 (555) 019-2834",
                isBiometricEnabled = true
            )
            navState.value = "dashboard"
            return true
        }
        return false
    }

    fun register(): Boolean {
        if (tempRegisterName.isNotBlank() && tempRegisterEmail.isNotBlank()) {
            currentUser.value = UserSession(
                fullName = tempRegisterName,
                email = tempRegisterEmail,
                phone = tempRegisterPhone,
                isBiometricEnabled = false
            )
            navState.value = "otp"
            otpTimerSeconds.value = 60
            return true
        }
        return false
    }

    fun verifyOtp(): Boolean {
        if (tempOtpCode.length == 6) {
            navState.value = "dashboard"
            return true
        }
        return false
    }

    fun logout() {
        viewModelScope.launch {
            resumes.value.forEach {
                resumeDao.deleteResumeById(it.id)
            }
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            currentUser.value = null
            navState.value = "setup_profile"
            bottomTabSelected.value = 0
        }
    }

    // Database CRUD Resume actions
    fun createNewResume() {
        viewModelScope.launch {
            val user = currentUser.value
            val initialResume = Resume(
                name = "My Resume ${resumes.value.size + 1}",
                fullName = user?.fullName ?: "John Doe",
                email = user?.email ?: "johndoe@example.com",
                phone = if (user?.phone?.isNotBlank() == true) user.phone else "+1 (123) 456-7890",
                location = if (user?.location?.isNotBlank() == true) user.location else "San Francisco, CA",
                jobTitle = if (user?.jobTitle?.isNotBlank() == true) user.jobTitle else "Software Engineer",
                summary = "Innovative and detail-oriented " + (if (user?.jobTitle?.isNotBlank() == true) user.jobTitle else "Software Engineer") + " with experience in designing and developing scalable web and mobile software code architectures.",
                skills = listOf(
                    SkillEntry("Kotlin", "Expert", "Technical"),
                    SkillEntry("Jetpack Compose", "Expert", "Technical"),
                    SkillEntry("Android SDK", "Expert", "Technical"),
                    SkillEntry("Room Database", "Intermediate", "Technical"),
                    SkillEntry("Git", "Expert", "Technical"),
                    SkillEntry("UI/UX Design", "Intermediate", "Tools & Technologies")
                ),
                education = listOf(
                    EducationEntry("Stanford University", "B.S. Computer Science", "2019", "2023", "3.8/4.0 GPA", "Dean's List, Computer Club Vice President")
                ),
                experience = listOf(
                    ExperienceEntry("TechCorp", "Junior Android Engineer", "June 2023", "Present", true, "• Implemented reactive MVVM architectures, lowering production latency by 15%.\n• Collaborated with core UI designers to roll out sleek user-facing menus with Material 3 ripples.\n• Reduced build integration sizes by cleaning up dead assemblies."),
                    ExperienceEntry("DevGroup", "Android Developer Intern", "June 2022", "Aug 2022", false, "• Assessed and patched critical memory leaks inside the SQLite database driver classes.\n• Formulated 10+ core unit-tests verifying background services.")
                ),
                projects = listOf(
                    ProjectEntry("TaskSync App", "Kotlin, Compose, Room", "A task collaboration tracker enabling multi-device syncing offline.", "github.com/alex/tasksync", "tasksync.live")
                ),
                languages = listOf(
                    LanguageEntry("English", "Native"),
                    LanguageEntry("Spanish", "Basic")
                ),
                atsScore = 78,
                atsFeedback = "Solid keyword alignment. Consider accentuating quantitative work metrics inside bullet descriptions to hit 85+ score."
            )
            val generatedId = resumeDao.insertResume(initialResume)
            // Load this resume as actively editing
            activeResume.value = initialResume.copy(id = generatedId.toInt())
            activeStep.value = 1
            navState.value = "editor"
        }
    }

    fun selectResumeForEditing(resume: Resume) {
        activeResume.value = resume
        activeStep.value = 1
        navState.value = "editor"
    }

    fun openWorkAndEducationEditor(resume: Resume? = null) {
        val target = resume ?: if (activeResume.value.id != 0) activeResume.value else resumes.value.firstOrNull()
        if (target != null) {
            activeResume.value = target
            navState.value = "work_edu_editor"
        } else {
            viewModelScope.launch {
                val user = currentUser.value
                val initialResume = Resume(
                    name = "My Resume 1",
                    fullName = user?.fullName ?: "John Doe",
                    email = user?.email ?: "johndoe@example.com",
                    phone = if (user?.phone?.isNotBlank() == true) user.phone else "+1 (123) 456-7890",
                    location = if (user?.location?.isNotBlank() == true) user.location else "San Francisco, CA",
                    jobTitle = if (user?.jobTitle?.isNotBlank() == true) user.jobTitle else "Software Engineer",
                    education = listOf(
                        EducationEntry("Stanford University", "B.S. Computer Science", "2019", "2023", "3.8/4.0 GPA", "• Dean's List\n• Relevant Coursework: Algorithms, Mobile Systems")
                    ),
                    experience = listOf(
                        ExperienceEntry("TechCorp", "Software Engineer", "June 2023", "Present", true, "• Implemented reactive MVVM architectures, lowering production latency by 15%.\n• Built Jetpack Compose UI workflows with Material 3.")
                    )
                )
                val generatedId = resumeDao.insertResume(initialResume)
                activeResume.value = initialResume.copy(id = generatedId.toInt())
                navState.value = "work_edu_editor"
            }
        }
    }

    fun saveActiveResume() {
        viewModelScope.launch {
            val r = activeResume.value.copy(lastModified = System.currentTimeMillis())
            resumeDao.insertResume(r)
        }
    }

    fun duplicateResume(resume: Resume) {
        viewModelScope.launch {
            val cloned = resume.copy(
                id = 0, // inserts as new
                name = "${resume.name} (Copy)",
                lastModified = System.currentTimeMillis()
            )
            resumeDao.insertResume(cloned)
        }
    }

    fun deleteResume(id: Int) {
        viewModelScope.launch {
            resumeDao.deleteResumeById(id)
        }
    }

    fun renameResume(resume: Resume, newName: String) {
        viewModelScope.launch {
            val updated = resume.copy(name = newName, lastModified = System.currentTimeMillis())
            resumeDao.insertResume(updated)
        }
    }

    // AI Assist tasks utilizing Gemini REST
    fun triggerAiSummaryGeneration() {
        viewModelScope.launch {
            isAiLoading.value = true
            aiMessageFeedback.value = "Crafting professional narrative with Gemini 3.5-Flash..."
            val skillsText = activeResume.value.skills.joinToString { it.name }
            val expCompanyText = activeResume.value.experience.joinToString { "${it.role} at ${it.company}" }
            
            val generated = GeminiClient.generateSummary(
                jobTitle = activeResume.value.jobTitle,
                skills = if (skillsText.isBlank()) "software engineering" else skillsText,
                experience = if (expCompanyText.isBlank()) "various design/engineering projects" else expCompanyText
            )
            
            activeResume.value = activeResume.value.copy(summary = generated)
            saveActiveResume()
            
            isAiLoading.value = false
            aiMessageFeedback.value = "Summary synchronized!"
        }
    }

    fun triggerAiExpBulletSuggester(expIndex: Int, details: String) {
        viewModelScope.launch {
            val currentExpList = activeResume.value.experience.toMutableList()
            if (expIndex < 0 || expIndex >= currentExpList.size) return@launch
            
            isAiLoading.value = true
            aiMessageFeedback.value = "Consulting AI bullet writer for metrics enhancements..."
            val exp = currentExpList[expIndex]
            
            val bullets = GeminiClient.generateBulletPoints(
                role = exp.role,
                company = exp.company,
                details = details
            )
            
            currentExpList[expIndex] = exp.copy(description = bullets)
            activeResume.value = activeResume.value.copy(experience = currentExpList)
            saveActiveResume()
            
            isAiLoading.value = false
            aiMessageFeedback.value = "Bullets generated with metrics!"
        }
    }

    fun triggerAiSkillsSuggester() {
        viewModelScope.launch {
            isAiLoading.value = true
            aiMessageFeedback.value = "Gemini gathering industry skill standard terms..."
            val role = activeResume.value.jobTitle
            val skillsString = GeminiClient.generateSkillSuggestions(role)
            
            val suggestions = skillsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val currentSkills = activeResume.value.skills.toMutableList()
            
            suggestions.forEach { skillName ->
                if (!currentSkills.any { it.name.equals(skillName, ignoreCase = true) }) {
                    currentSkills.add(SkillEntry(skillName, "Intermediate", "Technical"))
                }
            }
            
            activeResume.value = activeResume.value.copy(skills = currentSkills)
            saveActiveResume()
            isAiLoading.value = false
            aiMessageFeedback.value = "Relevant skills suggested!"
        }
    }

    fun analyzeResumeTextAgainstJd(
        resumeText: String,
        jobDescriptionText: String,
        presetKeywords: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            isAtsAnalyzing.value = true
            val jd = jobDescriptionText.ifBlank { AtsAnalyzerEngine.commonJobDescriptions.first().description }
            val result = AtsAnalyzerEngine.analyzeText(
                resumeText = resumeText,
                jobDescription = jd,
                presetKeywords = presetKeywords
            )
            atsScanResult.value = result

            // Update selected saved resume's ATS score if user loaded one
            val selectedId = atsSelectedResumeId.value
            if (selectedId != null) {
                val matchedResume = resumes.value.find { it.id == selectedId }
                if (matchedResume != null) {
                    resumeDao.updateResume(
                        matchedResume.copy(
                            atsScore = result.overallScore,
                            lastModified = System.currentTimeMillis()
                        )
                    )
                }
            }

            val newList = notifications.value.toMutableList()
            newList.add(
                0,
                NotificationData(
                    "ATS Analysis Complete",
                    "Your text scored ${result.overallScore}% (${result.foundKeywords.size} matched, ${result.missingKeywords.size} missing keywords).",
                    "Just now"
                )
            )
            notifications.value = newList
            isAtsAnalyzing.value = false
        }
    }

    fun runAtsCheck(resume: Resume, jobDescriptionText: String) {
        atsSelectedResumeId.value = resume.id
        val plainText = AtsAnalyzerEngine.resumeToPlainText(resume)
        atsUserInputText.value = plainText
        analyzeResumeTextAgainstJd(
            resumeText = plainText,
            jobDescriptionText = jobDescriptionText
        )
    }

    fun optimizeUserTextWithAi(
        userText: String,
        jobDescription: String,
        presetKeywords: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            isAiLoading.value = true
            aiMessageFeedback.value = "Optimizing text for missing ATS keywords..."

            val currentAnalysis = AtsAnalyzerEngine.analyzeText(
                resumeText = userText,
                jobDescription = jobDescription,
                presetKeywords = presetKeywords
            )
            val missing = currentAnalysis.missingKeywords

            val aiResponse = GeminiClient.autoFixResume(userText, jobDescription)
            val isFallbackMock = aiResponse.startsWith("Optimized with tailored keywords") ||
                aiResponse.startsWith("Failed to connect to AI server")

            val improvedText = if (!isFallbackMock && aiResponse.length > 40) {
                aiResponse
            } else if (missing.isNotEmpty()) {
                val topMissing = missing.take(5)
                val addition = "\n\nCore Competencies & Tools:\n• Proficient in ${topMissing.joinToString(", ")} across production projects."
                userText.trimEnd() + addition
            } else {
                userText
            }

            atsUserInputText.value = improvedText
            atsScanResult.value = AtsAnalyzerEngine.analyzeText(
                resumeText = improvedText,
                jobDescription = jobDescription,
                presetKeywords = presetKeywords
            )

            isAiLoading.value = false
            aiMessageFeedback.value = "Resume text optimized for ATS keywords!"
        }
    }

    fun autoImproveAtsWithAi(resume: Resume) {
        viewModelScope.launch {
            isAiLoading.value = true
            aiMessageFeedback.value = "Rewriting resume summary for ATS keywords matching..."
            
            val jd = atsJobDescription.value.ifBlank { "Target engineering manager with communication and system leadership focus." }
            val improvedSummary = GeminiClient.autoFixResume(resume.summary, jd)
            
            activeResume.value = activeResume.value.copy(
                summary = improvedSummary,
                atsScore = (atsScanResult.value?.overallScore ?: 70) + 5
            )
            saveActiveResume()
            isAiLoading.value = false
            aiMessageFeedback.value = "Summary optimized with targeted ATS keywords!"
        }
    }
}

// Simple authentication wrapper session state
data class UserSession(
    val fullName: String,
    val email: String,
    val phone: String,
    val isBiometricEnabled: Boolean,
    val jobTitle: String = "",
    val location: String = "",
    val profilePhotoUrl: String = ""
)

// ATS Scan Result model
data class AtsScanData(
    val overallScore: Int,
    val keywordsMatchPercent: Int,
    val formattingScore: Int,
    val completenessScore: Int,
    val readabilityScore: Int,
    val contactInfoScore: Int,
    val foundKeywords: List<String>,
    val missingKeywords: List<String>,
    val suggestions: List<AtsSuggestion>
)

data class AtsSuggestion(
    val title: String,
    val severity: String, // High, Medium, Low
    val instruction: String
)

data class NotificationData(
    val title: String,
    val message: String,
    val timeAgo: String
)

class MainViewModelFactory(private val resumeDao: ResumeDao, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(resumeDao, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
