package com.workoutlog.app.model

import kotlinx.serialization.Serializable

/*
 * Data model. Every box from the paper log has a field here.
 * Numbers are stored as text so you can type things like "13:30" or "1.5"
 * freely; they are parsed into numbers only for totals, charts and the PDF.
 */

@Serializable
data class AppData(
    val settings: Settings = Settings(),
    val logs: Map<String, DayLog> = emptyMap(),          // key = ISO date, e.g. 2026-11-24
    val goals: List<Goal> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val photos: Photos = Photos(),
    val trackers: List<Tracker> = emptyList(),
)

@Serializable
data class Settings(
    val theme: String = "",                 // "MENS" / "WOMENS"; empty = this APK's default
    val darkMode: String = "SYSTEM",        // SYSTEM / LIGHT / DARK
    val weightUnit: String = "lbs",
    val lengthUnit: String = "in",
    val distanceUnit: String = "mi",
    val waterUnit: String = "oz",
    val calorieGoal: String = "",
    val proteinGoal: String = "",
    val fatGoal: String = "",
    val carbsGoal: String = "",
    val extraGoal: String = "",
    val extraNutrientLabel: String = "Sugar", // the unlabeled 7th diet column
    val customMeasure1: String = "",          // the two blank rows in the measurement table
    val customMeasure2: String = "",
    val trackerStartDate: String = "",        // date of "Week 1" in the progress tracker
)

@Serializable
data class DayLog(
    val date: String,
    // ---- Workout page ----
    val workoutName: String = "",
    val warmUp: Boolean = false,
    val coolDown: Boolean = false,
    val water: String = "",
    val sleep: String = "",
    val weight: String = "",
    val duration: String = "",
    val exercises: List<ExerciseEntry> = listOf(ExerciseEntry()),
    val cardio: List<CardioEntry> = listOf(CardioEntry(), CardioEntry()),
    val notes: String = "",
    val rating: Int = 0,                      // 0 = unrated, else 20/40/60/80/100
    // ---- Diet page ----
    val mood: Int = 0,                        // 1..5
    val energy: Int = 0,                      // 1..5
    val activity: Int = 0,                    // 1..5
    val bodyFat: String = "",
    val caffeine: String = "",
    val fruit: String = "",
    val veggies: String = "",
    val sugarFree: Boolean = false,
    val glutenFree: Boolean = false,
    val lactoseFree: Boolean = false,
    val dairyFree: Boolean = false,
    val meals: List<Meal> = Meal.defaults(),
) {
    fun hasWorkout(): Boolean =
        workoutName.isNotBlank() || exercises.any { !it.isEmpty() } || cardio.any { !it.isEmpty() } ||
            notes.isNotBlank() || rating > 0 || water.isNotBlank() || sleep.isNotBlank() ||
            weight.isNotBlank() || duration.isNotBlank() || warmUp || coolDown

    fun hasDiet(): Boolean =
        meals.any { m -> m.items.any { !it.isEmpty() } } || mood > 0 || energy > 0 || activity > 0 ||
            bodyFat.isNotBlank() || caffeine.isNotBlank() || fruit.isNotBlank() || veggies.isNotBlank() ||
            sugarFree || glutenFree || lactoseFree || dairyFree

    fun isEmpty() = !hasWorkout() && !hasDiet()
}

@Serializable
data class ExerciseEntry(
    val name: String = "",
    val sets: List<SetEntry> = List(6) { SetEntry() },
) {
    fun isEmpty() = name.isBlank() && sets.all { it.weight.isBlank() && it.reps.isBlank() }
}

@Serializable
data class SetEntry(val weight: String = "", val reps: String = "")

@Serializable
data class CardioEntry(
    val type: String = "",
    val time: String = "",
    val distance: String = "",
    val calories: String = "",
) {
    fun isEmpty() = type.isBlank() && time.isBlank() && distance.isBlank() && calories.isBlank()
}

