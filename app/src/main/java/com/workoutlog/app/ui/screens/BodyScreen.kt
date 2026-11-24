package com.workoutlog.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workoutlog.app.data.AppViewModel
import com.workoutlog.app.data.loadPhoto
import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.Measurement
import com.workoutlog.app.model.Settings
import com.workoutlog.app.ui.components.DateButton
import com.workoutlog.app.ui.components.Field
import com.workoutlog.app.ui.components.SectionCard
import com.workoutlog.app.ui.components.SmallLabel
import com.workoutlog.app.ui.components.prettyDate
import java.time.LocalDate

/** All measurement rows from the paper table: label, unit, getter, setter. */
data class MeasureField(
    val label: String,
    val unit: (Settings) -> String,
    val get: (Measurement) -> String,
    val set: (Measurement, String) -> Measurement,
)

fun measureFields(s: Settings): List<MeasureField> = buildList {
    val len: (Settings) -> String = { it.lengthUnit }
    add(MeasureField("Neck", len, { it.neck }, { m, v -> m.copy(neck = v) }))
    add(MeasureField("Chest", len, { it.chest }, { m, v -> m.copy(chest = v) }))
    add(MeasureField("Biceps", len, { it.biceps }, { m, v -> m.copy(biceps = v) }))
    add(MeasureField("Waist", len, { it.waist }, { m, v -> m.copy(waist = v) }))
    add(MeasureField("Hips", len, { it.hips }, { m, v -> m.copy(hips = v) }))
    add(MeasureField("Thigh", len, { it.thigh }, { m, v -> m.copy(thigh = v) }))
    add(MeasureField("Calf", len, { it.calf }, { m, v -> m.copy(calf = v) }))
    add(MeasureField("Weight", { it.weightUnit }, { it.weight }, { m, v -> m.copy(weight = v) }))
    add(MeasureField("Body fat", { "%" }, { it.bodyFat }, { m, v -> m.copy(bodyFat = v) }))
    if (s.customMeasure1.isNotBlank()) add(MeasureField(s.customMeasure1, { "" }, { it.custom1 }, { m, v -> m.copy(custom1 = v) }))
    if (s.customMeasure2.isNotBlank()) add(MeasureField(s.customMeasure2, { "" }, { it.custom2 }, { m, v -> m.copy(custom2 = v) }))
}

@Composable
fun BodyScreen(vm: AppViewModel, data: AppData) {
    val s = data.settings
    val fields = measureFields(s)
    var expanded by remember { mutableStateOf<Long?>(null) }
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    val sorted = data.measurements.sortedByDescending { it.date }
    fun updM(id: Long, t: (Measurement) -> Measurement) =
        vm.update { d -> d.copy(measurements = d.measurements.map { if (it.id == id) t(it) else it }) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp)) {
        SectionCard("Before & After") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoBox("Before", data.photos.beforePath, data.photos.beforeDate, Modifier.weight(1f),
                    onPicked = { vm.savePhoto(it, true, LocalDate.now().toString()) },
                    onRemove = { vm.removePhoto(true) },
                    onDate = { v -> vm.updatePhotos { it.copy(beforeDate = v) } })
                PhotoBox("After", data.photos.afterPath, data.photos.afterDate, Modifier.weight(1f),
                    onPicked = { vm.savePhoto(it, false, LocalDate.now().toString()) },
                    onRemove = { vm.removePhoto(false) },
                    onDate = { v -> vm.updatePhotos { it.copy(afterDate = v) } })
            }
        }

        if (sorted.isNotEmpty()) {
            SectionCard("Measurement table") {
                // Spreadsheet view like the paper: metrics down the side, dates across.
                val cols = sorted.reversed()
                Row {
                    Column {
                        TableCell("Date", header = true, width = 76)
                        fields.forEach { TableCell(it.label, header = true, width = 76) }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        cols.forEach { m ->
                            Column {
                                TableCell(m.date.prettyDate(), header = true)
                                fields.forEach { f -> TableCell(f.get(m)) }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val m = Measurement(date = LocalDate.now().toString())
                vm.update { it.copy(measurements = it.measurements + m) }
                expanded = m.id
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        ) { Icon(Icons.Default.Add, null); Text("Add measurements") }

        sorted.forEach { m ->
            val open = expanded == m.id
            SectionCard(
                "Measured " + m.date.prettyDate(),
                trailing = {
                    Row {
                        IconButton(onClick = { confirmDelete = m.id }) { Icon(Icons.Default.Delete, "Delete") }
                        IconButton(onClick = { expanded = if (open) null else m.id }) {
                            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expand")
                        }
                    }
                },
            ) {
                if (open) {
                    DateButton("Date", m.date, { v -> if (v.isNotBlank()) updM(m.id) { it.copy(date = v) } }, Modifier.fillMaxWidth())
                    fields.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { f ->
                                Field(f.label, f.get(m), { v -> updM(m.id) { f.set(it, v) } }, Modifier.weight(1f),
                                    numeric = true, suffix = f.unit(s).ifBlank { null })
                            }
                            if (pair.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                } else {
                    val summary = fields.filter { it.get(m).isNotBlank() }.joinToString("  •  ") { "${it.label} ${it.get(m)}" }
                    Text(summary.ifBlank { "Tap ▾ to enter measurements" }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete these measurements?") },
            confirmButton = {
                TextButton(onClick = { vm.update { d -> d.copy(measurements = d.measurements.filterNot { it.id == id }) }; confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TableCell(text: String, header: Boolean = false, width: Int = 64) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.width(width.dp).padding(1.dp)
            .background(if (header) cs.surfaceVariant else cs.surface)
            .border(0.5.dp, cs.outlineVariant)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.ifBlank { " " }, style = MaterialTheme.typography.bodySmall,
            fontWeight = if (header) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun PhotoBox(
    label: String, path: String, date: String, modifier: Modifier,
    onPicked: (android.net.Uri) -> Unit, onRemove: () -> Unit, onDate: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    val bmp = remember(path) { loadPhoto(path, 800)?.asImageBitmap() }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SmallLabel(label.uppercase())
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.8f)
                .clip(MaterialTheme.shapes.medium)
                .background(cs.surfaceVariant)
                .border(1.dp, cs.outline, MaterialTheme.shapes.medium)
                .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            contentAlignment = Alignment.Center,
        ) {
            if (bmp != null) Image(bmp, label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, null, Modifier.size(40.dp), tint = cs.onSurfaceVariant)
                SmallLabel("Tap to add photo")
            }
        }
        DateButton("", date, onDate, Modifier.fillMaxWidth().padding(top = 4.dp))
        if (bmp != null) TextButton(onClick = onRemove) { Text("Remove") }
    }
}
