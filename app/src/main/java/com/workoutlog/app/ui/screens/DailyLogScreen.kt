package com.workoutlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workoutlog.app.data.AppViewModel
import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.CardioEntry
import com.workoutlog.app.model.DayLog
import com.workoutlog.app.model.ExerciseEntry
import com.workoutlog.app.model.FoodItem
import com.workoutlog.app.model.Macros
import com.workoutlog.app.model.Meal
import com.workoutlog.app.model.fmt
import com.workoutlog.app.model.macros
import com.workoutlog.app.model.num
import com.workoutlog.app.ui.components.BoxScale
import com.workoutlog.app.ui.components.CellField
import com.workoutlog.app.ui.components.CheckItem
import com.workoutlog.app.ui.components.DateButton
import com.workoutlog.app.ui.components.Field
import com.workoutlog.app.ui.components.LongDate
import com.workoutlog.app.ui.components.MoodScale
import com.workoutlog.app.ui.components.SectionCard
import com.workoutlog.app.ui.components.SmallLabel
import com.workoutlog.app.ui.components.WeekdayBoxes
import java.time.LocalDate

/** Replace element [i] of a list. */
fun <T> List<T>.with(i: Int, v: T): List<T> = toMutableList().also { it[i] = v }
fun <T> List<T>.without(i: Int): List<T> = toMutableList().also { it.removeAt(i) }

@Composable
fun DailyLogScreen(vm: AppViewModel, data: AppData, date: LocalDate, initialPage: Int = 0, onDate: (LocalDate) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(initialPage) }
    val key = date.toString()
    val log = data.logs[key] ?: DayLog(key)
    val upd: ((DayLog) -> DayLog) -> Unit = { vm.updateDay(key, it) }

    Column(Modifier.fillMaxSize()) {
        // Date bar with previous / next day arrows and a calendar picker.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onDate(date.minusDays(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.format(LongDate), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                WeekdayBoxes(date)
            }
            IconButton(onClick = { onDate(date.plusDays(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateButton("Jump to date", key, { if (it.isNotBlank()) onDate(LocalDate.parse(it)) }, Modifier.weight(1f))
            if (date != LocalDate.now()) TextButton(onClick = { onDate(LocalDate.now()) }) { Text("Today") }
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Workout") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Diet") })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(bottom = 24.dp)) {
            if (tab == 0) WorkoutPage(data, log, upd) else DietPage(data, log, upd)
        }
    }
}

// =====================================================================
//                              WORKOUT PAGE
// =====================================================================
@Composable
private fun WorkoutPage(data: AppData, log: DayLog, upd: ((DayLog) -> DayLog) -> Unit) {
    val s = data.settings

    SectionCard("Session") {
        Field("Workout name", log.workoutName, { v -> upd { it.copy(workoutName = v) } }, Modifier.fillMaxWidth())
        Row {
            CheckItem("Warm up", log.warmUp, { v -> upd { it.copy(warmUp = v) } })
            CheckItem("Cool down", log.coolDown, { v -> upd { it.copy(coolDown = v) } })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Water", log.water, { v -> upd { it.copy(water = v) } }, Modifier.weight(1f), numeric = true, suffix = s.waterUnit)
            Field("Sleep", log.sleep, { v -> upd { it.copy(sleep = v) } }, Modifier.weight(1f), numeric = true, suffix = "hrs")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Weight", log.weight, { v -> upd { it.copy(weight = v) } }, Modifier.weight(1f), numeric = true, suffix = s.weightUnit)
            Field("Duration", log.duration, { v -> upd { it.copy(duration = v) } }, Modifier.weight(1f), suffix = "min")
        }
    }

    SectionCard(
        "Exercises",
        trailing = {
            TextButton(onClick = {
                val prev = data.logs.values
                    .filter { it.date < log.date && it.exercises.any { e -> e.name.isNotBlank() } }
                    .maxByOrNull { it.date }
                if (prev != null) upd { it.copy(exercises = prev.exercises, workoutName = it.workoutName.ifBlank { prev.workoutName }) }
            }) {
                Icon(Icons.Default.ContentCopy, null, Modifier.padding(end = 4.dp))
                Text("Copy last")
            }
        },
    ) {
        log.exercises.forEachIndexed { i, ex ->
            ExerciseBlock(
                ex, s.weightUnit,
                onChange = { e -> upd { it.copy(exercises = it.exercises.with(i, e)) } },
                onDelete = { upd { it.copy(exercises = it.exercises.without(i)) } },
            )
            if (i < log.exercises.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }
        TextButton(onClick = { upd { it.copy(exercises = it.exercises + ExerciseEntry()) } }) {
            Icon(Icons.Default.Add, null); Text("Add exercise")
        }
    }

    SectionCard("Cardio") {
        log.cardio.forEachIndexed { i, c ->
            CardioRow(c, s.distanceUnit, { nc -> upd { it.copy(cardio = it.cardio.with(i, nc)) } }) {
                upd { it.copy(cardio = it.cardio.without(i)) }
            }
        }
        TextButton(onClick = { upd { it.copy(cardio = it.cardio + CardioEntry()) } }) {
            Icon(Icons.Default.Add, null); Text("Add cardio")
        }
    }

    SectionCard("Notes") {
        Field("Notes", log.notes, { v -> upd { it.copy(notes = v) } }, Modifier.fillMaxWidth(), singleLine = false, minLines = 3)
    }

    SectionCard("Rate your workout") {
        val opts = listOf(20, 40, 60, 80, 100)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            opts.forEachIndexed { i, v ->
                SegmentedButton(
                    selected = log.rating == v,
                    onClick = { upd { it.copy(rating = if (it.rating == v) 0 else v) } },
                    shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                    icon = {},
                ) { Text("$v%", maxLines = 1) }
            }
        }
    }
}

