package com.workoutlog.app.charts

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.DayLog
import com.workoutlog.app.model.estimated1RM
import com.workoutlog.app.model.fmt
import com.workoutlog.app.model.macros
import com.workoutlog.app.model.minutes
import com.workoutlog.app.model.num
import com.workoutlog.app.model.topWeight
import com.workoutlog.app.model.volume
import com.workoutlog.app.ui.screens.measureFields
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** One point: x is a day number (LocalDate.toEpochDay) or a week number. */
data class Pt(val x: Double, val y: Double)

data class Series(
    val title: String,
    val unit: String,
    val points: List<Pt>,
    val goal: Double? = null,
    val xIsWeek: Boolean = false,
    val lowerIsBetter: Boolean = false,
) {
    val first get() = points.firstOrNull()?.y
    val last get() = points.lastOrNull()?.y
    val best get() = if (lowerIsBetter) points.minOfOrNull { it.y } else points.maxOfOrNull { it.y }

    fun summary(): String {
        if (points.isEmpty()) return "No data in this range."
        val u = if (unit.isBlank()) "" else " $unit"
        val change = last!! - first!!
        val sign = if (change > 0) "+" else ""
        return "Start ${first!!.fmt()}$u → Latest ${last!!.fmt()}$u  (change $sign${change.fmt()}$u)  •  " +
            "Best ${best!!.fmt()}$u  •  ${points.size} entries"
    }
}

/** Every kind of graph the app can make. Used by both the Charts tab and the PDF. */
sealed class ChartKind(val category: String, val label: String) {
    data object BodyWeight : ChartKind("Body", "Body weight")
    data class Measure(val field: String) : ChartKind("Body", field)
    data class ExerciseTop(val name: String) : ChartKind("Strength", "$name – heaviest set")
    data class Exercise1RM(val name: String) : ChartKind("Strength", "$name – est. 1-rep max")
    data class ExerciseVolume(val name: String) : ChartKind("Strength", "$name – volume")
    data class Nutrition(val field: String) : ChartKind("Nutrition", field)
    data class Wellness(val field: String) : ChartKind("Wellness", field)
    data class Cardio(val type: String, val field: String) : ChartKind("Cardio", "$type – $field")
    data class TrackerItem(val id: Long, val name: String) : ChartKind("Tracker", name)
}

object ChartData {
    val nutritionFields = listOf("Calories", "Protein", "Fat", "Carbs")
    val wellnessFields = listOf("Sleep", "Water", "Mood", "Energy", "Activity", "Workout rating", "Caffeine drinks", "Fruit", "Veggies", "Workout duration")
    val cardioFields = listOf("Distance", "Time", "Calories")

    fun exerciseNames(d: AppData): List<String> =
        d.logs.values.flatMap { l -> l.exercises.map { it.name.trim() } }
            .filter { it.isNotBlank() }
            .groupBy { it.lowercase() }
            .map { (_, v) -> v.first() }
            .sortedBy { it.lowercase() }

    fun cardioTypes(d: AppData): List<String> =
        d.logs.values.flatMap { l -> l.cardio.map { it.type.trim() } }
            .filter { it.isNotBlank() }
            .groupBy { it.lowercase() }.map { (_, v) -> v.first() }.sortedBy { it.lowercase() }

    /** All chart kinds that currently have something to show, grouped for the chooser. */
    fun allKinds(d: AppData): List<ChartKind> = buildList {
        add(ChartKind.BodyWeight)
        measureFields(d.settings).filter { it.label != "Weight" }.forEach { add(ChartKind.Measure(it.label)) }
        exerciseNames(d).forEach {
            add(ChartKind.ExerciseTop(it)); add(ChartKind.Exercise1RM(it)); add(ChartKind.ExerciseVolume(it))
        }
        val extra = d.settings.extraNutrientLabel.ifBlank { "Extra" }
        (nutritionFields + extra).forEach { add(ChartKind.Nutrition(it)) }
        wellnessFields.forEach { add(ChartKind.Wellness(it)) }
        cardioTypes(d).forEach { t -> cardioFields.forEach { add(ChartKind.Cardio(t, it)) } }
        d.trackers.forEach { add(ChartKind.TrackerItem(it.id, it.name.ifBlank { "Tracker" })) }
    }

