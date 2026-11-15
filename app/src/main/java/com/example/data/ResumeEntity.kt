package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// Experience entry
data class ExperienceEntry(
    val company: String = "",
    val role: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isCurrent: Boolean = false,
    val description: String = "" // bullet points separated by newline
)

// Education entry
data class EducationEntry(
    val institution: String = "",
    val degree: String = "",
    val startYear: String = "",
    val endYear: String = "",
    val grade: String = "",
    val achievements: String = ""
)

// Skill entry
data class SkillEntry(
    val name: String = "",
    val level: String = "Intermediate", // Beginner, Intermediate, Expert
    val category: String = "Technical" // Technical, Soft, Tools & Technologies
)

// Project entry
data class ProjectEntry(
    val title: String = "",
    val techStack: String = "",
    val description: String = "",
    val githubLink: String = "",
    val liveLink: String = ""
)

// Certification entry
data class CertificationEntry(
    val name: String = "",
    val issuer: String = "",
    val date: String = "",
    val credentialId: String = "",
    val url: String = ""
)

// Language entry
data class LanguageEntry(
    val name: String = "",
    val proficiency: String = "Fluent" // Native, Fluent, Intermediate, Basic
)

@Entity(tableName = "resumes")
data class Resume(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "", // Resume title e.g. "My Software Engineer Resume"
    
    // Personal Info
    val fullName: String = "",
    val jobTitle: String = "",
    val phone: String = "",
    val email: String = "",
    val location: String = "",
    val linkedinUrl: String = "",
    val githubUrl: String = "",
    val portfolioUrl: String = "",
    val avatarUri: String = "", // image uri or base64 placeholder

    // Professional Summary
    val summary: String = "",

    // Multi-entry elements (stored as serialized JSON strings via converters)
    val education: List<EducationEntry> = emptyList(),
    val experience: List<ExperienceEntry> = emptyList(),
    val skills: List<SkillEntry> = emptyList(),
    val projects: List<ProjectEntry> = emptyList(),
    val certifications: List<CertificationEntry> = emptyList(),
    val languages: List<LanguageEntry> = emptyList(),

    // Achievements
    val achievements: String = "", // awards, pubs, volunteering, hobbies etc.

    // Style and templates settings
    val templateId: String = "Modern Professional", // Preconfigured dynamic style rules
    val colorSchemeId: String = "Blue", // Blue, Green, Cyan, Slate, Dark
    val fontSize: String = "Standard", // Compact, Standard, Large
    val margins: String = "Standard", // Narrow, Standard, Wide
    val lastModified: Long = System.currentTimeMillis(),

    // ATS Checklist/Cache
    val atsScore: Int = 0,
    val atsFeedback: String = "" // json or text cached analysis
)

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromEducationList(value: List<EducationEntry>?): String {
        val type = Types.newParameterizedType(List::class.java, EducationEntry::class.java)
        return moshi.adapter<List<EducationEntry>>(type).toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toEducationList(value: String): List<EducationEntry> {
        val type = Types.newParameterizedType(List::class.java, EducationEntry::class.java)
        return moshi.adapter<List<EducationEntry>>(type).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromExperienceList(value: List<ExperienceEntry>?): String {
        val type = Types.newParameterizedType(List::class.java, ExperienceEntry::class.java)
        return moshi.adapter<List<ExperienceEntry>>(type).toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toExperienceList(value: String): List<ExperienceEntry> {
        val type = Types.newParameterizedType(List::class.java, ExperienceEntry::class.java)
        return moshi.adapter<List<ExperienceEntry>>(type).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromSkillList(value: List<SkillEntry>?): String {
        val type = Types.newParameterizedType(List::class.java, SkillEntry::class.java)
        return moshi.adapter<List<SkillEntry>>(type).toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toSkillList(value: String): List<SkillEntry> {
        val type = Types.newParameterizedType(List::class.java, SkillEntry::class.java)
        return moshi.adapter<List<SkillEntry>>(type).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromProjectList(value: List<ProjectEntry>?): String {
        val type = Types.newParameterizedType(List::class.java, ProjectEntry::class.java)
        return moshi.adapter<List<ProjectEntry>>(type).toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toProjectList(value: String): List<ProjectEntry> {
        val type = Types.newParameterizedType(List::class.java, ProjectEntry::class.java)
        return moshi.adapter<List<ProjectEntry>>(type).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromCertificationList(value: List<CertificationEntry>?): String {
        val type = Types.newParameterizedType(List::class.java, CertificationEntry::class.java)
        return moshi.adapter<List<CertificationEntry>>(type).toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toCertificationList(value: String): List<CertificationEntry> {
        val type = Types.newParameterizedType(List::class.java, CertificationEntry::class.java)
        return moshi.adapter<List<CertificationEntry>>(type).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromLanguageList(value: List<LanguageEntry>?): String {
        val type = Types.newParameterizedType(List::class.java, LanguageEntry::class.java)
        return moshi.adapter<List<LanguageEntry>>(type).toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toLanguageList(value: String): List<LanguageEntry> {
        val type = Types.newParameterizedType(List::class.java, LanguageEntry::class.java)
        return moshi.adapter<List<LanguageEntry>>(type).fromJson(value) ?: emptyList()
    }
}

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resumes ORDER BY lastModified DESC")
    fun getAllResumes(): Flow<List<Resume>>

    @Query("SELECT * FROM resumes WHERE id = :id")
    suspend fun getResumeById(id: Int): Resume?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: Resume): Long

    @Update
    suspend fun updateResume(resume: Resume)

    @Delete
    suspend fun deleteResume(resume: Resume)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteResumeById(id: Int)
}

@Database(entities = [Resume::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao
}