@Composable
private fun ExerciseBlock(ex: ExerciseEntry, unit: String, onChange: (ExerciseEntry) -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Field("Exercise", ex.name, { onChange(ex.copy(name = it)) }, Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Remove exercise") }
    }
    Spacer(Modifier.height(6.dp))
    // Header row: Set 1..6
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(44.dp))
        for (i in 1..6) SmallLabel("Set $i", Modifier.weight(1f))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SmallLabel(unit, Modifier.width(44.dp), TextAlign.Start)
        ex.sets.forEachIndexed { i, set ->
            CellField(set.weight, { onChange(ex.copy(sets = ex.sets.with(i, set.copy(weight = it)))) }, Modifier.weight(1f).padding(1.dp), shaded = true)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SmallLabel("Reps", Modifier.width(44.dp), TextAlign.Start)
        ex.sets.forEachIndexed { i, set ->
            CellField(set.reps, { onChange(ex.copy(sets = ex.sets.with(i, set.copy(reps = it)))) }, Modifier.weight(1f).padding(1.dp))
        }
    }
}

@Composable
private fun CardioRow(c: CardioEntry, distUnit: String, onChange: (CardioEntry) -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Field("Cardio type", c.type, { onChange(c.copy(type = it)) }, Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Remove cardio") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 10.dp)) {
        Field("Time", c.time, { onChange(c.copy(time = it)) }, Modifier.weight(1f))
        Field("Distance", c.distance, { onChange(c.copy(distance = it)) }, Modifier.weight(1f), numeric = true, suffix = distUnit)
        Field("Calories", c.calories, { onChange(c.copy(calories = it)) }, Modifier.weight(1f), numeric = true)
    }
}

