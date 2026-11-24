package com.workoutlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.workoutlog.app.data.AppViewModel
import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.Goal
import com.workoutlog.app.ui.components.CheckItem
import com.workoutlog.app.ui.components.DateButton
import com.workoutlog.app.ui.components.Field
import com.workoutlog.app.ui.components.SectionCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun GoalsScreen(vm: AppViewModel, data: AppData) {
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    fun updGoal(id: Long, t: (Goal) -> Goal) = vm.update { d -> d.copy(goals = d.goals.map { if (it.id == id) t(it) else it }) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp)) {
        if (data.goals.isEmpty()) {
            Text(
                "No goals yet. Tap “Add goal” to set one — e.g. Bench 150 lbs by a deadline.",
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
            )
        }
        data.goals.forEachIndexed { idx, g ->
            SectionCard(
                "Goal ${idx + 1}" + if (g.achieved) "  ✓" else "",
                trailing = { IconButton(onClick = { confirmDelete = g.id }) { Icon(Icons.Default.Delete, "Delete goal") } },
            ) {
                Field("Your goal", g.goal, { v -> updGoal(g.id) { it.copy(goal = v) } }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    DateButton("Start", g.start, { v -> updGoal(g.id) { it.copy(start = v) } }, Modifier.weight(1f))
                    DateButton("Deadline", g.deadline, { v -> updGoal(g.id) { it.copy(deadline = v) } }, Modifier.weight(1f))
                }
                daysLeft(g)?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary) }
                Field("Your action plan", g.plan, { v -> updGoal(g.id) { it.copy(plan = v) } }, Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
                Field("Achievement", g.achievement, { v -> updGoal(g.id) { it.copy(achievement = v) } }, Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CheckItem("Goal achieved", g.achieved, { v -> updGoal(g.id) { it.copy(achieved = v) } })
                }
            }
        }
        Button(
            onClick = { vm.update { it.copy(goals = it.goals + Goal(start = LocalDate.now().toString())) } },
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        ) { Icon(Icons.Default.Add, null); Text("Add goal") }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this goal?") },
            confirmButton = {
                TextButton(onClick = { vm.update { d -> d.copy(goals = d.goals.filterNot { it.id == id }) }; confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

fun daysLeft(g: Goal): String? {
    if (g.achieved || g.deadline.isBlank()) return null
    return try {
        val d = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(g.deadline))
        when {
            d > 1 -> "$d days left"
            d == 1L -> "1 day left"
            d == 0L -> "Due today"
            else -> "${-d} days past deadline"
        }
    } catch (_: Exception) { null }
}
