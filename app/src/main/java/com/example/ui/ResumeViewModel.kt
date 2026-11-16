@file:OptIn(FlowPreview::class)

package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { RESUMES, ATS, LETTER, SETTINGS, EDITOR, PREVIEW }

/** One-line message for the snackbar, optionally with an action such as Undo. */
data class UiMessage(val text: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)

class ResumeViewModel(private val dao: ResumeDao, private val app: Context) : ViewModel() {

    private val prefs = app.getSharedPreferences("autoresume_prefs", Context.MODE_PRIVATE)

    val resumes: StateFlow<List<Resume>> =
        dao.getAllResumes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val screen = MutableStateFlow(Screen.RESUMES)
    val draft = MutableStateFlow<Resume?>(null)
    val themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val message = MutableStateFlow<UiMessage?>(null)
    fun say(text: String) { message.value = UiMessage(text) }

    val showOnboarding = MutableStateFlow(!prefs.getBoolean("onboarded", false))
    val language = MutableStateFlow(prefs.getString("language", "en") ?: "en")

    // cover letter state
    val letterText = MutableStateFlow("")
    val jobDescription = MutableStateFlow("")

    val aiAvailable: Boolean =
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    val aiBusy = MutableStateFlow<String?>(null)

    val atsResult = MutableStateFlow<AtsScanData?>(null)
    val atsAiText = MutableStateFlow("")