// =====================================================================
//                               DIET PAGE
// =====================================================================
@Composable
private fun DietPage(data: AppData, log: DayLog, upd: ((DayLog) -> DayLog) -> Unit) {
    val s = data.settings

    SectionCard("How you feel") {
        MoodScale(log.mood) { v -> upd { it.copy(mood = v) } }
        BoxScale("Energy", log.energy) { v -> upd { it.copy(energy = v) } }
        BoxScale("Activity", log.activity) { v -> upd { it.copy(activity = v) } }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Body fat", log.bodyFat, { v -> upd { it.copy(bodyFat = v) } }, Modifier.weight(1f), numeric = true, suffix = "%")
            Field("Caffeine drinks", log.caffeine, { v -> upd { it.copy(caffeine = v) } }, Modifier.weight(1f), numeric = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Fruit", log.fruit, { v -> upd { it.copy(fruit = v) } }, Modifier.weight(1f), numeric = true, suffix = "srv")
            Field("Veggies", log.veggies, { v -> upd { it.copy(veggies = v) } }, Modifier.weight(1f), numeric = true, suffix = "srv")
        }
        Row {
            CheckItem("Sugar-free", log.sugarFree, { v -> upd { it.copy(sugarFree = v) } }, Modifier.weight(1f))
            CheckItem("Gluten-free", log.glutenFree, { v -> upd { it.copy(glutenFree = v) } }, Modifier.weight(1f))
        }
        Row {
            CheckItem("Lactose-free", log.lactoseFree, { v -> upd { it.copy(lactoseFree = v) } }, Modifier.weight(1f))
            CheckItem("Dairy-free", log.dairyFree, { v -> upd { it.copy(dairyFree = v) } }, Modifier.weight(1f))
        }
    }

    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = {
            val prev = data.logs.values
                .filter { it.date < log.date && it.meals.any { m -> m.items.any { f -> !f.isEmpty() } } }
                .maxByOrNull { it.date }
            if (prev != null) upd { it.copy(meals = prev.meals) }
        }) {
            Icon(Icons.Default.ContentCopy, null, Modifier.padding(end = 4.dp)); Text("Copy previous day's meals")
        }
    }

    log.meals.forEachIndexed { mi, meal ->
        MealCard(meal, s.extraNutrientLabel) { nm -> upd { it.copy(meals = it.meals.with(mi, nm)) } }
    }

    SectionCard("Daily totals") {
        val total = log.macros()
        val goal = Macros(
            s.calorieGoal.num() ?: Double.NaN, s.proteinGoal.num() ?: Double.NaN,
            s.fatGoal.num() ?: Double.NaN, s.carbsGoal.num() ?: Double.NaN, s.extraGoal.num() ?: Double.NaN,
        )
        MacroHeader(s.extraNutrientLabel)
        MacroRow("Total", total)
        MacroRow("Goal", goal)
        MacroRow(
            "Vs. goal",
            Macros(total.calories - goal.calories, total.protein - goal.protein, total.fat - goal.fat, total.carbs - goal.carbs, total.extra - goal.extra),
            signed = true,
        )
        if (goal.calories.isNaN()) SmallLabel("Set daily goals in Settings to see Vs. Goal.", Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

@Composable
private fun MealCard(meal: Meal, extraLabel: String, onChange: (Meal) -> Unit) {
    SectionCard(
        meal.name,
        trailing = {
            CellField(meal.time, { onChange(meal.copy(time = it)) }, Modifier.width(80.dp), numeric = false, placeholder = "time")
        },
    ) {
        meal.items.forEachIndexed { i, f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CellField(f.product, { onChange(meal.copy(items = meal.items.with(i, f.copy(product = it)))) },
                    Modifier.weight(1f), numeric = false, placeholder = "Product / food", align = TextAlign.Start)
                IconButton(onClick = { onChange(meal.copy(items = meal.items.without(i).ifEmpty { listOf(FoodItem()) })) }) {
                    Icon(Icons.Default.Delete, "Remove food")
                }
            }
            Row(Modifier.padding(bottom = 8.dp)) {
                val cells = listOf(
                    "Serv." to f.serving, "Cal" to f.calories, "Prot" to f.protein,
                    "Fat" to f.fat, "Carbs" to f.carbs, extraLabel to f.extra,
                )
                cells.forEachIndexed { ci, (lbl, v) ->
                    Column(Modifier.weight(1f).padding(1.dp)) {
                        SmallLabel(lbl, Modifier.fillMaxWidth())
                        CellField(v, { nv ->
                            val nf = when (ci) {
                                0 -> f.copy(serving = nv); 1 -> f.copy(calories = nv); 2 -> f.copy(protein = nv)
                                3 -> f.copy(fat = nv); 4 -> f.copy(carbs = nv); else -> f.copy(extra = nv)
                            }
                            onChange(meal.copy(items = meal.items.with(i, nf)))
                        }, Modifier.fillMaxWidth(), numeric = ci != 0, shaded = ci % 2 == 1)
                    }
                }
            }
        }
        TextButton(onClick = { onChange(meal.copy(items = meal.items + FoodItem())) }) {
            Icon(Icons.Default.Add, null); Text("Add food")
        }
        HorizontalDivider()
        val sub = meal.macros()
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("Subtotal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.4f))
            listOf(sub.calories, sub.protein, sub.fat, sub.carbs, sub.extra).forEach {
                Text(it.fmt(), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1.4f))
            listOf("cal", "prot", "fat", "carbs", extraLabel.lowercase()).forEach { SmallLabel(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun MacroHeader(extraLabel: String) {
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1.4f))
        listOf("Cal", "Prot", "Fat", "Carbs", extraLabel).forEach { SmallLabel(it, Modifier.weight(1f)) }
    }
}

@Composable
private fun MacroRow(label: String, m: Macros, signed: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.4f))
        listOf(m.calories, m.protein, m.fat, m.carbs, m.extra).forEach { v ->
            val txt = when {
                v.isNaN() -> "–"
                signed && v > 0 -> "+" + v.fmt()
                else -> v.fmt()
            }
            Text(
                txt, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold,
                color = if (signed && !v.isNaN()) (if (v > 0) cs.secondary else cs.primary) else cs.onSurface,
            )
        }
    }
}


@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(o) }) }
    }
}
