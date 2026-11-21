package com.example.ui

import android.content.SharedPreferences
import com.example.api.ApiResult
import com.example.api.BackendClient
import com.example.data.Resume
import com.example.data.ResumeDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class SyncOutcome {
    data class Ok(val pulled: Int, val pushed: Int, val removed: Int, val warning: String? = null) : SyncOutcome()
    data class Fail(val message: String, val unauthorized: Boolean = false, val offline: Boolean = false) : SyncOutcome()
}

/**
 * Keeps the phone's resumes (Room) and the account's resumes (MongoDB, through the backend) in step.
 * The phone always works offline; this runs when there is a connection.
 *
 * Rules: the copy with the newer edit time wins. Each resume is linked to its server id in SharedPreferences,
 * so the Room database (and its schema) did not have to change. Only resume content is sent: no PDF, no photo.
 */
class ResumeSync(private val dao: ResumeDao, private val prefs: SharedPreferences) {

    private data class Link(val rid: String, val ref: String)

    private val mutex = Mutex()
    private val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(Resume::class.java)

    // ---------- link storage ----------
    private fun loadLinks(): MutableMap<Int, Link> {
        val out = mutableMapOf<Int, Link>()
        try {
            val o = JSONObject(prefs.getString(KEY_LINKS, "{}") ?: "{}")
            for (k in o.keys()) {
                val v = o.getJSONObject(k)
                k.toIntOrNull()?.let { out[it] = Link(v.optString("rid", ""), v.optString("ref", "")) }
            }
        } catch (e: Exception) { /* start clean */ }
        return out
    }

    private fun saveLinks(m: Map<Int, Link>) {
        val o = JSONObject()
        m.forEach { (id, l) -> o.put(id.toString(), JSONObject().put("rid", l.rid).put("ref", l.ref)) }
        prefs.edit().putString(KEY_LINKS, o.toString()).apply()
    }

    private fun loadDeleted(): MutableList<String> {
        val out = mutableListOf<String>()
        try {
            val a = JSONArray(prefs.getString(KEY_DELETED, "[]") ?: "[]")
            for (i in 0 until a.length()) out.add(a.getString(i))
        } catch (e: Exception) { /* none */ }
        return out
    }

    private fun saveDeleted(l: List<String>) {
        prefs.edit().putString(KEY_DELETED, JSONArray(l).toString()).apply()
    }

    /** The account's id for a local resume, or null when it has not been uploaded yet. */
    fun remoteId(localId: Int): String? = loadLinks()[localId]?.rid?.takeIf { it.isNotEmpty() }

    /** Call after a resume is deleted on the phone, so the deletion reaches the account on the next sync. */
    fun noteDeleted(localId: Int) {
        val links = loadLinks()
        val link = links.remove(localId) ?: return
        saveLinks(links)
        if (link.rid.isNotEmpty()) saveDeleted(loadDeleted().also { it.add(link.rid) })
    }

    /** Signing out removes the account's resumes from this phone (they stay in the account). */
    suspend fun wipeLocal() = mutex.withLock {
        dao.getAllResumes().first().forEach { dao.deleteResumeById(it.id) }
        prefs.edit().remove(KEY_LINKS).remove(KEY_DELETED).apply()
    }

    // ---------- conversion ----------
    private fun payload(r: Resume, ref: String?): JSONObject {
        val data = JSONObject(adapter.toJson(r))
        for (k in listOf("id", "name", "avatarUri", "lastModified")) data.remove(k)
        val o = JSONObject().put("name", r.name).put("data", data).put("clientUpdatedAt", r.lastModified)
        if (ref != null) o.put("clientRef", ref)
        return o
    }

    private fun fromRemote(j: JSONObject, localId: Int, avatar: String): Resume? = try {
        val data = JSONObject((j.optJSONObject("data") ?: JSONObject()).toString())
        data.put("id", localId)
        data.put("name", if (j.has("name")) j.optString("name", "Untitled resume") else "Untitled resume")
        data.put("avatarUri", avatar)
        data.put("lastModified", j.optLong("clientUpdatedAt", 0L))
        adapter.fromJson(data.toString())
    } catch (e: Exception) {
        null
    }

    private fun newRef() = "a" + UUID.randomUUID().toString().replace("-", "")

