package com.example.studentmarks.data.repository

import android.content.Context
import com.example.studentmarks.data.database.ClassProfileDao
import com.example.studentmarks.data.database.ClassProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ClassProfileRepository(
    private val dao: ClassProfileDao,
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _activeProfileId = MutableStateFlow(
        if (preferences.contains(ACTIVE_PROFILE_KEY)) {
            preferences.getLong(ACTIVE_PROFILE_KEY, 0L)
        } else {
            null
        },
    )
    val activeProfileId: StateFlow<Long?> = _activeProfileId.asStateFlow()

    fun observeProfiles(): Flow<List<ClassProfileEntity>> = dao.observeProfiles()

    suspend fun getAllProfiles(): List<ClassProfileEntity> = dao.getAllProfiles()

    suspend fun getProfileById(profileId: Long): ClassProfileEntity? = dao.getProfileById(profileId)

    suspend fun createProfile(profile: ClassProfileEntity): Long = dao.insertProfile(profile)

    suspend fun updateProfile(
        profileId: Long,
        teacherName: String,
        classGrade: String,
        currentTerm: Int,
    ): Int = dao.updateProfileById(profileId, teacherName, classGrade, currentTerm)

    suspend fun updateCurrentTerm(profileId: Long, currentTerm: Int): Int =
        dao.updateCurrentTerm(profileId, currentTerm)

    suspend fun deleteProfile(profileId: Long): Int = dao.deleteProfileById(profileId)

    fun setActiveProfileId(profileId: Long?) {
        val editor = preferences.edit()
        if (profileId == null) {
            editor.remove(ACTIVE_PROFILE_KEY)
        } else {
            editor.putLong(ACTIVE_PROFILE_KEY, profileId)
        }
        editor.apply()
        _activeProfileId.value = profileId
    }

    private companion object {
        const val PREFERENCES_NAME = "student_marks_preferences"
        const val ACTIVE_PROFILE_KEY = "active_profile_id"
    }
}