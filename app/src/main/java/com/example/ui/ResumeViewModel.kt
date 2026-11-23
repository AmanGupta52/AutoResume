@file:OptIn(FlowPreview::class)

package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.AiClient
import com.example.api.AiError
import com.example.api.AiMessage
import com.example.api.AiResult
import com.example.api.ApiResult
import com.example.api.BackendClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.DateFormat
import java.util.Date

enum class Screen { RESUMES, ATS, LETTER, SETTINGS, EDITOR, PREVIEW, GUIDED }

data class AuthUser(val id: String, val name: String, val email: String)

enum class AuthStage { LOGIN, REGISTER, VERIFY, FORGOT, RESET }

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

    val language = MutableStateFlow(prefs.getString("language", "en") ?: "en")

    // cover letter state
    val letterText = MutableStateFlow("")
    val jobDescription = MutableStateFlow("")

    val aiAvailableFlow = MutableStateFlow(AiClient.isConfigured)
    val aiAvailable: Boolean get() = aiAvailableFlow.value
    /** Which AI job is running ("summary", "exp0", "skills", "advice", "ats", "letter"), or null. */
    val aiBusy = MutableStateFlow<String?>(null)

    /** Tab the person is on in the editor. The AI is told this before every request. */
    val editorTab = MutableStateFlow(0)
    /** AI text waiting for the person to accept it. Nothing changes in the resume until then. */
    val proposal = MutableStateFlow<AiProposal?>(null)
    private var aiSeq = 0
    private var aiJob: Job? = null

    // chat helper
    val showChat = MutableStateFlow(false)
    val chat = MutableStateFlow<List<AiMessage>>(emptyList())
    val chatStream = MutableStateFlow<String?>(null)
    val chatError = MutableStateFlow<AiError?>(null)
    val chatErrorDetail = MutableStateFlow<String?>(null)
    private var chatJob: Job? = null

    val guidedStep = MutableStateFlow(0)

    val atsResult = MutableStateFlow<AtsScanData?>(null)
    val atsAiText = MutableStateFlow("")

    private val saveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        Lang.code = language.value
        AiClient.tokenProvider = { prefs.getString("auth_token", "") ?: "" } // AI goes through the backend, so it needs the login
        aiAvailableFlow.value = AiClient.isConfigured
        viewModelScope.launch {
            saveTrigger.debounce(500).collect { saveNow() }
        }
    }

    // ---------- account (login) and sync with the backend ----------
    private val sync = ResumeSync(dao, prefs)
    private fun token(): String = prefs.getString("auth_token", "") ?: ""

    val auth = MutableStateFlow(loadAuth())
    val authStage = MutableStateFlow(AuthStage.LOGIN)
    val authBusy = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val authNotice = MutableStateFlow<String?>(null)
    val pendingEmail = MutableStateFlow("")
    val syncStatus = MutableStateFlow("")
    private val syncTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var dirty = false // the open draft has edits that are not in the database yet

    // second init block: it must come after the properties above, which are created in textual order
    init {
        viewModelScope.launch {
            syncTrigger.debounce(3000).collect { runSync() }
        }
        if (auth.value != null) runSync()
    }

    private fun loadAuth(): AuthUser? {
        val id = prefs.getString("auth_uid", "") ?: ""
        if (token().isBlank() || id.isBlank()) return null
        return AuthUser(id, prefs.getString("auth_name", "") ?: "", prefs.getString("auth_email", "") ?: "")
    }

    fun authGo(stage: AuthStage) {
        authStage.value = stage
        authError.value = null
        authNotice.value = null
    }

    private fun authCall(
        path: String,
        body: JSONObject,
        onFail: (ApiResult.Fail) -> Unit = { authError.value = it.message },
        onOk: (JSONObject) -> Unit,
    ) {
        if (authBusy.value) return
        authBusy.value = true
        authError.value = null
        authNotice.value = null
        viewModelScope.launch {
            when (val r = BackendClient.call("POST", path, body)) {
                is ApiResult.Ok -> onOk(r.json)
                is ApiResult.Fail -> onFail(r)
            }
            authBusy.value = false
        }
    }

    fun register(name: String, email: String, password: String) = authCall(
        "/api/auth/register",
        JSONObject().put("name", name.trim()).put("email", email.trim()).put("password", password),
    ) {
        pendingEmail.value = email.trim().lowercase()
        authStage.value = AuthStage.VERIFY
        authNotice.value = "We sent a 6-digit code to ${pendingEmail.value}."
    }

    fun verify(code: String) = authCall(
        "/api/auth/verify",
        JSONObject().put("email", pendingEmail.value).put("code", code.trim()),
    ) { startSession(it) }

    fun resendCode() = authCall(
        "/api/auth/resend",
        JSONObject().put("email", pendingEmail.value),
    ) { authNotice.value = "A new code is on its way." }

    fun login(email: String, password: String) = authCall(
        "/api/auth/login",
        JSONObject().put("email", email.trim()).put("password", password),
        onFail = {
            if (it.code == "EMAIL_NOT_VERIFIED") {
                pendingEmail.value = email.trim().lowercase()
                authStage.value = AuthStage.VERIFY
                authNotice.value = it.message
            } else authError.value = it.message
        },
    ) { startSession(it) }

    fun forgotPassword(email: String) = authCall(
        "/api/auth/forgot",
        JSONObject().put("email", email.trim()),
    ) {
        pendingEmail.value = email.trim().lowercase()
        authStage.value = AuthStage.RESET
        authNotice.value = "If an account exists, a code is on its way to ${pendingEmail.value}."
    }

    fun resetPassword(code: String, password: String) = authCall(
        "/api/auth/reset",
        JSONObject().put("email", pendingEmail.value).put("code", code.trim()).put("password", password),
    ) {
        authStage.value = AuthStage.LOGIN
        authNotice.value = "Your password was changed. Please log in."
    }

    private fun startSession(j: JSONObject) {
        val tokenValue = j.optString("token", "")
        val u = j.optJSONObject("user")
        if (tokenValue.isBlank() || u == null) {
            authError.value = "Unexpected answer from the server."
            return
        }
        val user = AuthUser(u.optString("id"), u.optString("name"), u.optString("email"))
        val last = prefs.getString("auth_last_uid", "") ?: ""
        viewModelScope.launch {
            // another person's resumes must not stay on this phone
            if (last.isNotBlank() && last != user.id) sync.wipeLocal()
            prefs.edit().putString("auth_token", tokenValue).putString("auth_uid", user.id)
                .putString("auth_name", user.name).putString("auth_email", user.email).putString("auth_last_uid", user.id).apply()
            authStage.value = AuthStage.LOGIN
            pendingEmail.value = ""
            auth.value = user
            aiAvailableFlow.value = AiClient.isConfigured
            runSync()
        }
    }

    private fun expireSession() {
        prefs.edit().remove("auth_token").apply() // local resumes stay; they sync again after the next login
        aiAvailableFlow.value = false
        auth.value = null
        authStage.value = AuthStage.LOGIN
        authNotice.value = "Your session expired. Please log in again."
        syncStatus.value = ""
    }

    fun syncSoon() {
        if (auth.value != null) syncTrigger.tryEmit(Unit)
    }

    fun runSync() {
        val t = token()
        if (t.isBlank() || !BackendClient.isConfigured) return
        viewModelScope.launch {
            syncStatus.value = "Syncing..."
            when (val o = sync.syncAll(t, draft.value?.id)) {
                is SyncOutcome.Ok -> {
                    syncStatus.value = "Last synced " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
                    o.warning?.let { say(it) }
                }
                is SyncOutcome.Fail -> when {
                    o.unauthorized -> expireSession()
                    o.offline -> syncStatus.value = "Offline. Your changes will sync later."
                    else -> syncStatus.value = "Sync problem: ${o.message}"
                }
            }
        }
    }

    private suspend fun flushDraft() {
        val r = draft.value ?: return
        if (!dirty) return
        dirty = false
        dao.insertResume(r.copy(lastModified = System.currentTimeMillis()))
    }

    /**
     * Sends the PDF the phone just made to the backend, which keeps it in Cloudinary and stores only the link.
     * The copy on the phone is already saved; this never blocks or removes it.
     */
    fun storePdf(localId: Int, file: File) {
        val t = token()
        if (t.isBlank() || !BackendClient.isConfigured) return
        viewModelScope.launch {
            flushDraft()
            var rid = sync.remoteId(localId)
            if (rid == null) { // a brand new resume: upload it first so the PDF has somewhere to belong
                sync.syncAll(t, draft.value?.id)
                rid = sync.remoteId(localId)
            }
            if (rid == null) {
                say("PDF saved on the phone. It will be saved to your account once you are online and the resume has synced.")
                return@launch
            }
            val bytes = withContext(Dispatchers.IO) { try { file.readBytes() } catch (e: Exception) { null } }
            if (bytes == null) { say("Could not read the PDF to send it to your account."); return@launch }
            when (val r = BackendClient.putBytes("/api/resumes/$rid/pdf", bytes, t)) {
                is ApiResult.Ok -> say("PDF also saved to your account.")
                is ApiResult.Fail -> when {
                    r.unauthorized -> expireSession()
                    r.code == "PDF_STORAGE_OFF" -> say("PDF saved on the phone. Cloud PDF storage is not set up on the server.")
                    else -> say("PDF saved on the phone, but not to your account: ${r.message}")
                }
            }
        }
    }

    /** Syncs first, so nothing is lost, then removes the account's resumes from this phone. */
    fun logout() {
        val t = token()
        viewModelScope.launch {
            syncStatus.value = "Syncing before sign out..."
            flushDraft()
            val o = sync.syncAll(t, null)
            if (o is SyncOutcome.Fail) {
                if (o.unauthorized) expireSession()
                else {
                    syncStatus.value = ""
                    say("Could not sync your latest changes. Connect to the internet and try again.")
                }
                return@launch
            }
            sync.wipeLocal()
            prefs.edit().remove("auth_token").remove("auth_uid").remove("auth_name").remove("auth_email").remove("auth_last_uid").apply()
            aiAvailableFlow.value = false
            resetAi()
            draft.value = null
            screen.value = Screen.RESUMES
            syncStatus.value = ""
            authStage.value = AuthStage.LOGIN
            auth.value = null
        }
    }

    // ---------- navigation ----------
    fun go(s: Screen) { screen.value = s }

    fun openEditor(r: Resume) {
        resetAi()
        draft.value = r
        editorTab.value = 0
        screen.value = Screen.EDITOR
    }

    fun openGuided(r: Resume) {
        resetAi()
        draft.value = r
        guidedStep.value = 0
        screen.value = Screen.GUIDED
    }

    fun closeEditor() {
        saveNow()
        resetAi()
        draft.value = null
        screen.value = Screen.RESUMES
    }

    // ---------- resumes ----------
    fun create(name: String, withSample: Boolean) {
        viewModelScope.launch {
            val base = if (withSample) sampleResume() else Resume()
            val r = base.copy(name = name.ifBlank { "Untitled resume" })
            val id = dao.insertResume(r).toInt()
            syncSoon()
            if (withSample) openEditor(r.copy(id = id)) else openGuided(r.copy(id = id))
        }
    }

    fun update(change: (Resume) -> Resume) {
        draft.update { it?.let(change) }
        dirty = true
        saveTrigger.tryEmit(Unit)
    }

    private fun saveNow() {
        val r = draft.value ?: return
        if (!dirty) return // nothing changed, so the copy on the account must not be overwritten
        dirty = false
        viewModelScope.launch {
            dao.insertResume(r.copy(lastModified = System.currentTimeMillis()))
            syncSoon()
        }
    }

    fun duplicate(r: Resume) = viewModelScope.launch {
        dao.insertResume(r.copy(id = 0, name = "${r.name} (copy)", lastModified = System.currentTimeMillis()))
        syncSoon()
    }

    fun delete(r: Resume) = viewModelScope.launch {
        sync.noteDeleted(r.id) // the account copy is removed on the next sync
        dao.deleteResumeById(r.id)
        syncSoon()
    }

    fun rename(r: Resume, name: String) = viewModelScope.launch {
        dao.insertResume(r.copy(name = name.ifBlank { r.name }, lastModified = System.currentTimeMillis()))
        syncSoon()
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

    // ---------- AI (never inserts fake text; the person accepts every change) ----------
    private fun resetAi() {
        aiSeq++
        aiJob?.cancel(); chatJob?.cancel()
        aiBusy.value = null
        proposal.value = null
        chat.value = emptyList(); chatStream.value = null; chatError.value = null; chatErrorDetail.value = null
        showChat.value = false
    }

    private fun needAi(): Boolean {
        if (!aiAvailable) {
            say(AiError.NO_KEY.message)
            return false
        }
        return true
    }

    private fun failed(e: AiError) = say(e.message)

    /** Settings button: one tiny request so the person (and you) can see whether AI works and why not. */
    fun testAi() {
        if (aiBusy.value != null) return
        viewModelScope.launch {
            aiBusy.value = "test"
            when (val res = AiClient.ask("Reply with the single word OK.", "ping", maxTokens = 20, temperature = 0.0)) {
                is AiResult.Ok -> say("AI works.")
                is AiResult.Fail -> say(res.error.message + (res.detail?.let { " ($it)" } ?: ""))
            }
            aiBusy.value = null
        }
    }

    private fun busyKey(req: AiRequest) = when (req.kind) {
        ProposalKind.SUMMARY -> "summary"
        ProposalKind.BULLETS -> "exp${req.index}"
        ProposalKind.SKILLS -> "skills"
        ProposalKind.ADVICE -> "advice"
    }

    private fun proposalTitle(req: AiRequest, r: Resume) = when (req.kind) {
        ProposalKind.SUMMARY -> "Summary"
        ProposalKind.BULLETS -> r.experience.getOrNull(req.index)
            ?.let { "Job ${req.index + 1}" + if (it.role.isNotBlank()) " - ${it.role}" else "" } ?: "Achievements"
        ProposalKind.SKILLS -> "Skills for your target job"
        ProposalKind.ADVICE -> "Fixes for the top problems"
    }

    private fun currentText(req: AiRequest, r: Resume) = when (req.kind) {
        ProposalKind.SUMMARY -> r.summary
        ProposalKind.BULLETS -> r.experience.getOrNull(req.index)?.description.orEmpty()
        else -> ""
    }

    private fun tabFor(req: AiRequest) = when (req.kind) {
        ProposalKind.SUMMARY -> "Summary"
        ProposalKind.BULLETS -> "Experience"
        ProposalKind.SKILLS -> "Skills"
        ProposalKind.ADVICE -> "Check"
    }

    /** Starts an AI request. The answer streams into a preview sheet; nothing is applied yet. */
    fun ask(req: AiRequest) {
        val r = draft.value ?: return
        if (req.kind == ProposalKind.BULLETS && r.experience.getOrNull(req.index)?.role.isNullOrBlank()) {
            say("Add the job title for this job first."); return
        }
        val before = currentText(req, r)
        proposal.value = AiProposal(req, proposalTitle(req, r), before)
        if (!aiAvailable) {
            proposal.update { it?.copy(loading = false, error = AiError.NO_KEY) }
            return
        }
        startAi(if (req.tweak != null) before else "")
    }

    private fun startAi(base: String) {
        val p = proposal.value ?: return
        val r = draft.value ?: return
        val keep = p.text // an error never throws away the last good answer
        aiJob?.cancel()
        val my = ++aiSeq
        aiBusy.value = busyKey(p.request)
        proposal.value = p.copy(base = base, text = "", loading = true, error = null, errorDetail = null)
        aiJob = viewModelScope.launch {
            try {
                val problems = if (p.request.kind == ProposalKind.ADVICE)
                    WritingCheck.analyze(r).take(3).joinToString("\n") { "- ${it.where}: ${it.problem} ${it.fix}" } else ""
                val msgs = listOf(
                    AiMessage("system", AiPrompts.system(p.request)),
                    AiMessage("user", AiPrompts.user(p.request, r, tabFor(p.request), base, problems)),
                )
                val res = AiClient.stream(msgs, maxTokens = if (p.request.kind == ProposalKind.ADVICE) 900 else 450) { t ->
                    if (my == aiSeq) proposal.update { cur -> if (cur != null && cur.loading) cur.copy(text = t) else cur }
                }
                if (my == aiSeq) when (res) {
                    is AiResult.Ok -> proposal.update { it?.copy(text = res.text, loading = false) }
                    is AiResult.Fail -> proposal.update { it?.copy(text = keep, loading = false, error = res.error, errorDetail = res.detail) }
                }
            } finally {
                if (my == aiSeq) aiBusy.value = null
            }
        }
    }

    /** Same request again, for a new variation (or after an error). */
    fun retryProposal() {
        val p = proposal.value ?: return
        if (p.loading) return
        if (!aiAvailable) { proposal.update { it?.copy(error = AiError.NO_KEY) }; return }
        startAi(p.base)
    }

    /** Shorter / stronger / more formal, applied to the text now in the sheet. */
    fun tweakProposal(t: Tweak) {
        val p = proposal.value ?: return
        if (p.loading || !aiAvailable) return
        val basis = p.text.ifBlank { p.before }
        proposal.value = p.copy(request = p.request.copy(tweak = t))
        startAi(basis)
    }

    fun editProposalText(text: String) = proposal.update { it?.copy(text = text) }

    fun closeProposal() {
        aiSeq++
        aiJob?.cancel()
        aiBusy.value = null
        proposal.value = null
    }

    private fun parseSkills(text: String): List<String> =
        text.split(",", "\n").map { it.trim().trimStart('•', '-', '*').trim().trimEnd('.') }
            .filter { it.isNotBlank() && it.length <= 40 }

    fun proposalSkills(text: String): List<String> = parseSkills(text)

    /** The person tapped "Use this". [skills] is the chosen subset when the proposal is a skills list. */
    fun applyProposal(skills: List<String>? = null) {
        val p = proposal.value ?: return
        if (p.loading || p.text.isBlank() || p.request.kind == ProposalKind.ADVICE) return
        val before = draft.value
        val text = p.text.trim()
        val idx = p.request.index
        when (p.request.kind) {
            ProposalKind.SUMMARY -> update { it.copy(summary = text) }
            ProposalKind.BULLETS -> update { cur ->
                cur.copy(experience = cur.experience.toMutableList().also { l ->
                    if (idx in l.indices) l[idx] = l[idx].copy(description = text)
                })
            }
            ProposalKind.SKILLS -> {
                val names = skills ?: parseSkills(text)
                update { cur ->
                    val have = cur.skills.map { it.name.lowercase() }.toSet()
                    cur.copy(skills = cur.skills + names.filter { it.lowercase() !in have }.map { SkillEntry(it, "Intermediate", "Technical") })
                }
            }
            ProposalKind.ADVICE -> {}
        }
        closeProposal()
        message.value = UiMessage("Added to your resume", "Undo") {
            draft.value = before
            dirty = true
            saveTrigger.tryEmit(Unit)
        }
    }

    fun aiSummary() = ask(AiRequest(ProposalKind.SUMMARY))
    fun aiBullets(index: Int) = ask(AiRequest(ProposalKind.BULLETS, index))
    fun aiSkills() {
        val r = draft.value ?: return
        if (r.jobTitle.isBlank() && !TargetJob.parse(r).isSet) { say("Add your job title or target job first."); return }
        ask(AiRequest(ProposalKind.SKILLS))
    }

    // ---------- target job ----------
    fun setTarget(t: TargetJob) = update { it.withTarget(t) }

    fun applySmartTemplateIfDefault() {
        val r = draft.value ?: return
        if (r.templateId != "Modern Professional") return
        val t = TargetJob.parse(r)
        val (name, _) = ResumeCoach.templateFor(t.title.ifBlank { r.jobTitle }, t.description)
        if (name != r.templateId) update { it.copy(templateId = name) }
    }

    // ---------- chat helper ----------
    fun openChat() { showChat.value = true }
    fun closeChat() { showChat.value = false }
    fun clearChat() { chatJob?.cancel(); chat.value = emptyList(); chatStream.value = null; chatError.value = null; chatErrorDetail.value = null }

    fun sendChat(text: String) {
        val r = draft.value ?: return
        val t = text.trim()
        if (t.isEmpty() || chatStream.value != null) return
        chatError.value = null; chatErrorDetail.value = null
        chat.update { it + AiMessage("user", t) }
        if (!aiAvailable) { chatError.value = AiError.NO_KEY; return }
        chatStream.value = ""
        val tab = if (screen.value == Screen.GUIDED) "Guided" else EditorTabNames.getOrElse(editorTab.value) { "Basics" }
        chatJob = viewModelScope.launch {
            try {
                val history = chat.value.takeLast(8)
                val msgs = listOf(AiMessage("system", AiPrompts.chatSystem(tab, draft.value ?: r))) + history
                when (val res = AiClient.stream(msgs, maxTokens = 600, temperature = 0.5) { chatStream.value = it }) {
                    is AiResult.Ok -> chat.update { it + AiMessage("assistant", res.text) }
                    is AiResult.Fail -> { chatError.value = res.error; chatErrorDetail.value = res.detail }
                }
            } finally {
                chatStream.value = null
            }
        }
    }

    fun stopChat() {
        val partial = chatStream.value.orEmpty()
        chatJob?.cancel()
        chatStream.value = null
        if (partial.isNotBlank()) chat.update { it + AiMessage("assistant", partial) }
    }

    fun retryChat() {
        val last = chat.value.lastOrNull() ?: return
        if (last.role != "user") return
        chat.update { it.dropLast(1) }
        sendChat(last.content)
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
            val system = "You are an ATS optimisation expert. Compare the resume with the job description and give: " +
                "1) a rewritten professional summary, 2) up to 5 concrete bullet rewrites that naturally include missing keywords. " +
                "Use ONLY facts present in the resume; never invent numbers. Plain text, no markdown."
            val user = "${AiContext.block("ATS", r)}\n\nJOB DESCRIPTION TO MATCH:\n$jobDescription"
            when (val res = AiClient.ask(system, user, 900)) {
                is AiResult.Ok -> atsAiText.value = res.text
                is AiResult.Fail -> say(res.error.message + (res.detail?.let { " ($it)" } ?: ""))
            }
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
            dirty = true
            saveTrigger.tryEmit(Unit)
        }
    }

    fun updateDesign(change: (DesignOptions) -> DesignOptions) =
        update { it.copy(fontSize = change(DesignOptions.parse(it)).encode()) }

    // ---------- import / backup ----------
    fun importLinkedIn(uri: Uri) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { LinkedInImport.fromZip(app, uri) }
            if (r == null) {
                say("That file is not a LinkedIn data export (.zip with Profile.csv, Positions.csv...).")
            } else {
                val id = dao.insertResume(r).toInt()
                syncSoon()
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
            syncSoon()
            val saved = copy.copy(id = id)
            message.value = UiMessage("Created a tailored copy with ${added.size} new skills", "Open") { openEditor(saved) }
        }
    }

    // ---------- cover letter ----------
    fun generateLetter(r: Resume, role: String, company: String, manager: String, tone: String, jd: String) {
        viewModelScope.launch {
            aiBusy.value = "letter"
            val target = TargetJob.parse(r)
            val useRole = role.ifBlank { target.title.ifBlank { r.jobTitle } }
            val useJd = jd.ifBlank { target.description }
            var ai: String? = null
            var err: AiError? = null
            if (aiAvailable) {
                val system = "You write honest, specific cover letters. Use ONLY facts that appear in the resume; never invent employers, numbers or skills. " +
                    "Plain text, no markdown, no placeholders in brackets. Return only the letter."
                val user = "Write a cover letter for the role '$useRole' at '${company.ifBlank { "the company" }}', addressed to '${manager.ifBlank { "Hiring Manager" }}'. " +
                    "Tone: $tone. Length: 220-300 words, 3 short paragraphs plus sign-off.\n\nRESUME:\n${AtsAnalyzerEngine.resumeToPlainText(r)}\n\nJOB DESCRIPTION:\n$useJd"
                when (val res = AiClient.ask(system, user, 700, 0.5)) {
                    is AiResult.Ok -> ai = res.text
                    is AiResult.Fail -> err = res.error
                }
            }
            letterText.value = ai ?: CoverLetter.offlineDraft(r, useRole, company, manager)
            if (ai == null) say(if (aiAvailable) "${err?.message ?: "AI did not answer."} Here is a basic draft you can edit." else "AI is off, so here is a fill-in draft from your resume. Edit it before sending.")
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
    fun strength(r: Resume): Pair<Int, String?> = ResumeCoach.strength(r) to ResumeCoach.nextStep(r)?.label
}