    private fun logsIn(d: AppData, from: LocalDate?, to: LocalDate?): List<DayLog> =
        d.logs.values.filter { l ->
            val ld = runCatching { LocalDate.parse(l.date) }.getOrNull() ?: return@filter false
            (from == null || !ld.isBefore(from)) && (to == null || !ld.isAfter(to))
        }.sortedBy { it.date }

    private fun dayX(date: String) = LocalDate.parse(date).toEpochDay().toDouble()

    fun build(d: AppData, kind: ChartKind, from: LocalDate?, to: LocalDate?): Series {
        val s = d.settings
        val logs = logsIn(d, from, to)
        fun inRange(date: String): Boolean {
            val ld = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
            return (from == null || !ld.isBefore(from)) && (to == null || !ld.isAfter(to))
        }
        return when (kind) {
            ChartKind.BodyWeight -> {
                val map = sortedMapOf<String, Double>()
                logs.forEach { l -> l.weight.num()?.let { map[l.date] = it } }
                d.measurements.filter { inRange(it.date) }.forEach { m -> m.weight.num()?.let { map[m.date] = it } }
                Series("Body weight", s.weightUnit, map.map { Pt(dayX(it.key), it.value) })
            }
            is ChartKind.Measure -> {
                val f = measureFields(s).first { it.label == kind.field }
                val pts = d.measurements.filter { inRange(it.date) }.sortedBy { it.date }
                    .mapNotNull { m -> f.get(m).num()?.let { Pt(dayX(m.date), it) } }
                Series(kind.field, f.unit(s), pts, lowerIsBetter = kind.field in setOf("Waist", "Hips", "Body fat"))
            }
            is ChartKind.ExerciseTop -> exerciseSeries(logs, kind.name, kind.label, s.weightUnit) { it.topWeight() }
            is ChartKind.Exercise1RM -> exerciseSeries(logs, kind.name, kind.label, s.weightUnit) { it.estimated1RM() }
            is ChartKind.ExerciseVolume -> exerciseSeries(logs, kind.name, kind.label, s.weightUnit + "×reps") { it.volume().takeIf { v -> v > 0 } }
            is ChartKind.Nutrition -> {
                val (unit, goal) = when (kind.field) {
                    "Calories" -> "kcal" to s.calorieGoal.num()
                    "Protein" -> "g" to s.proteinGoal.num()
                    "Fat" -> "g" to s.fatGoal.num()
                    "Carbs" -> "g" to s.carbsGoal.num()
                    else -> "g" to s.extraGoal.num()
                }
                val pts = logs.filter { it.meals.any { m -> m.items.any { f -> !f.isEmpty() } } }.map { l ->
                    val m = l.macros()
                    val v = when (kind.field) {
                        "Calories" -> m.calories; "Protein" -> m.protein; "Fat" -> m.fat; "Carbs" -> m.carbs; else -> m.extra
                    }
                    Pt(dayX(l.date), v)
                }
                Series("Daily ${kind.field.lowercase()}", unit, pts, goal = goal)
            }
            is ChartKind.Wellness -> {
                val (unit, get) = when (kind.field) {
                    "Sleep" -> "hrs" to { l: DayLog -> l.sleep.num() }
                    "Water" -> s.waterUnit to { l: DayLog -> l.water.num() }
                    "Mood" -> "/5" to { l: DayLog -> l.mood.takeIf { it > 0 }?.toDouble() }
                    "Energy" -> "/5" to { l: DayLog -> l.energy.takeIf { it > 0 }?.toDouble() }
                    "Activity" -> "/5" to { l: DayLog -> l.activity.takeIf { it > 0 }?.toDouble() }
                    "Workout rating" -> "%" to { l: DayLog -> l.rating.takeIf { it > 0 }?.toDouble() }
                    "Caffeine drinks" -> "" to { l: DayLog -> l.caffeine.num() }
                    "Fruit" -> "srv" to { l: DayLog -> l.fruit.num() }
                    "Veggies" -> "srv" to { l: DayLog -> l.veggies.num() }
                    else -> "min" to { l: DayLog -> l.duration.minutes() }
                }
                Series(kind.field, unit, logs.mapNotNull { l -> get(l)?.let { Pt(dayX(l.date), it) } })
            }
            is ChartKind.Cardio -> {
                val pts = logs.mapNotNull { l ->
                    val entries = l.cardio.filter { it.type.trim().equals(kind.type, ignoreCase = true) }
                    if (entries.isEmpty()) return@mapNotNull null
                    val v = when (kind.field) {
                        "Distance" -> entries.mapNotNull { it.distance.num() }.takeIf { it.isNotEmpty() }?.sum()
                        "Time" -> entries.mapNotNull { it.time.minutes() }.takeIf { it.isNotEmpty() }?.sum()
                        else -> entries.mapNotNull { it.calories.num() }.takeIf { it.isNotEmpty() }?.sum()
                    }
                    v?.let { Pt(dayX(l.date), it) }
                }
                val unit = when (kind.field) { "Distance" -> s.distanceUnit; "Time" -> "min"; else -> "kcal" }
                Series(kind.label, unit, pts, lowerIsBetter = kind.field == "Time")
            }
            is ChartKind.TrackerItem -> {
                val t = d.trackers.firstOrNull { it.id == kind.id }
                val pts = t?.weeks?.mapIndexedNotNull { i, v ->
                    (v.num() ?: v.takeIf { it.contains(":") }?.minutes())?.let { Pt((i + 1).toDouble(), it) }
                } ?: emptyList()
                Series(t?.name?.ifBlank { "Tracker" } ?: "Tracker", t?.unit ?: "", pts, xIsWeek = true)
            }
        }
    }

