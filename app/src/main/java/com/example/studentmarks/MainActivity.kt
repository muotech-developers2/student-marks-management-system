package com.example.studentmarks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import com.example.studentmarks.data.database.AppDatabase
import com.example.studentmarks.data.repository.AssessmentRepository
import com.example.studentmarks.data.repository.ClassProfileRepository
import com.example.studentmarks.data.repository.ExcelExportRepository
import com.example.studentmarks.data.repository.MarkRepository
import com.example.studentmarks.data.repository.ReportsRepository
import com.example.studentmarks.data.repository.StudentRepository
import com.example.studentmarks.data.repository.SubjectRepository
import com.example.studentmarks.data.repository.TermRepository
import com.example.studentmarks.ui.AppViewModel
import com.example.studentmarks.ui.AppViewModelFactory
import com.example.studentmarks.ui.AssessmentsViewModel
import com.example.studentmarks.ui.AssessmentsViewModelFactory
import com.example.studentmarks.ui.MarksViewModel
import com.example.studentmarks.ui.MarksViewModelFactory
import com.example.studentmarks.ui.ReportsViewModel
import com.example.studentmarks.ui.ReportsViewModelFactory
import com.example.studentmarks.ui.StudentMarksApp
import com.example.studentmarks.ui.StudentsViewModel
import com.example.studentmarks.ui.StudentsViewModelFactory
import com.example.studentmarks.ui.SubjectsViewModel
import com.example.studentmarks.ui.SubjectsViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory(
            ClassProfileRepository(
                AppDatabase.getInstance(applicationContext).classProfileDao(),
                applicationContext,
            ),
        )
    }
    private val studentsViewModel: StudentsViewModel by viewModels {
        StudentsViewModelFactory(
            StudentRepository(
                AppDatabase.getInstance(applicationContext).studentDao(),
            ),
        )
    }
    private val subjectsViewModel: SubjectsViewModel by viewModels {
        SubjectsViewModelFactory(
            SubjectRepository(
                AppDatabase.getInstance(applicationContext).subjectDao(),
            ),
        )
    }
    private val assessmentsViewModel: AssessmentsViewModel by viewModels {
        AssessmentsViewModelFactory(
            AssessmentRepository(
                AppDatabase.getInstance(applicationContext).assessmentDao(),
            ),
            TermRepository(
                AppDatabase.getInstance(applicationContext).termDao(),
            ),
            ClassProfileRepository(
                AppDatabase.getInstance(applicationContext).classProfileDao(),
                applicationContext,
            ),
        )
    }
    private val marksViewModel: MarksViewModel by viewModels {
        MarksViewModelFactory(
            TermRepository(
                AppDatabase.getInstance(applicationContext).termDao(),
            ),
            AssessmentRepository(
                AppDatabase.getInstance(applicationContext).assessmentDao(),
            ),
            SubjectRepository(
                AppDatabase.getInstance(applicationContext).subjectDao(),
            ),
            StudentRepository(
                AppDatabase.getInstance(applicationContext).studentDao(),
            ),
            MarkRepository(
                AppDatabase.getInstance(applicationContext).markDao(),
            ),
            ClassProfileRepository(
                AppDatabase.getInstance(applicationContext).classProfileDao(),
                applicationContext,
            ),
        )
    }
    private val reportsViewModel: ReportsViewModel by viewModels {
        ReportsViewModelFactory(
            TermRepository(
                AppDatabase.getInstance(applicationContext).termDao(),
            ),
            ReportsRepository(
                AppDatabase.getInstance(applicationContext).markDao(),
                AppDatabase.getInstance(applicationContext).studentDao(),
                AppDatabase.getInstance(applicationContext).subjectDao(),
                AppDatabase.getInstance(applicationContext).assessmentDao(),
                AppDatabase.getInstance(applicationContext).termDao(),
            ),
            ExcelExportRepository(
                AppDatabase.getInstance(applicationContext).markDao(),
                AppDatabase.getInstance(applicationContext).studentDao(),
                AppDatabase.getInstance(applicationContext).subjectDao(),
                AppDatabase.getInstance(applicationContext).assessmentDao(),
                AppDatabase.getInstance(applicationContext).termDao(),
                AppDatabase.getInstance(applicationContext).classProfileDao(),
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                StudentMarksApp(viewModel, studentsViewModel, subjectsViewModel, assessmentsViewModel, marksViewModel, reportsViewModel)
            }
        }
    }
}