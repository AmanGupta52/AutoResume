package com.example

import com.example.data.ExperienceEntry
import com.example.data.Resume
import com.example.ui.AiGuard
import com.example.ui.ProposalKind
import com.example.ui.ResumeCoach
import com.example.ui.SuggestionAction
import com.example.ui.TargetJob
import com.example.ui.withTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistTest {

    @Test
    fun targetJob_roundTrips_through_the_resume() {
        val r = Resume().withTarget(TargetJob("Data Analyst", "SQL and Tableau"))
        val back = TargetJob.parse(r)
        assertEquals("Data Analyst", back.title)
        assertEquals("SQL and Tableau", back.description)
    }

    @Test
    fun targetJob_empty_is_not_stored() {
        val r = Resume().withTarget(TargetJob("", ""))
        assertEquals("", r.atsFeedback)
        assertFalse(TargetJob.parse(r).isSet)
    }

    @Test
    fun guard_finds_numbers_that_are_not_in_the_resume() {
        val r = Resume(summary = "Grew qualified leads 42% in a year.")
        val fresh = AiGuard.newNumbers("Grew qualified leads 42% and cut cost by 18%.", r)
        assertEquals(listOf("18%"), fresh)
    }

    @Test
    fun guard_detects_bracket_placeholders() {
        assertTrue(AiGuard.hasPlaceholders("Cut report time by [X%]"))
        assertFalse(AiGuard.hasPlaceholders("Cut report time by hand"))
    }

    @Test
    fun template_is_chosen_from_the_job() {
        assertEquals("Developer Resume", ResumeCoach.templateFor("Software Engineer", "").first)
        assertEquals("Minimal ATS", ResumeCoach.templateFor("Accountant", "").first)
        assertEquals("Executive Premium", ResumeCoach.templateFor("Director of Sales", "").first)
        assertEquals("Modern Professional", ResumeCoach.templateFor("", "").first)
    }

    @Test
    fun empty_summary_tab_offers_an_ai_summary_first() {
        val s = ResumeCoach.suggestions(1, Resume(), aiOn = true)
        val first = s.first().action
        assertTrue(first is SuggestionAction.Ai && first.req.kind == ProposalKind.SUMMARY)
    }

    @Test
    fun without_ai_no_suggestion_needs_the_ai() {
        for (tab in 0..8) {
            assertTrue(ResumeCoach.suggestions(tab, Resume(), aiOn = false).none { it.usesAi })
        }
    }

    @Test
    fun years_of_experience_come_from_the_dates() {
        val r = Resume(experience = listOf(ExperienceEntry("Acme", "Analyst", "Jan 2020", "", true, "")))
        assertTrue((ResumeCoach.yearsOfExperience(r) ?: 0) >= 1)
    }
}