@Serializable
data class Meal(
    val name: String,
    val time: String = "",
    val items: List<FoodItem> = listOf(FoodItem()),
) {
    companion object {
        fun defaults() = listOf(Meal("Breakfast"), Meal("Lunch"), Meal("Dinner"), Meal("Snacks"))
    }
}

@Serializable
data class FoodItem(
    val product: String = "",
    val serving: String = "",
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val extra: String = "",
) {
    fun isEmpty() = listOf(product, serving, calories, protein, fat, carbs, extra).all { it.isBlank() }
}

@Serializable
data class Goal(
    val id: Long = System.currentTimeMillis(),
    val goal: String = "",
    val start: String = "",
    val deadline: String = "",
    val plan: String = "",
    val achievement: String = "",
    val achieved: Boolean = false,
)

@Serializable
data class Measurement(
    val id: Long = System.currentTimeMillis(),
    val date: String = "",
    val neck: String = "",
    val chest: String = "",
    val biceps: String = "",
    val waist: String = "",
    val hips: String = "",
    val thigh: String = "",
    val calf: String = "",
    val weight: String = "",
    val bodyFat: String = "",
    val custom1: String = "",
    val custom2: String = "",
)

@Serializable
data class Photos(
    val beforePath: String = "",
    val beforeDate: String = "",
    val afterPath: String = "",
    val afterDate: String = "",
)

@Serializable
data class Tracker(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val unit: String = "",
    val weeks: List<String> = List(TRACKER_WEEKS) { "" },
) {
    companion object { const val TRACKER_WEEKS = 33 }
}

/** Nutrition totals for one meal or a whole day. */
data class Macros(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val extra: Double = 0.0,
) {
    operator fun plus(o: Macros) =
        Macros(calories + o.calories, protein + o.protein, fat + o.fat, carbs + o.carbs, extra + o.extra)
}

fun String.num(): Double? = trim().replace(",", "").toDoubleOrNull()

/** Parses "13:30" (min:sec), "1:05:00" (h:min:sec) or "45" (minutes) into minutes. */
fun String.minutes(): Double? {
    val t = trim()
    if (t.isEmpty()) return null
    val parts = t.split(":")
    return try {
        when (parts.size) {
            1 -> t.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            2 -> parts[0].toDouble() + parts[1].toDouble() / 60.0
            3 -> parts[0].toDouble() * 60 + parts[1].toDouble() + parts[2].toDouble() / 60.0
            else -> null
        }
    } catch (_: NumberFormatException) { null }
}

fun FoodItem.macros() = Macros(
    calories.num() ?: 0.0, protein.num() ?: 0.0, fat.num() ?: 0.0, carbs.num() ?: 0.0, extra.num() ?: 0.0,
)

fun Meal.macros(): Macros = items.fold(Macros()) { acc, f -> acc + f.macros() }
fun DayLog.macros(): Macros = meals.fold(Macros()) { acc, m -> acc + m.macros() }

fun Double.fmt(): String =
    if (this == Math.floor(this) && !isInfinite() && Math.abs(this) < 1e12) toLong().toString()
    else String.format(java.util.Locale.US, "%.1f", this)

/** Heaviest weight lifted in any set of this exercise. */
fun ExerciseEntry.topWeight(): Double? = sets.mapNotNull { it.weight.num() }.maxOrNull()

/** Total volume = sum of weight x reps across sets. */
fun ExerciseEntry.volume(): Double =
    sets.sumOf { (it.weight.num() ?: 0.0) * (it.reps.num() ?: 0.0) }

/** Estimated one-rep max (Epley formula) from the best set. */
fun ExerciseEntry.estimated1RM(): Double? = sets.mapNotNull { s ->
    val w = s.weight.num(); val r = s.reps.num()
    if (w == null || r == null || r <= 0) null else w * (1 + r / 30.0)
}.maxOrNull()
