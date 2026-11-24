package com.workoutlog.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.workoutlog.app.data.AppViewModel
import com.workoutlog.app.model.AppData
import com.workoutlog.app.ui.components.Field
import com.workoutlog.app.ui.components.SectionCard
import com.workoutlog.app.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData, theme: AppTheme) {
    val s = data.settings
    val ctx = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
            Toast.makeText(ctx, "Backup saved", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: ""
            val ok = vm.importJson(text)
            Toast.makeText(ctx, if (ok) "Backup restored" else "That file isn't a Workout Log backup", Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp)) {
        SectionCard("Theme") {
            ChipRow(listOf("Men's", "Women's"), if (theme == AppTheme.MENS) "Men's" else "Women's") { v ->
                vm.updateSettings { it.copy(theme = if (v == "Men's") "MENS" else "WOMENS") }
            }
            Row(Modifier.padding(top = 6.dp)) {
                ChipRow(listOf("System", "Light", "Dark"), s.darkMode.lowercase().replaceFirstChar { it.uppercase() }) { v ->
                    vm.updateSettings { it.copy(darkMode = v.uppercase()) }
                }
            }
        }

        SectionCard("Units") {
            Text("Weight", style = MaterialTheme.typography.labelLarge)
            ChipRow(listOf("lbs", "kg"), s.weightUnit) { v -> vm.updateSettings { it.copy(weightUnit = v) } }
            Text("Body measurements", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
            ChipRow(listOf("in", "cm"), s.lengthUnit) { v -> vm.updateSettings { it.copy(lengthUnit = v) } }
            Text("Distance", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
            ChipRow(listOf("mi", "km", "m"), s.distanceUnit) { v -> vm.updateSettings { it.copy(distanceUnit = v) } }
            Text("Water", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
            ChipRow(listOf("oz", "cups", "L", "ml"), s.waterUnit) { v -> vm.updateSettings { it.copy(waterUnit = v) } }
        }

        SectionCard("Daily nutrition goals") {
            Text("Used for the “Vs. Goal” row and goal lines on graphs.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Calories", s.calorieGoal, { v -> vm.updateSettings { it.copy(calorieGoal = v) } }, Modifier.weight(1f), numeric = true)
                Field("Protein (g)", s.proteinGoal, { v -> vm.updateSettings { it.copy(proteinGoal = v) } }, Modifier.weight(1f), numeric = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Fat (g)", s.fatGoal, { v -> vm.updateSettings { it.copy(fatGoal = v) } }, Modifier.weight(1f), numeric = true)
                Field("Carbs (g)", s.carbsGoal, { v -> vm.updateSettings { it.copy(carbsGoal = v) } }, Modifier.weight(1f), numeric = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Extra column name", s.extraNutrientLabel, { v -> vm.updateSettings { it.copy(extraNutrientLabel = v) } }, Modifier.weight(1f))
                Field("${s.extraNutrientLabel.ifBlank { "Extra" }} goal", s.extraGoal, { v -> vm.updateSettings { it.copy(extraGoal = v) } }, Modifier.weight(1f), numeric = true)
            }
        }

        SectionCard("Custom measurements") {
            Text("The paper table has two blank rows — name them here (e.g. Forearm, Shoulders) and they appear on the Body tab.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Custom 1", s.customMeasure1, { v -> vm.updateSettings { it.copy(customMeasure1 = v) } }, Modifier.weight(1f))
                Field("Custom 2", s.customMeasure2, { v -> vm.updateSettings { it.copy(customMeasure2 = v) } }, Modifier.weight(1f))
            }
        }

        SectionCard("Backup & restore") {
            Text(
                "Your log is stored only on this phone. Export a backup file now and then (and before uninstalling or switching phones). " +
                    "Photos are not included in the backup.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedButton(onClick = { exportLauncher.launch("workout-log-backup-${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) { Text("Export backup") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) { Text("Restore backup") }
            }
        }
    }
}
