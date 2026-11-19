package com.example

import com.example.data.ExperienceEntry
import com.example.data.Resume
import com.example.ui.DesignOptions
import com.example.ui.IssueLevel
import com.example.ui.LinkedInImport
import com.example.ui.WritingCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingCheckTest {
    @Test
    fun flagsWeakPhraseMisspellingAndMissingNumbers() {
        val r = Resume(
            fullName = "A", email = "a@b.com", phone = "1", summary = "x".repeat(80),
            experience = listOf(ExperienceEntry("Co", "Dev", "2020", "2021", false, "• Responsible for the managment of things\n• Worked on stuff for teams")),
        )
        val issues = WritingCheck.analyze(r)
        assertTrue(issues.any { it.problem.contains("responsible for") })
        assertTrue(issues.any { it.problem.contains("managment") && it.level == IssueLevel.HIGH })
        assertTrue(issues.any { it.problem.contains("contain a number") })
    }

    @Test
    fun invalidEmailIsHigh() {
        val issues = WritingCheck.analyze(Resume(email = "nope"))
        assertTrue(issues.any { it.where == "Email" && it.level == IssueLevel.HIGH })
    }

    @Test
    fun designOptionsRoundTripAndLegacyValues() {
        assertEquals("Compact", DesignOptions.parse("Compact").size)
        val o = DesignOptions(size = "Large", font = "Lora", order = DesignOptions.DEFAULT_ORDER.reversed(), tight = true)
        assertEquals(o, DesignOptions.parse(o.encode()))
        assertEquals(DesignOptions.DEFAULT_ORDER.size, DesignOptions.parse("Standard;order=skills").order.size)
        // values saved by older versions still map to the new fonts
        assertEquals("Lora", DesignOptions.parse("Standard;font=Serif").font)
        assertEquals("Inter", DesignOptions.parse("Standard;font=Sans").font)
        assertEquals("Default", DesignOptions.parse("Standard;font=Mono").font)
    }

    @Test
    fun csvParserHandlesQuotesAndNewlines() {
        val rows = LinkedInImport.parseCsv("\uFEFFName,Desc\n\"Acme, Inc\",\"line1\nline2 \"\"quoted\"\"\"\n")
        assertEquals(listOf("Name", "Desc"), rows[0])
        assertEquals("Acme, Inc", rows[1][0])
        assertEquals("line1\nline2 \"quoted\"", rows[1][1])
    }
}
