package com.workoutlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workoutlog.app.data.AppViewModel
import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.Tracker
import com.workoutlog.app.ui.components.CellField
import com.workoutlog.app.ui.components.DateButton
import com.workoutlog.app.ui.components.Field
import com.workoutlog.app.ui.components.SectionCard
import com.workoutlog.app.ui.components.SmallLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val MonthDay = DateTimeFormatter.ofPattern("M/d")

/** Which week number "today" falls into, given the tracker start date. */
fun currentWeek(start: String): Int? = try {
    (ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.now()) / 7 + 1).toInt()
} catch (_: Exception) { null }

@Composable
fun TrackerScreen(vm: AppViewModel, data: AppData) {
    val start = data.settings.trackerStartDate
    val nowWeek = currentWeek(start)
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    fun updT(id: Long, t: (Tracker) -> Tracker) = vm.update { d -> d.copy(trackers = d.trackers.map { if (it.id == id) t(it) else it }) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp)) {
        SectionCard("Progress tracker") {
            Text(
                "Track anything week by week for 33 weeks — e.g. bench max, 1.5 mile time, pull-ups, body weight.",
                style = MaterialTheme.typography.bodySmall,
            )
            DateButton("Week 1 starts", start, { v -> vm.updateSettings { it.copy(trackerStartDate = v) } },
                Modifier.fillMaxWidth().padding(top = 8.dp))
            if (nowWeek != null && nowWeek in 1..Tracker.TRACKER_WEEKS)
                Text("This is week $nowWeek", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
        }

        data.trackers.forEach { t ->
            SectionCard(
                t.name.ifBlank { "Item to track" },
                trailing = { IconButton(onClick = { confirmDelete = t.id }) { Icon(Icons.Default.Delete, "Delete tracker") } },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Item to track", t.name, { v -> updT(t.id) { it.copy(name = v) } }, Modifier.weight(2f))
                    Field("Unit", t.unit, { v -> updT(t.id) { it.copy(unit = v) } }, Modifier.weight(1f))
                }
                val perRow = 6
                (0 until Tracker.TRACKER_WEEKS).chunked(perRow).forEach { row ->
                    Row(Modifier.padding(top = 4.dp)) {
                        row.forEach { w ->
                            Column(Modifier.weight(1f).padding(1.dp)) {
                                val dateLbl = try { " " + LocalDate.parse(start).plusWeeks(w.toLong()).format(MonthDay) } catch (_: Exception) { "" }
                                SmallLabel("W${w + 1}$dateLbl", Modifier.fillMaxWidth())
                                CellField(
                                    t.weeks.getOrElse(w) { "" },
                                    { v -> updT(t.id) { it.copy(weeks = it.weeks.padded().with(w, v)) } },
                                    Modifier.fillMaxWidth(),
                                    numeric = false,
                                    shaded = nowWeek == w + 1,
                                )
                            }
                        }
                        repeat(perRow - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (start.isBlank()) vm.updateSettings { it.copy(trackerStartDate = LocalDate.now().toString()) }
                vm.update { it.copy(trackers = it.trackers + Tracker()) }
            },
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) { Icon(Icons.Default.Add, null); Text("Add item to track") }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this tracker?") },
            confirmButton = {
                TextButton(onClick = { vm.update { d -> d.copy(trackers = d.trackers.filterNot { it.id == id }) }; confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

private fun List<String>.padded(): List<String> =
    if (size >= Tracker.TRACKER_WEEKS) this else this + List(Tracker.TRACKER_WEEKS - size) { "" }