    private val saveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        Lang.code = language.value
        viewModelScope.launch {
            saveTrigger.debounce(500).collect { saveNow() }
        }
    }

    // ---------- navigation ----------
    fun go(s: Screen) { screen.value = s }

    fun openEditor(r: Resume) {
        draft.value = r
        screen.value = Screen.EDITOR
    }

    fun closeEditor() {
        saveNow()
        draft.value = null
        screen.value = Screen.RESUMES
    }

    // ---------- resumes ----------
    fun create(name: String, withSample: Boolean) {
        viewModelScope.launch {
            val base = if (withSample) sampleResume() else Resume()
            val r = base.copy(name = name.ifBlank { "Untitled resume" })
            val id = dao.insertResume(r).toInt()
            openEditor(r.copy(id = id))
        }
    }

    fun update(change: (Resume) -> Resume) {
        draft.update { it?.let(change) }
        saveTrigger.tryEmit(Unit)
    }

    private fun saveNow() {
        val r = draft.value ?: return
        viewModelScope.launch { dao.insertResume(r.copy(lastModified = System.currentTimeMillis())) }
    }

    fun duplicate(r: Resume) = viewModelScope.launch {
        dao.insertResume(r.copy(id = 0, name = "${r.name} (copy)", lastModified = System.currentTimeMillis()))
    }

    fun delete(r: Resume) = viewModelScope.launch { dao.deleteResumeById(r.id) }

    fun rename(r: Resume, name: String) = viewModelScope.launch {
        dao.insertResume(r.copy(name = name.ifBlank { r.name }, lastModified = System.currentTimeMillis()))
    }

    fun setAvatar(uri: Uri) {
        val id = draft.value?.id ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                try {
                    val bmp = app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                    val side = minOf(bmp.width, bmp.height)
                    val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
                    val out = Bitmap.createScaledBitmap(square, minOf(side, 500), minOf(side, 500), true)
                    val f = File(app.filesDir, "avatar_${id}_${System.currentTimeMillis()}.jpg")
                    f.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                    Uri.fromFile(f).toString()
                } catch (e: Exception) { null }
            }
            if (saved == null) say("Could not read that image.") else update { it.copy(avatarUri = saved) }
        }
    }

    // ---------- AI (never inserts fake text when no key is configured) ----------
    private fun needAi(): Boolean {
        if (!aiAvailable) {
            say("AI is off. Add GEMINI_API_KEY to enable it (see README).")
            return false
        }
        return true
    }

    fun aiSummary() {
        val r = draft.value ?: return
        if (!needAi()) return
        viewModelScope.launch {
            aiBusy.value = "summary"
            val skills = r.skills.joinToString { it.name }
            val exp = r.experience.joinToString("; ") { "${it.role} at ${it.company}" }
            val text = GeminiClient.generateSummary(r.jobTitle.ifBlank { "professional" }, skills, exp)
            update { it.copy(summary = text) }
            aiBusy.value = null
        }
    }

    fun aiBullets(index: Int) {
        val r = draft.value ?: return
        val e = r.experience.getOrNull(index) ?: return
        if (!needAi()) return
        viewModelScope.launch {
            aiBusy.value = "exp$index"
            val text = GeminiClient.generateBulletPoints(e.role, e.company, e.description)
            update { cur -> cur.copy(experience = cur.experience.toMutableList().also { l -> if (index in l.indices) l[index] = l[index].copy(description = text) }) }
            aiBusy.value = null
        }
    }

    fun aiSkills() {
        val r = draft.value ?: return
        if (r.jobTitle.isBlank()) { say("Add your job title first."); return }
        if (!needAi()) return
        viewModelScope.launch {
            aiBusy.value = "skills"
            val names = GeminiClient.generateSkillSuggestions(r.jobTitle).split(",").map { it.trim() }.filter { it.isNotBlank() }
            update { cur ->
                val have = cur.skills.map { it.name.lowercase() }.toSet()
                cur.copy(skills = cur.skills + names.filter { it.lowercase() !in have }.map { SkillEntry(it, "Intermediate", "Technical") })
            }
            aiBusy.value = null
        }
    }

    // ---------- ATS ----------
    fun analyze(r: Resume, jobDescription: String, presetKeywords: List<String>) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                AtsAnalyzerEngine.analyzeText(
                    resumeText = AtsAnalyzerEngine.resumeToPlainText(r),
                    jobDescription = jobDescription,
                    presetKeywords = presetKeywords,
                )
            }
            atsResult.value = result
            atsAiText.value = ""
            dao.insertResume(r.copy(atsScore = result.overallScore))
        }
    }

    fun aiImprove(r: Resume, jobDescription: String) {
        if (!needAi()) return
        viewModelScope.launch {
            aiBusy.value = "ats"
            atsAiText.value = GeminiClient.autoFixResume(AtsAnalyzerEngine.resumeToPlainText(r), jobDescription)
            aiBusy.value = null
        }
    }


    // ---------- undo ----------
    /** Applies a removal and offers Undo in the snackbar. */
    fun removeEntry(label: String, change: (Resume) -> Resume) {
        val before = draft.value ?: return
        update(change)
        message.value = UiMessage("$label removed", "Undo") {
            draft.value = before
            saveTrigger.tryEmit(Unit)
        }
    }

    fun updateDesign(change: (DesignOptions) -> DesignOptions) =
        update { it.copy(fontSize = change(DesignOptions.parse(it)).encode()) }

    // ---------- onboarding / import / backup ----------
    fun finishOnboarding(name: String, job: String, email: String) {
        prefs.edit().putBoolean("onboarded", true).apply()
        showOnboarding.value = false
        if (name.isBlank() && job.isBlank()) return
        viewModelScope.launch {
            val r = Resume(name = "My resume", fullName = name.trim(), jobTitle = job.trim(), email = email.trim())
            val id = dao.insertResume(r).toInt()
            openEditor(r.copy(id = id))
        }
    }

    fun importLinkedIn(uri: Uri) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { LinkedInImport.fromZip(app, uri) }
            if (r == null) {
                say("That file is not a LinkedIn data export (.zip with Profile.csv, Positions.csv...).")
            } else {
                val id = dao.insertResume(r).toInt()
                openEditor(r.copy(id = id))
                say("Imported. Please check each section.")
            }
        }
    }

    // ---------- tailor to a job ----------
    /** Copies the resume and adds the chosen keywords as skills. Original stays untouched. */
    fun createTailored(base: Resume, keywords: List<String>) {
        viewModelScope.launch {
            val have = base.skills.map { it.name.lowercase() }.toSet()
            val added = keywords.filter { it.isNotBlank() && it.lowercase() !in have }.map { SkillEntry(it, "Intermediate", "Technical") }
            val copy = base.copy(id = 0, name = "${base.name} - tailored", skills = base.skills + added, atsScore = 0, lastModified = System.currentTimeMillis())
            val id = dao.insertResume(copy).toInt()
            val saved = copy.copy(id = id)
            message.value = UiMessage("Created a tailored copy with ${added.size} new skills", "Open") { openEditor(saved) }
        }
    }

    // ---------- cover letter ----------
    fun generateLetter(r: Resume, role: String, company: String, manager: String, tone: String, jd: String) {
        viewModelScope.launch {
            aiBusy.value = "letter"
            val ai = if (aiAvailable) GeminiClient.generateCoverLetter(AtsAnalyzerEngine.resumeToPlainText(r), jd, role.ifBlank { r.jobTitle }, company, manager, tone) else null
            letterText.value = ai ?: CoverLetter.offlineDraft(r, role, company, manager)
            if (ai == null) say(if (aiAvailable) "AI did not answer, so here is a basic draft you can edit." else "AI is off, so here is a fill-in draft from your resume. Edit it before sending.")
            aiBusy.value = null
        }
    }

    fun setLanguage(code: String) {
        language.value = code
        Lang.code = code
        prefs.edit().putString("language", code).apply()
    }

    fun writingIssues(r: Resume): List<WritingIssue> = WritingCheck.analyze(r)

    // ---------- settings ----------
    fun setThemeMode(mode: String) {
        themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    private fun sampleResume() = Resume(
        fullName = "Alex Morgan", jobTitle = "Marketing Manager", phone = "+1 555 0100",
        email = "alex.morgan@example.com", location = "Austin, TX",
        summary = "Marketing manager with 6 years of experience growing B2B brands through content, lifecycle email and paid campaigns. Led a team of 5 and increased qualified leads by 42% in 12 months.",
        experience = listOf(
            ExperienceEntry("Northwind", "Marketing Manager", "Mar 2022", "", true,
                "• Grew qualified leads 42% year over year with a new content and webinar programme.\n• Cut cost per lead by 18% by reallocating paid budget using cohort reports.\n• Managed a team of 5 and a 1.2M annual budget."),
            ExperienceEntry("Brightside", "Marketing Specialist", "Jun 2019", "Feb 2022", false,
                "• Launched lifecycle email flows that lifted trial-to-paid conversion by 11%.\n• Ran 30+ A/B tests on landing pages."),
        ),
        education = listOf(EducationEntry("University of Texas", "B.A. Communications", "2015", "2019", "", "")),
        skills = listOf("SEO", "Content strategy", "Google Analytics", "HubSpot", "Email marketing", "A/B testing", "Team leadership")
            .map { SkillEntry(it, "Intermediate", "Technical") },
        languages = listOf(LanguageEntry("English", "Native")),
    )

    /** 0-100 and the single most useful next step. */
    fun strength(r: Resume): Pair<Int, String?> {
        val checks = listOf(
            (r.fullName.isNotBlank() && r.jobTitle.isNotBlank()) to "Add your name and job title",
            (r.email.isNotBlank() && r.phone.isNotBlank()) to "Add email and phone",
            (r.summary.length >= 60) to "Write a summary of at least two sentences",
            r.experience.isNotEmpty() to "Add your work experience",
            r.experience.all { it.description.length >= 40 } to "Describe achievements for every job",
            r.education.isNotEmpty() to "Add your education",
            (r.skills.size >= 5) to "List at least 5 skills",
            r.location.isNotBlank() to "Add your location",
        )
        val done = checks.count { it.first }
        return (done * 100 / checks.size) to checks.firstOrNull { !it.first }?.second
    }
}
