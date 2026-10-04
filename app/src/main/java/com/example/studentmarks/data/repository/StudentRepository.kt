package com.example.studentmarks.data.repository

import com.example.studentmarks.data.database.StudentDao
import com.example.studentmarks.data.database.StudentEntity
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentRepository(
    private val dao: StudentDao,
) {
    fun observeStudents(profileId: Long, isActive: Boolean): Flow<List<StudentEntity>> =
        dao.observeStudents(profileId, isActive)

    fun searchStudents(
        profileId: Long,
        isActive: Boolean,
        query: String,
    ): Flow<List<StudentEntity>> = dao.observeStudents(profileId, isActive).map { students ->
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) {
            students
        } else {
            students.filter { normalize(it.name).contains(normalizedQuery) }
        }
    }

    fun observeCount(profileId: Long, isActive: Boolean): Flow<Int> =
        dao.observeCount(profileId, isActive)

    suspend fun getStudentsForProfile(profileId: Long): List<StudentEntity> =
        dao.getStudentsForProfile(profileId)

    suspend fun hasDuplicateName(
        profileId: Long,
        name: String,
        excludedStudentId: Long? = null,
    ): Boolean {
        val normalizedName = normalize(name)
        return dao.getStudentsForProfile(profileId).any { student ->
            student.id != excludedStudentId && normalize(student.name) == normalizedName
        }
    }

    suspend fun addStudent(
        profileId: Long,
        name: String,
        now: Long,
    ): Long = dao.insertStudent(
        StudentEntity(
            profileId = profileId,
            name = name,
            isActive = true,
            createdAtMillis = now,
            updatedAtMillis = now,
        ),
    )

    suspend fun updateStudentName(
        studentId: Long,
        profileId: Long,
        name: String,
        now: Long,
    ): Int = dao.updateStudentName(studentId, profileId, name, now)

    suspend fun setStudentActive(
        studentId: Long,
        profileId: Long,
        isActive: Boolean,
        now: Long,
    ): Int = dao.setStudentActive(studentId, profileId, isActive, now)

    suspend fun deleteStudent(studentId: Long, profileId: Long): Int =
        dao.deleteStudent(studentId, profileId)

    private fun normalize(value: String): String =
        value.filterNot(Char::isWhitespace).lowercase(Locale.ROOT)
}