    private fun exerciseSeries(
        logs: List<DayLog>, name: String, title: String, unit: String,
        value: (com.workoutlog.app.model.ExerciseEntry) -> Double?,
    ): Series {
        val pts = logs.mapNotNull { l ->
            l.exercises.filter { it.name.trim().equals(name, ignoreCase = true) }
                .mapNotNull(value).maxOrNull()?.let { Pt(dayX(l.date), it) }
        }
        return Series(title, unit, pts)
    }
}

/** Colors as ARGB ints so the same renderer works on screen and in the PDF. */
data class ChartColors(
    val line: Int, val fill: Int, val goal: Int, val text: Int, val grid: Int, val point: Int,
)

/**
 * Draws a line chart onto an android.graphics.Canvas. The app screen
 * (Compose's nativeCanvas) and the PDF page both use this same code.
 * [scale] = pixels per "unit"; use screen density on screen and ~0.75 for PDF points.
 */
object ChartRenderer {
    private val shortFmt = DateTimeFormatter.ofPattern("M/d")
    private val longFmt = DateTimeFormatter.ofPattern("M/d/yy")

    fun draw(c: Canvas, width: Float, height: Float, s: Series, col: ChartColors, scale: Float) {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col.text; textSize = 11f * scale }
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col.grid; strokeWidth = 1f * scale / 2f + 0.5f }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = col.line; strokeWidth = 2.5f * scale; style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col.fill; style = Paint.Style.FILL }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col.point; style = Paint.Style.FILL }
        val goalP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = col.goal; strokeWidth = 1.8f * scale; style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(8f * scale, 5f * scale), 0f)
        }

        if (s.points.isEmpty()) {
            text.textAlign = Paint.Align.CENTER
            c.drawText("No data yet for this range", width / 2, height / 2, text)
            return
        }

        // ---- Y range with "nice" round tick values ----
        var minY = s.points.minOf { it.y }
        var maxY = s.points.maxOf { it.y }
        s.goal?.let { minY = minOf(minY, it); maxY = maxOf(maxY, it) }
        if (abs(maxY - minY) < 1e-9) { minY -= 1; maxY += 1 }
        val step = niceStep((maxY - minY) / 4)
        val yLo = floor(minY / step) * step
        val yHi = ceil(maxY / step) * step
        val ticks = generateSequence(yLo) { it + step }.takeWhile { it <= yHi + step / 2 }.toList()

        val left = ticks.maxOf { text.measureText(it.fmt()) } + 8f * scale
        val right = width - 10f * scale
        val top = 10f * scale
        val bottom = height - 20f * scale

        // ---- X range ----
        var minX = s.points.minOf { it.x }
        var maxX = s.points.maxOf { it.x }
        if (maxX - minX < 1e-9) { minX -= 1; maxX += 1 }
        fun px(x: Double) = (left + (x - minX) / (maxX - minX) * (right - left)).toFloat()
        fun py(y: Double) = (bottom - (y - yLo) / (yHi - yLo) * (bottom - top)).toFloat()

        // Grid + Y labels
        text.textAlign = Paint.Align.RIGHT
        for (t in ticks) {
            val y = py(t)
            c.drawLine(left, y, right, y, grid)
            c.drawText(t.fmt(), left - 5f * scale, y + 4f * scale, text)
        }

        // X labels (up to 5)
        text.textAlign = Paint.Align.CENTER
        val span = maxX - minX
        val labelCount = 5
        val usedX = mutableSetOf<String>()
        for (i in 0 until labelCount) {
            val xv = minX + span * i / (labelCount - 1)
            val lbl = if (s.xIsWeek) "W${xv.toInt()}"
            else LocalDate.ofEpochDay(Math.round(xv)).format(if (span > 300) longFmt else shortFmt)
            if (!usedX.add(lbl)) continue
            text.textAlign = when (i) { 0 -> Paint.Align.LEFT; labelCount - 1 -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
            val xp = when (i) { 0 -> left; labelCount - 1 -> right; else -> px(xv) }
            c.drawText(lbl, xp, height - 4f * scale, text)
        }

        // Goal line
        s.goal?.let { g ->
            val y = py(g)
            val p = Path().apply { moveTo(left, y); lineTo(right, y) }
            c.drawPath(p, goalP)
            text.textAlign = Paint.Align.RIGHT
            text.color = col.goal
            c.drawText("Goal ${g.fmt()}", right, y - 4f * scale, text)
            text.color = col.text
        }

        // Area under line + line
        val pts = s.points.sortedBy { it.x }
        if (pts.size > 1) {
            val area = Path().apply {
                moveTo(px(pts.first().x), bottom)
                pts.forEach { lineTo(px(it.x), py(it.y)) }
                lineTo(px(pts.last().x), bottom); close()
            }
            c.drawPath(area, fill)
            val path = Path().apply {
                moveTo(px(pts.first().x), py(pts.first().y))
                pts.drop(1).forEach { lineTo(px(it.x), py(it.y)) }
            }
            c.drawPath(path, line)
        }
        val r = if (pts.size > 60) 1.8f * scale else 3.5f * scale
        pts.forEach { c.drawCircle(px(it.x), py(it.y), r, dot) }
    }

    private fun niceStep(raw: Double): Double {
        if (raw <= 0) return 1.0
        val mag = 10.0.pow(floor(log10(raw)))
        val n = raw / mag
        val nice = when { n <= 1 -> 1.0; n <= 2 -> 2.0; n <= 2.5 -> 2.5; n <= 5 -> 5.0; else -> 10.0 }
        return nice * mag
    }
}
