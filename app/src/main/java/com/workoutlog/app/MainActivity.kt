package com.workoutlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.app.data.AppViewModel
import com.workoutlog.app.ui.screens.BodyScreen
import com.workoutlog.app.ui.screens.ChartsScreen
import com.workoutlog.app.ui.screens.DailyLogScreen
import com.workoutlog.app.ui.screens.GoalsScreen
import com.workoutlog.app.ui.screens.ReportScreen
import com.workoutlog.app.ui.screens.SettingsScreen
import com.workoutlog.app.ui.screens.TrackerScreen
import com.workoutlog.app.ui.theme.AppTheme
import com.workoutlog.app.ui.theme.LocalThemeStyle
import com.workoutlog.app.ui.theme.WorkoutLogTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val data by vm.data.collectAsStateWithLifecycle()
            val theme = when (data.settings.theme.ifBlank { BuildConfig.DEFAULT_THEME }) {
                "WOMENS" -> AppTheme.WOMENS
                else -> AppTheme.MENS
            }
            WorkoutLogTheme(theme, data.settings.darkMode) { App(vm, theme) }
        }
    }

    override fun onPause() {
        super.onPause()
        vm.flush()
    }
}

internal enum class Tab(val label: String, val icon: ImageVector) {
    LOG("Log", Icons.Default.FitnessCenter),
    GOALS("Goals", Icons.Default.Flag),
    BODY("Body", Icons.Default.Accessibility),
    TRACK("Tracker", Icons.Default.GridOn),
    CHARTS("Graphs", Icons.AutoMirrored.Filled.ShowChart),
}

internal enum class Overlay { NONE, SETTINGS, REPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App(vm: AppViewModel, theme: AppTheme, startTab: Tab = Tab.LOG, startOverlay: Overlay = Overlay.NONE, startTabIndex: Int = 0, startDate: String = LocalDate.now().toString()) {
    val data by vm.data.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(startTab) }
    var overlay by rememberSaveable { mutableStateOf(startOverlay) }
    var dateStr by rememberSaveable { mutableStateOf(startDate) }
    val style = LocalThemeStyle.current

    BackHandler(enabled = overlay != Overlay.NONE || tab != Tab.LOG) {
        if (overlay != Overlay.NONE) overlay = Overlay.NONE else tab = Tab.LOG
    }

    val title = when (overlay) {
        Overlay.SETTINGS -> "Settings"
        Overlay.REPORT -> "PDF Report"
        Overlay.NONE -> when (tab) {
            Tab.LOG -> "Daily Log"
            Tab.GOALS -> "My Goals"
            Tab.BODY -> "Body Measurements"
            Tab.TRACK -> "Progress Tracker"
            Tab.CHARTS -> "Graphs"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(style.headerTitle(title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (overlay != Overlay.NONE) IconButton(onClick = { overlay = Overlay.NONE }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { overlay = Overlay.REPORT }) { Icon(Icons.Default.PictureAsPdf, "Create PDF report") }
                    IconButton(onClick = { overlay = Overlay.SETTINGS }) { Icon(Icons.Default.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = {
            if (overlay == Overlay.NONE) NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (overlay) {
                Overlay.SETTINGS -> SettingsScreen(vm, data, theme)
                Overlay.REPORT -> ReportScreen(data, theme)
                Overlay.NONE -> when (tab) {
                    Tab.LOG -> DailyLogScreen(vm, data, LocalDate.parse(dateStr), startTabIndex) { dateStr = it.toString() }
                    Tab.GOALS -> GoalsScreen(vm, data)
                    Tab.BODY -> BodyScreen(vm, data)
                    Tab.TRACK -> TrackerScreen(vm, data)
                    Tab.CHARTS -> ChartsScreen(data)
                }
            }
        }
    }
}
