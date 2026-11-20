package com.example.ui

import com.example.data.Resume

/**
 * The job the person is applying for. Stored inside the (previously unused) `atsFeedback` column,
 * so no database migration is needed and duplicating a resume carries it along automatically.
 * Format: "T1" + US + title + US + description   (US = unit separator, never typed by a person)
 */
data class TargetJob(val title: String = "", val description: String = "") {
    val isSet: Boolean get() = title.isNotBlank() || description.isNotBlank()

    fun encode(): String = if (!isSet) "" else "T1$SEP${title.trim()}$SEP${description.trim()}"

    companion object {
        private const val SEP = "\u001F"

        fun parse(raw: String): TargetJob {
            if (!raw.startsWith("T1$SEP")) return TargetJob()
            val p = raw.split(SEP)
            return TargetJob(p.getOrNull(1).orEmpty(), p.getOrNull(2).orEmpty())
        }

        fun parse(r: Resume): TargetJob = parse(r.atsFeedback)
    }
}

fun Resume.withTarget(t: TargetJob): Resume = copy(atsFeedback = t.encode())
