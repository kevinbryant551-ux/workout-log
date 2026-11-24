package com.workoutlog.app

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.workoutlog.app.data.AppViewModel
import com.workoutlog.app.ui.theme.AppTheme
import com.workoutlog.app.ui.theme.WorkoutLogTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders each screen in both themes to app/build/screenshots as PNG files for visual review. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h1500dp-xhdpi")
class ScreenshotTest(private val theme: AppTheme, private val screen: String) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun params(): List<Array<Any>> = AppTheme.entries.flatMap { t ->
            listOf("workout", "diet", "goals", "body", "tracker", "charts", "report", "settings").map { arrayOf<Any>(t, it) }
        }
    }

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun render() {
        val vm = AppViewModel(ApplicationProvider.getApplicationContext())
        val sample = SampleData.build()
        vm.update { sample }
        val todayLog = sample.logs.keys.max()
        compose.setContent {
            WorkoutLogTheme(theme, if (theme == AppTheme.MENS) "DARK" else "LIGHT") {
                when (screen) {
                    "workout" -> App(vm, theme, Tab.LOG, startTabIndex = 0, startDate = sample.logs.values.filter { it.exercises.size > 1 }.maxOf { it.date })
                    "diet" -> App(vm, theme, Tab.LOG, startTabIndex = 1, startDate = todayLog)
                    "goals" -> App(vm, theme, Tab.GOALS)
                    "body" -> App(vm, theme, Tab.BODY)
                    "tracker" -> App(vm, theme, Tab.TRACK)
                    "charts" -> App(vm, theme, Tab.CHARTS)
                    "report" -> App(vm, theme, startOverlay = Overlay.REPORT)
                    else -> App(vm, theme, startOverlay = Overlay.SETTINGS)
                }
            }
        }
        compose.waitForIdle()
        val root = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "${theme.name.lowercase()}-$screen.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
