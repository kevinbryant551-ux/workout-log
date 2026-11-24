package com.workoutlog.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.workoutlog.app.model.AppData
import com.workoutlog.app.pdf.PdfReport
import com.workoutlog.app.pdf.ReportOptions
import com.workoutlog.app.ui.components.CheckItem
import com.workoutlog.app.ui.components.DateButton
import com.workoutlog.app.ui.components.SectionCard
import com.workoutlog.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

@Composable
fun ReportScreen(data: AppData, theme: AppTheme) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val earliest = remember(data) {
        (data.logs.keys + data.measurements.map { it.date }).filter { it.isNotBlank() }.minOrNull()
            ?: LocalDate.now().minusDays(30).toString()
    }
    var from by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(30).toString()) }
    var to by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var goals by rememberSaveable { mutableStateOf(true) }
    var measurements by rememberSaveable { mutableStateOf(true) }
    var workouts by rememberSaveable { mutableStateOf(true) }
    var nutrition by rememberSaveable { mutableStateOf(true) }
    var trackers by rememberSaveable { mutableStateOf(true) }
    var charts by rememberSaveable { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var lastFile by remember { mutableStateOf<File?>(null) }

    fun fileName() = "workout-log_${from}_to_${to}.pdf"

    suspend fun generate(): File = withContext(Dispatchers.Default) {
        val dir = File(ctx.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, fileName())
        val a = runCatching { LocalDate.parse(from) }.getOrDefault(LocalDate.now().minusDays(30))
        val b = runCatching { LocalDate.parse(to) }.getOrDefault(LocalDate.now())
        PdfReport(
            data, theme,
            ReportOptions(minOf(a, b), maxOf(a, b), goals, measurements, workouts, nutrition, trackers, charts),
        ).writeTo(f)
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val src = lastFile
        if (uri != null && src != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
            Toast.makeText(ctx, "PDF saved", Toast.LENGTH_SHORT).show()
        }
    }

    fun share(f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Share PDF report"))
    }

    fun open(f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { ctx.startActivity(view) } catch (_: Exception) {
            Toast.makeText(ctx, "No PDF viewer installed — use Share or Save instead.", Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        SectionCard("Date range") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateButton("From", from, { if (it.isNotBlank()) from = it }, Modifier.weight(1f))
                DateButton("To", to, { if (it.isNotBlank()) to = it }, Modifier.weight(1f))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val today = LocalDate.now()
                AssistChip(onClick = { from = today.minusDays(7).toString(); to = today.toString() }, label = { Text("Last 7 days") })
                AssistChip(onClick = { from = today.minusDays(30).toString(); to = today.toString() }, label = { Text("30 days") })
                AssistChip(onClick = { from = today.minusDays(90).toString(); to = today.toString() }, label = { Text("90 days") })
                AssistChip(onClick = { from = today.minusYears(1).toString(); to = today.toString() }, label = { Text("1 year") })
                AssistChip(onClick = { from = earliest; to = today.toString() }, label = { Text("Everything") })
            }
        }
        SectionCard("Include") {
            CheckItem("Summary & goals", goals, { goals = it })
            CheckItem("Body measurements & photos", measurements, { measurements = it })
            CheckItem("Daily workout logs", workouts, { workouts = it })
            CheckItem("Daily diet / nutrition logs", nutrition, { nutrition = it })
            CheckItem("Progress tracker", trackers, { trackers = it })
            CheckItem("Graphs over time", charts, { charts = it })
        }
        Button(
            onClick = {
                busy = true
                scope.launch {
                    try {
                        lastFile = generate()
                        lastFile?.let { open(it) }
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Could not create PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally { busy = false }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.PictureAsPdf, null)
            Text("  Create PDF")
        }
        lastFile?.let { f ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { saveLauncher.launch(f.name) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Save, null); Text("  Save")
                }
                OutlinedButton(onClick = { share(f) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, null); Text("  Share")
                }
            }
            Text(
                "Created ${f.name}. Save puts it in a folder you choose (e.g. Downloads); Share sends it by email, Drive, messages, etc.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