    // ---------- the sync ----------
    /** [openDraftId] is the resume being edited right now; it is never overwritten from the server. */
    suspend fun syncAll(token: String, openDraftId: Int?): SyncOutcome = mutex.withLock syncBody@{
        if (!BackendClient.isConfigured) return@syncBody SyncOutcome.Fail("The backend address is not set in this build.")
        val links = loadLinks()
        var pulled = 0
        var pushed = 0
        var removed = 0
        var warning: String? = null

        fun fail(r: ApiResult.Fail): SyncOutcome.Fail = SyncOutcome.Fail(r.message, r.unauthorized, r.offline)

        // 1. deletions made on the phone while offline
        val pendingDeletes = loadDeleted()
        for (rid in pendingDeletes.toList()) {
            when (val r = BackendClient.call("DELETE", "/api/resumes/$rid", token = token)) {
                is ApiResult.Ok -> pendingDeletes.remove(rid)
                is ApiResult.Fail -> if (r.status == 404) pendingDeletes.remove(rid) else if (r.unauthorized || r.offline) { saveDeleted(pendingDeletes); return@syncBody fail(r) }
            }
        }
        saveDeleted(pendingDeletes)

        // 2. what the account has
        val listed = when (val r = BackendClient.call("GET", "/api/resumes", token = token)) {
            is ApiResult.Ok -> r.json.optJSONArray("resumes") ?: JSONArray()
            is ApiResult.Fail -> return@syncBody fail(r)
        }
        val remote = (0 until listed.length()).mapNotNull { listed.optJSONObject(it) }
        val remoteById = remote.associateBy { it.optString("id") }
        val local = dao.getAllResumes().first()

        // 3. resumes that exist on both sides: newer edit wins
        for (l in local) {
            val link = links[l.id] ?: continue
            if (link.rid.isEmpty()) continue
            val rj = remoteById[link.rid]
            if (rj == null) {
                // deleted from another device
                if (l.id == openDraftId) links.remove(l.id) // being edited: keep it and upload it again below
                else { dao.deleteResumeById(l.id); links.remove(l.id); removed++ }
                continue
            }
            val remoteTs = rj.optLong("clientUpdatedAt", 0L)
            if (remoteTs > l.lastModified) {
                if (l.id != openDraftId) fromRemote(rj, l.id, l.avatarUri)?.let { dao.insertResume(it); pulled++ }
            } else if (remoteTs < l.lastModified) {
                when (val r = BackendClient.call("PUT", "/api/resumes/${link.rid}", payload(l, null), token)) {
                    is ApiResult.Ok -> pushed++
                    is ApiResult.Fail -> when {
                        r.status == 409 -> {
                            // the account got a newer edit in the meantime
                            val server = r.json?.optJSONObject("resume")
                            if (server != null && l.id != openDraftId) fromRemote(server, l.id, l.avatarUri)?.let { dao.insertResume(it); pulled++ }
                        }
                        r.status == 404 -> links.remove(l.id)
                        r.unauthorized || r.offline -> { saveLinks(links); return@syncBody fail(r) }
                        else -> warning = r.message
                    }
                }
            }
        }

        // 4. resumes only the account has: bring them to the phone
        val knownRids = links.values.map { it.rid }.filter { it.isNotEmpty() }.toSet()
        for (rj in remote) {
            val rid = rj.optString("id")
            if (rid in knownRids || rid in pendingDeletes) continue
            val ref = rj.optString("clientRef", "")
            // a create that worked but whose link was never saved (app closed in between): adopt it
            val adopt = links.entries.firstOrNull { it.value.rid.isEmpty() && ref.isNotEmpty() && it.value.ref == ref }
            if (adopt != null) { links[adopt.key] = Link(rid, ref); continue }
            val r = fromRemote(rj, 0, "") ?: continue
            val newId = dao.insertResume(r).toInt()
            links[newId] = Link(rid, ref)
            pulled++
        }
        saveLinks(links)

        // 5. resumes only the phone has: upload them
        for (l in dao.getAllResumes().first()) {
            val existing = links[l.id]
            if (existing != null && existing.rid.isNotEmpty()) continue
            val ref = existing?.ref?.takeIf { it.isNotEmpty() } ?: newRef()
            links[l.id] = Link("", ref)
            saveLinks(links) // remember the ref BEFORE the call, so a retry cannot create a duplicate
            when (val r = BackendClient.call("POST", "/api/resumes", payload(l, ref), token)) {
                is ApiResult.Ok -> {
                    links[l.id] = Link(r.json.optJSONObject("resume")?.optString("id").orEmpty(), ref)
                    pushed++
                }
                is ApiResult.Fail -> when {
                    r.unauthorized || r.offline -> { saveLinks(links); return@syncBody fail(r) }
                    else -> warning = r.message
                }
            }
        }
        saveLinks(links)
        SyncOutcome.Ok(pulled, pushed, removed, warning)
    }

    private companion object {
        const val KEY_LINKS = "sync_links"
        const val KEY_DELETED = "sync_deleted"
    }
}
