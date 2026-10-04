package com.example.studentmarks.data.repository

import com.example.studentmarks.data.database.AssessmentDao
import com.example.studentmarks.data.database.AssessmentEntity
import com.example.studentmarks.data.database.ClassProfileDao
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.MarkDao
import com.example.studentmarks.data.database.MarkEntity
import com.example.studentmarks.data.database.StudentDao
import com.example.studentmarks.data.database.StudentEntity
import com.example.studentmarks.data.database.SubjectDao
import com.example.studentmarks.data.database.SubjectEntity
import com.example.studentmarks.data.database.TermDao
import com.example.studentmarks.data.database.TermEntity
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExcelExportRepository(
    private val markDao: MarkDao,
    private val studentDao: StudentDao,
    private val subjectDao: SubjectDao,
    private val assessmentDao: AssessmentDao,
    private val termDao: TermDao,
    private val profileDao: ClassProfileDao,
) {
    suspend fun generateStudentWorkbook(
        profileId: Long,
        termId: Long,
        studentId: Long,
        assessmentId: Long? = null,
    ): ByteArray {
        val profile = profileDao.getProfileById(profileId) ?: throw IllegalArgumentException("Profile not found: $profileId")
        val term = termDao.getTermsForProfile(profileId).firstOrNull { it.id == termId }
            ?: throw IllegalArgumentException("Term not found: $termId")
        val student = studentDao.getStudent(studentId, profileId)
            ?: throw IllegalArgumentException("Student not found: $studentId")
        val assessmentList = assessmentDao.getAssessmentsForTerm(profileId, termId)
        val selectedAssessment = assessmentId?.let { assessmentList.firstOrNull { it.id == assessmentId } }
        val marks = if (selectedAssessment != null) {
            markDao.getMarksForAssessmentByProfileAndTerm(profileId, termId, selectedAssessment.id)
        } else {
            markDao.getMarksForStudentTerm(profileId, termId, studentId)
        }
        val subjects = subjectDao.getSubjectsForProfile(profileId).filter { it.isActive }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(safeSheetName("Student Marks"))

        writeHeaderBlock(
            sheet = sheet,
            workbook = workbook,
            title = "Student Marks Report",
            rows = listOf(
                "School/Profile" to profile.classGrade,
                "Teacher" to profile.teacherName,
                "Term" to "Term ${term.termNumber}",
                "Student" to student.name,
                "Assessment" to (selectedAssessment?.name ?: "All Assessments"),
            ),
        )

        val headerRow = sheet.createRow(6)
        headerRow.createCell(0).setCellValue("Subject")
        if (selectedAssessment == null) {
            val assessmentNames = assessmentList.sortedBy { it.sortOrder }.sortedBy { it.name }
            assessmentNames.forEachIndexed { index, assessment ->
                headerRow.createCell(index + 1).setCellValue(assessment.name)
            }
            subjects.forEachIndexed { index, subject ->
                val row = sheet.createRow(7 + index)
                row.createCell(0).setCellValue(subject.name)
                val subjectMarks = marks.filter { it.subjectId == subject.id }
                assessmentNames.forEachIndexed { assessmentIndex, assessment ->
                    val markValue = subjectMarks.firstOrNull { it.assessmentId == assessment.id }?.markValue
                    setCellValue(row.createCell(assessmentIndex + 1), markValue)
                }
            }
        } else {
            headerRow.createCell(1).setCellValue("Mark")
            subjects.forEachIndexed { index, subject ->
                val row = sheet.createRow(7 + index)
                row.createCell(0).setCellValue(subject.name)
                val markValue = marks.firstOrNull { it.subjectId == subject.id && it.studentId == student.id }?.markValue
                setCellValue(row.createCell(1), markValue)
            }
        }

        applyColumnWidths(sheet, 0 to 20, 1 to 14, 2 to 14, 3 to 14, 4 to 14, 5 to 14)
        return workbook.toByteArray()
    }

    suspend fun generateSubjectWorkbook(
        profileId: Long,
        termId: Long,
        subjectId: Long,
        assessmentId: Long? = null,
    ): ByteArray {
        val profile = profileDao.getProfileById(profileId) ?: throw IllegalArgumentException("Profile not found: $profileId")
        val term = termDao.getTermsForProfile(profileId).firstOrNull { it.id == termId }
            ?: throw IllegalArgumentException("Term not found: $termId")
        val subject = subjectDao.getSubject(subjectId, profileId)
            ?: throw IllegalArgumentException("Subject not found: $subjectId")
        val assessmentList = assessmentDao.getAssessmentsForTerm(profileId, termId)
        val selectedAssessment = assessmentId?.let { assessmentList.firstOrNull { it.id == assessmentId } }
        val marks = if (selectedAssessment != null) {
            markDao.getMarksForAssessmentByProfileAndTerm(profileId, termId, selectedAssessment.id)
        } else {
            markDao.getMarksForSubjectTerm(profileId, termId, subjectId)
        }
        val students = studentDao.getStudentsForProfile(profileId).filter { it.isActive }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(safeSheetName("Subject Marks"))

        writeHeaderBlock(
            sheet = sheet,
            workbook = workbook,
            title = "Subject Report",
            rows = listOf(
                "School/Profile" to profile.classGrade,
                "Teacher" to profile.teacherName,
                "Term" to "Term ${term.termNumber}",
                "Subject" to subject.name,
                "Assessment" to (selectedAssessment?.name ?: "All Assessments"),
            ),
        )

        val headerRow = sheet.createRow(6)
        headerRow.createCell(0).setCellValue("Student")
        if (selectedAssessment == null) {
            val assessmentNames = assessmentList.sortedBy { it.sortOrder }.sortedBy { it.name }
            assessmentNames.forEachIndexed { index, assessment ->
                headerRow.createCell(index + 1).setCellValue(assessment.name)
            }
            students.forEachIndexed { index, student ->
                val row = sheet.createRow(7 + index)
                row.createCell(0).setCellValue(student.name)
                val studentMarks = marks.filter { it.studentId == student.id }
                assessmentNames.forEachIndexed { assessmentIndex, assessment ->
                    val markValue = studentMarks.firstOrNull { it.assessmentId == assessment.id }?.markValue
                    setCellValue(row.createCell(assessmentIndex + 1), markValue)
                }
            }
        } else {
            headerRow.createCell(1).setCellValue("Mark")
            students.forEachIndexed { index, student ->
                val row = sheet.createRow(7 + index)
                row.createCell(0).setCellValue(student.name)
                val markValue = marks.firstOrNull { it.studentId == student.id && it.subjectId == subjectId }?.markValue
                setCellValue(row.createCell(1), markValue)
            }
        }

        return workbook.toByteArray()
    }

    suspend fun generateClassWorkbook(
        profileId: Long,
        termId: Long,
        assessmentId: Long? = null,
        includeInactiveStudents: Boolean = false,
        includeInactiveSubjects: Boolean = false,
    ): ByteArray {
        val profile = profileDao.getProfileById(profileId) ?: throw IllegalArgumentException("Profile not found: $profileId")
        val term = termDao.getTermsForProfile(profileId).firstOrNull { it.id == termId }
            ?: throw IllegalArgumentException("Term not found: $termId")
        val selectedAssessment = assessmentId?.let { assessmentDao.getAssessmentsForTerm(profileId, termId).firstOrNull { it.id == assessmentId } }
        val students = studentDao.getStudentsForProfile(profileId).filter { if (includeInactiveStudents) true else it.isActive }
        val subjects = subjectDao.getSubjectsForProfile(profileId).filter { if (includeInactiveSubjects) true else it.isActive }
        val marks = if (selectedAssessment != null) {
            markDao.getMarksForAssessmentByProfileAndTerm(profileId, termId, selectedAssessment.id)
        } else {
            markDao.getMarksForProfileAndTerm(profileId, termId)
        }

        val workbook = XSSFWorkbook()
        val classSheet = workbook.createSheet(safeSheetName("Class Marks"))
        writeHeaderBlock(
            sheet = classSheet,
            workbook = workbook,
            title = "Class Marks",
            rows = listOf(
                "School/Profile" to profile.classGrade,
                "Teacher" to profile.teacherName,
                "Term" to "Term ${term.termNumber}",
                "Assessment" to (selectedAssessment?.name ?: "All Assessments"),
            ),
        )

        val headerRow = classSheet.createRow(6)
        headerRow.createCell(0).setCellValue("Student")
        subjects.forEachIndexed { index, subject ->
            headerRow.createCell(index + 1).setCellValue(subject.name)
        }

        students.forEachIndexed { studentIndex, student ->
            val row = classSheet.createRow(7 + studentIndex)
            row.createCell(0).setCellValue(student.name)
            subjects.forEachIndexed { subjectIndex, subject ->
                val value = marks.firstOrNull { it.studentId == student.id && it.subjectId == subject.id }?.markValue
                setCellValue(row.createCell(subjectIndex + 1), value)
            }
        }

        val summarySheet = workbook.createSheet(safeSheetName("Assessment Summary"))
        writeSummarySheet(summarySheet, profile, term, selectedAssessment, subjects, students, marks)

        val missingSheet = workbook.createSheet(safeSheetName("Missing Marks"))
        writeMissingSheet(missingSheet, profile, term, selectedAssessment, subjects, students, marks)

        val studentSheet = workbook.createSheet(safeSheetName("Students"))
        writeStudentListSheet(studentSheet, students)

        val subjectSheet = workbook.createSheet(safeSheetName("Subjects"))
        writeSubjectListSheet(subjectSheet, subjects)

        return workbook.toByteArray()
    }

    suspend fun generateBlankTemplate(
        profileId: Long,
        termId: Long,
        assessmentId: Long? = null,
        includeInactiveStudents: Boolean = false,
        includeInactiveSubjects: Boolean = false,
    ): ByteArray {
        val profile = profileDao.getProfileById(profileId) ?: throw IllegalArgumentException("Profile not found: $profileId")
        val term = termDao.getTermsForProfile(profileId).firstOrNull { it.id == termId }
            ?: throw IllegalArgumentException("Term not found: $termId")
        val selectedAssessment = assessmentId?.let { assessmentDao.getAssessmentsForTerm(profileId, termId).firstOrNull { it.id == assessmentId } }
        val students = studentDao.getStudentsForProfile(profileId).filter { if (includeInactiveStudents) true else it.isActive }
        val subjects = subjectDao.getSubjectsForProfile(profileId).filter { if (includeInactiveSubjects) true else it.isActive }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(safeSheetName("Template"))
        writeHeaderBlock(
            sheet = sheet,
            workbook = workbook,
            title = "Blank Mark Template",
            rows = listOf(
                "School/Profile" to profile.classGrade,
                "Teacher" to profile.teacherName,
                "Term" to "Term ${term.termNumber}",
                "Assessment" to (selectedAssessment?.name ?: "All Assessments"),
            ),
        )

        val headerRow = sheet.createRow(6)
        headerRow.createCell(0).setCellValue("Student")
        subjects.forEachIndexed { index, subject ->
            headerRow.createCell(index + 1).setCellValue(subject.name)
        }

        students.forEachIndexed { index, student ->
            val row = sheet.createRow(7 + index)
            row.createCell(0).setCellValue(student.name)
            subjects.forEachIndexed { subjectIndex, _ ->
                row.createCell(subjectIndex + 1)
            }
        }

        return workbook.toByteArray()
    }

    fun buildFileName(
        profileClass: String,
        exportType: String,
        termNumber: Int,
        assessmentName: String? = null,
    ): String {
        val baseName = listOfNotNull(
            profileClass.trim().ifEmpty { "Class" },
            "Term",
            termNumber.toString(),
            assessmentName?.takeIf { it.isNotBlank() }?.let { sanitizeFileName(it) },
            exportType,
        ).joinToString("_")
        return "${sanitizeFileName(baseName)}.xlsx"
    }

    private fun writeHeaderBlock(
        sheet: Sheet,
        workbook: Workbook,
        title: String,
        rows: List<Pair<String, String>>,
    ) {
        val titleRow = sheet.createRow(0)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue(title)
        val titleStyle = workbook.createCellStyle()
        val titleFont = workbook.createFont()
        titleFont.bold = true
        titleFont.fontHeightInPoints = 12
        titleStyle.setFont(titleFont)
        titleCell.cellStyle = titleStyle

        var rowIndex = 2
        rows.forEach { (label, value) ->
            val row = sheet.createRow(rowIndex)
            row.createCell(0).setCellValue(label)
            row.createCell(1).setCellValue(value)
            rowIndex += 1
        }
    }

    private fun writeSummarySheet(
        summarySheet: Sheet,
        profile: ClassProfileEntity,
        term: TermEntity,
        selectedAssessment: AssessmentEntity?,
        subjects: List<SubjectEntity>,
        students: List<StudentEntity>,
        marks: List<MarkEntity>,
    ) {
        val headerRow = summarySheet.createRow(0)
        headerRow.createCell(0).setCellValue("Subject")
        headerRow.createCell(1).setCellValue("Entered")
        headerRow.createCell(2).setCellValue("Missing")
        headerRow.createCell(3).setCellValue("Average")
        headerRow.createCell(4).setCellValue("Highest")
        headerRow.createCell(5).setCellValue("Lowest")

        subjects.forEachIndexed { index, subject ->
            val subjectMarks = marks.filter { it.subjectId == subject.id }
            val values = subjectMarks.map { it.markValue }
            val row = summarySheet.createRow(index + 1)
            row.createCell(0).setCellValue(subject.name)
            row.createCell(1).setCellValue(subjectMarks.size.toDouble())
            row.createCell(2).setCellValue((students.size - subjectMarks.size).coerceAtLeast(0).toDouble())
            row.createCell(3).setCellValue(values.averageOrNull() ?: Double.NaN)
            row.createCell(4).setCellValue(values.maxOrNull() ?: Double.NaN)
            row.createCell(5).setCellValue(values.minOrNull() ?: Double.NaN)
        }
        summarySheet.createRow(subjects.size + 2).createCell(0).setCellValue("Profile")
        summarySheet.getRow(subjects.size + 2).createCell(1).setCellValue(profile.classGrade)
        summarySheet.createRow(subjects.size + 3).createCell(0).setCellValue("Term")
        summarySheet.getRow(subjects.size + 3).createCell(1).setCellValue("Term ${term.termNumber}")
        summarySheet.createRow(subjects.size + 4).createCell(0).setCellValue("Assessment")
        summarySheet.getRow(subjects.size + 4).createCell(1).setCellValue(selectedAssessment?.name ?: "All Assessments")
    }

    private fun writeMissingSheet(
        missingSheet: Sheet,
        profile: ClassProfileEntity,
        term: TermEntity,
        selectedAssessment: AssessmentEntity?,
        subjects: List<SubjectEntity>,
        students: List<StudentEntity>,
        marks: List<MarkEntity>,
    ) {
        val headerRow = missingSheet.createRow(0)
        headerRow.createCell(0).setCellValue("Student")
        headerRow.createCell(1).setCellValue("Subject")
        headerRow.createCell(2).setCellValue("Assessment")

        var rowIndex = 1
        students.forEach { student ->
            subjects.forEach { subject ->
                val correspondingMark = marks.firstOrNull { it.studentId == student.id && it.subjectId == subject.id }
                if (correspondingMark == null) {
                    val row = missingSheet.createRow(rowIndex)
                    row.createCell(0).setCellValue(student.name)
                    row.createCell(1).setCellValue(subject.name)
                    row.createCell(2).setCellValue(selectedAssessment?.name ?: "All Assessments")
                    rowIndex += 1
                }
            }
        }

        if (rowIndex == 1) {
            val row = missingSheet.createRow(1)
            row.createCell(0).setCellValue("No missing marks")
        }
    }

    private fun writeStudentListSheet(studentSheet: Sheet, students: List<StudentEntity>) {
        val headerRow = studentSheet.createRow(0)
        headerRow.createCell(0).setCellValue("Student Name")
        headerRow.createCell(1).setCellValue("Status")
        students.forEachIndexed { index, student ->
            val row = studentSheet.createRow(index + 1)
            row.createCell(0).setCellValue(student.name)
            row.createCell(1).setCellValue(if (student.isActive) "Active" else "Inactive")
        }
    }

    private fun writeSubjectListSheet(subjectSheet: Sheet, subjects: List<SubjectEntity>) {
        val headerRow = subjectSheet.createRow(0)
        headerRow.createCell(0).setCellValue("Subject Name")
        headerRow.createCell(1).setCellValue("Status")
        subjects.forEachIndexed { index, subject ->
            val row = subjectSheet.createRow(index + 1)
            row.createCell(0).setCellValue(subject.name)
            row.createCell(1).setCellValue(if (subject.isActive) "Active" else "Inactive")
        }
    }

    private fun setCellValue(cell: Cell, markValue: Double?) {
        if (markValue == null) {
            cell.setCellValue("—")
            return
        }
        if (markValue == 0.0) {
            cell.setCellValue(0.0)
        } else {
            cell.setCellValue(markValue)
        }
    }

    private fun applyColumnWidths(sheet: Sheet, vararg widths: Pair<Int, Int>) {
        widths.forEach { (index, width) ->
            sheet.setColumnWidth(index, width * 256)
        }
    }

    private fun safeSheetName(value: String): String = WorkbookUtil.createSafeSheetName(value)

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private data class WorkbookSheet(
        val name: String,
        val rows: List<List<String?>>, 
    )

    private fun Workbook.toByteArray(): ByteArray {
        val sheets = this.sheetIterator().asSequence().map { sheet ->
            val rows = sheet.map { row ->
                row.map { cell ->
                    when (cell.cellType) {
                        CellType.BLANK -> null
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.FORMULA -> cell.cellFormula ?: ""
                        CellType.ERROR -> cell.errorCellValue.toString()
                        else -> ""
                    }
                }
            }
            WorkbookSheet(sheet.sheetName, rows)
        }.toList()

        return buildMinimalXlsx(sheets)
    }

    private fun buildMinimalXlsx(sheets: List<WorkbookSheet>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                  ${sheets.indices.joinToString(separator = "") { i ->
                        val sheetIndex = i + 1
                        "<Override PartName=\"/xl/worksheets/sheet${sheetIndex}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    }}
                </Types>
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                </Relationships>
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("docProps/core.xml"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <dc:creator>Student Marks App</dc:creator>
                  <cp:lastModifiedBy>Student Marks App</cp:lastModifiedBy>
                  <dcterms:created xsi:type="dcterms:W3CDTF">2026-01-01T00:00:00Z</dcterms:created>
                  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-01-01T00:00:00Z</dcterms:modified>
                </cp:coreProperties>
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("docProps/app.xml"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                  <Application>Student Marks App</Application>
                </Properties>
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    ${sheets.indices.joinToString(separator = "") { i ->
                        val sheetIndex = i + 1
                        "<sheet name=\"${escapeXml(sheets[i].name)}\" sheetId=\"${sheetIndex}\" r:id=\"rId${sheetIndex}\"/>"
                    }}
                  </sheets>
                </workbook>
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  ${sheets.indices.joinToString(separator = "") { i ->
                        val sheetIndex = i + 1
                        "<Relationship Id=\"rId${sheetIndex}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${sheetIndex}.xml\"/>"
                    }}
                </Relationships>
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            sheets.forEachIndexed { index, sheet ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${index + 1}.xml"))
                zip.write(renderWorksheetXml(sheet).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun renderWorksheetXml(sheet: WorkbookSheet): String {
        val rowsXml = sheet.rows.mapIndexed { rowIndex, row ->
            val rowXml = row.mapIndexed { colIndex, value ->
                val columnLabel = columnLabel(colIndex)
                val cellRef = "$columnLabel${rowIndex + 1}"
                if (value == null) {
                    "<c r=\"$cellRef\"/>"
                } else {
                    val escapedValue = escapeXml(value)
                    "<c r=\"$cellRef\" t=\"inlineStr\"><is><t>${escapedValue}</t></is></c>"
                }
            }.joinToString("")
            "<row r=\"${rowIndex + 1}\">$rowXml</row>"
        }.joinToString("")

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData>
            $rowsXml
          </sheetData>
        </worksheet>
        """.trimIndent()
    }

    private fun columnLabel(index: Int): String {
        var result = ""
        var current = index
        while (current >= 0) {
            val remainder = current % 26
            result = (('A'.code + remainder).toChar()) + result
            current = (current / 26) - 1
        }
        return result
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun sanitizeFileName(value: String): String = value
        .replace(Regex("[^A-Za-z0-9._ -]+"), "_")
        .replace("\\s+".toRegex(), "_")
        .trim('_')
        .ifBlank { "class_export" }
        .take(80)
}
