package com.workoutlog.app

import android.graphics.Bitmap
import android.graphics.Canvas
import com.workoutlog.app.model.minutes
import com.workoutlog.app.pdf.PageSink
import com.workoutlog.app.pdf.PdfReport
import com.workoutlog.app.pdf.ReportOptions
import com.workoutlog.app.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws each report page into a PNG (Robolectric has no real PdfDocument). */
private class PngSink(private val dir: File, private val prefix: String) : PageSink {
    var pages = 0
    private var bmp: Bitmap? = null
    override fun startPage(number: Int, width: Int, height: Int): Canvas {
        pages = number
        val b = Bitmap.createBitmap(width * 2, height * 2, Bitmap.Config.ARGB_8888)
        bmp = b
        return Canvas(b).apply { scale(2f, 2f) }
    }
    override fun finishPage() {
        File(dir, "$prefix-page${pages.toString().padStart(2, '0')}.png").outputStream().use {
            bmp!!.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    override fun close() {}
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PdfReportTest {
    @Test
    fun rendersReportPagesForBothThemes() {
        val data = SampleData.build()
        val dir = File("build/reports-sample").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        for (t in AppTheme.entries) {
            val sink = PngSink(dir, t.name.lowercase())
            PdfReport(data, t, ReportOptions(SampleData.today.minusDays(14), SampleData.today)).write(sink)
            assertTrue("expected several pages, got ${sink.pages}", sink.pages >= 4)
        }
    }

    @Test
    fun parsesTimes() {
        assertEquals(13.5, "13:30".minutes()!!, 1e-9)
        assertEquals(65.0, "1:05:00".minutes()!!, 1e-9)
        assertEquals(45.0, "45".minutes()!!, 1e-9)
    }
}
