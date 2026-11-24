package com.workoutlog.app.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.workoutlog.app.charts.ChartColors
import com.workoutlog.app.charts.ChartData
import com.workoutlog.app.charts.ChartKind
import com.workoutlog.app.charts.ChartRenderer
import com.workoutlog.app.data.loadPhoto
import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.DayLog
import com.workoutlog.app.model.Macros
import com.workoutlog.app.model.fmt
import com.workoutlog.app.model.macros
import com.workoutlog.app.model.num
import com.workoutlog.app.ui.components.MoodFaces
import com.workoutlog.app.ui.screens.measureFields
import com.workoutlog.app.ui.theme.AppTheme
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ReportOptions(
    val from: LocalDate,
    val to: LocalDate,
    val goals: Boolean = true,
    val measurements: Boolean = true,
    val workouts: Boolean = true,
    val nutrition: Boolean = true,
    val trackers: Boolean = true,
    val charts: Boolean = true,
)

/**
 * Where pages get drawn. On the phone this is a real PdfDocument; tests use a
 * bitmap-backed version so the pages can be checked as images.
 */
interface PageSink {
    fun startPage(number: Int, width: Int, height: Int): Canvas
    fun finishPage()
    fun close()
}

class PdfDocumentSink(private val file: File) : PageSink {
    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    override fun startPage(number: Int, width: Int, height: Int): Canvas {
        val p = doc.startPage(PdfDocument.PageInfo.Builder(width, height, number).create())
        page = p
        return p.canvas
    }
    override fun finishPage() { page?.let { doc.finishPage(it) }; page = null }
    override fun close() {
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}

/**
 * Builds a multi-page US-Letter PDF using Android's built-in PdfDocument.
 * Units are PostScript points (72 per inch). Page = 612 x 792.
 */
class PdfReport(private val data: AppData, private val theme: AppTheme, private val opt: ReportOptions) {

    private val pageW = 612f
    private val pageH = 792f
    private val margin = 40f
    private val contentW = pageW - margin * 2

    private val accent = if (theme == AppTheme.MENS) Color.rgb(31, 79, 130) else Color.rgb(194, 24, 91)
    private val accent2 = if (theme == AppTheme.MENS) Color.rgb(217, 96, 15) else Color.rgb(130, 80, 196)
    private val shade = if (theme == AppTheme.MENS) Color.rgb(230, 236, 242) else Color.rgb(252, 230, 238)
    private val ink = Color.rgb(30, 30, 34)
    private val grey = Color.rgb(110, 110, 118)
    private val lineCol = Color.rgb(190, 190, 198)

    private lateinit var sink: PageSink
    private var pageOpen = false
    private lateinit var c: Canvas
    private var y = 0f
    private var pageNo = 0

    private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val dayFmt = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")

    private fun paint(size: Float, color: Int = ink, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; textAlign = align
            typeface = when {
                theme == AppTheme.WOMENS && bold -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                bold -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                else -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
        }

    private fun title(t: String) = if (theme == AppTheme.MENS) t.uppercase() else t

    // ---------------- page management ----------------
    private fun newPage() {
        if (pageOpen) finishPage()
        pageNo++
        c = sink.startPage(pageNo, pageW.toInt(), pageH.toInt())
        c.drawColor(Color.WHITE)
        pageOpen = true
        // Header band
        c.drawRect(0f, 0f, pageW, 6f, Paint().apply { color = accent })
        c.drawText(title("Workout & Diet Log"), margin, 26f, paint(9f, grey, bold = true))
        c.drawText(
            "${opt.from.format(dateFmt)} – ${opt.to.format(dateFmt)}",
            pageW - margin, 26f, paint(9f, grey, align = Paint.Align.RIGHT),
        )
        y = 46f
    }

    private fun finishPage() {
        c.drawText("Page $pageNo", pageW / 2, pageH - 18f, paint(8f, grey, align = Paint.Align.CENTER))
        sink.finishPage()
        pageOpen = false
    }

    private fun ensure(space: Float) { if (y + space > pageH - 40f) newPage() }

    // ---------------- drawing helpers ----------------
    private fun sectionTitle(t: String) {
        ensure(60f)
        y += 8f
        c.drawText(title(t), margin, y + 14f, paint(15f, accent, bold = true))
        y += 20f
        c.drawLine(margin, y, pageW - margin, y, Paint().apply { color = accent; strokeWidth = 1.5f })
        y += 10f
    }

    private fun subTitle(t: String, color: Int = ink) {
        ensure(40f)
        c.drawText(t, margin, y + 11f, paint(11.5f, color, bold = true))
        y += 17f
    }

    /** Wraps text into lines that fit [width]. */
    private fun wrap(text: String, p: Paint, width: Float): List<String> {
        val out = mutableListOf<String>()
        text.split("\n").forEach { para ->
            var line = ""
            para.split(" ").forEach { w ->
                val test = if (line.isEmpty()) w else "$line $w"
                if (p.measureText(test) <= width) line = test
                else {
                    if (line.isNotEmpty()) out += line
                    var rest = w
                    while (p.measureText(rest) > width && rest.length > 1) {
                        var n = rest.length
                        while (n > 1 && p.measureText(rest.substring(0, n)) > width) n--
                        out += rest.substring(0, n); rest = rest.substring(n)
                    }
                    line = rest
                }
            }
            out += line
        }
        return out
    }

    private fun paragraph(text: String, size: Float = 9.5f, color: Int = ink, x: Float = margin, width: Float = contentW) {
        val p = paint(size, color)
        wrap(text, p, width).forEach { l ->
            ensure(size + 4f)
            c.drawText(l, x, y + size, p)
            y += size + 3.5f
        }
    }

    private fun keyValues(items: List<Pair<String, String>>, cols: Int = 4) {
        val filtered = items.filter { it.second.isNotBlank() }
        if (filtered.isEmpty()) return
        val colW = contentW / cols
        filtered.chunked(cols).forEach { row ->
            ensure(26f)
            row.forEachIndexed { i, (k, v) ->
                val x = margin + i * colW
                c.drawText(k.uppercase(), x, y + 8f, paint(7f, grey, bold = true))
                c.drawText(fit(v, paint(10f), colW - 6f), x, y + 20f, paint(10f))
            }
            y += 26f
        }
    }

    private fun fit(s: String, p: Paint, w: Float): String {
        if (p.measureText(s) <= w) return s
        var t = s
        while (t.isNotEmpty() && p.measureText("$t…") > w) t = t.dropLast(1)
        return "$t…"
    }

    /**
     * Draws a bordered table. [widths] are relative weights. Rows marked in
     * [boldRows] get a shaded background (for subtotals / totals).
     */
    private fun table(
        header: List<String>, rows: List<List<String>>, widths: List<Float>,
        boldRows: Set<Int> = emptySet(), fontSize: Float = 8.5f, firstColLeft: Boolean = true,
    ) {
        val total = widths.sum()
        val ws = widths.map { it / total * contentW }
        val rowH = fontSize + 9f
        val border = Paint().apply { color = lineCol; style = Paint.Style.STROKE; strokeWidth = 0.6f }
        val headBg = Paint().apply { color = accent }
        val shadeBg = Paint().apply { color = shade }

        fun drawHeader() {
            var x = margin
            c.drawRect(margin, y, margin + contentW, y + rowH, headBg)
            header.forEachIndexed { i, h ->
                val p = paint(fontSize - 0.5f, Color.WHITE, bold = true, align = if (i == 0 && firstColLeft) Paint.Align.LEFT else Paint.Align.CENTER)
                val tx = if (i == 0 && firstColLeft) x + 4f else x + ws[i] / 2
                c.drawText(fit(h, p, ws[i] - 4f), tx, y + rowH - 5.5f, p)
                x += ws[i]
            }
            y += rowH
        }

        ensure(rowH * 3)
        drawHeader()
        rows.forEachIndexed { ri, r ->
            if (y + rowH > pageH - 40f) { newPage(); drawHeader() }
            val bold = ri in boldRows
            if (bold) c.drawRect(margin, y, margin + contentW, y + rowH, shadeBg)
            var x = margin
            r.forEachIndexed { i, cell ->
                val left = i == 0 && firstColLeft
                val p = paint(fontSize, ink, bold = bold, align = if (left) Paint.Align.LEFT else Paint.Align.CENTER)
                c.drawRect(x, y, x + ws[i], y + rowH, border)
                c.drawText(fit(cell, p, ws[i] - 4f), if (left) x + 4f else x + ws[i] / 2, y + rowH - 5.5f, p)
                x += ws[i]
            }
            y += rowH
        }
        y += 8f
    }

    // ---------------- sections ----------------
    private fun cover(logs: List<DayLog>) {
        newPage()
        y += 30f
        c.drawText(title("Fitness Progress Report"), margin, y + 26f, paint(26f, accent, bold = true))
        y += 40f
        c.drawText("${opt.from.format(dateFmt)}  –  ${opt.to.format(dateFmt)}", margin, y + 14f, paint(13f, grey))
        y += 26f
        c.drawText("Generated ${LocalDate.now().format(dateFmt)}", margin, y + 10f, paint(9f, grey))
        y += 30f

        val workouts = logs.filter { l -> l.exercises.any { !it.isEmpty() } || l.cardio.any { !it.isEmpty() } }
        val dietDays = logs.filter { it.meals.any { m -> m.items.any { f -> !f.isEmpty() } } }
        val avgCal = dietDays.map { it.macros().calories }.average().takeIf { !it.isNaN() }
        val avgProt = dietDays.map { it.macros().protein }.average().takeIf { !it.isNaN() }
        val avgSleep = logs.mapNotNull { it.sleep.num() }.average().takeIf { !it.isNaN() }
        val weights = logs.mapNotNull { l -> l.weight.num()?.let { l.date to it } }
        val avgRating = workouts.filter { it.rating > 0 }.map { it.rating }.average().takeIf { !it.isNaN() }
        val w = data.settings.weightUnit

        sectionTitle("Summary")
        keyValues(
            listOf(
                "Days logged" to logs.size.toString(),
                "Workouts" to workouts.size.toString(),
                "Days with food logged" to dietDays.size.toString(),
                "Avg workout rating" to (avgRating?.let { "${it.fmt()}%" } ?: "–"),
                "Avg calories / day" to (avgCal?.fmt() ?: "–"),
                "Avg protein / day" to (avgProt?.let { "${it.fmt()} g" } ?: "–"),
                "Avg sleep" to (avgSleep?.let { "${it.fmt()} hrs" } ?: "–"),
                "Weight change" to if (weights.size >= 2)
                    "${weights.first().second.fmt()} → ${weights.last().second.fmt()} $w" else "–",
            ),
        )
        // Top lifts in range
        val best = mutableMapOf<String, Double>()
        workouts.forEach { l -> l.exercises.forEach { e ->
            val top = e.sets.mapNotNull { it.weight.num() }.maxOrNull() ?: return@forEach
            val k = e.name.trim()
            if (k.isNotBlank()) best[k] = maxOf(best[k] ?: 0.0, top)
        } }
        if (best.isNotEmpty()) {
            subTitle("Heaviest sets in this period")
            table(
                listOf("Exercise", "Heaviest set ($w)"),
                best.entries.sortedByDescending { it.value }.take(15).map { listOf(it.key, it.value.fmt()) },
                listOf(3f, 1.4f),
            )
        }
    }

    private fun goalsSection() {
        if (data.goals.isEmpty()) return
        sectionTitle("My Goals")
        table(
            listOf("Your goal", "Start", "Deadline", "Action plan", "Achievement", "Done"),
            data.goals.map {
                listOf(it.goal, it.start.short(), it.deadline.short(), it.plan, it.achievement, if (it.achieved) "✓" else "")
            },
            listOf(2.2f, 1f, 1f, 3f, 2f, 0.6f),
        )
        // Full text of action plans, which can be long.
        data.goals.filter { it.plan.length > 40 || it.achievement.length > 30 }.forEach {
            subTitle(it.goal.ifBlank { "Goal" })
            if (it.plan.isNotBlank()) paragraph("Action plan: ${it.plan}")
            if (it.achievement.isNotBlank()) paragraph("Achievement: ${it.achievement}")
            y += 4f
        }
    }

    private fun measurementsSection() {
        val ms = data.measurements.filter { inRange(it.date) }.sortedBy { it.date }
        val hasPhotos = data.photos.beforePath.isNotBlank() || data.photos.afterPath.isNotBlank()
        if (ms.isEmpty() && !hasPhotos) return
        sectionTitle("Body Measurements")
        if (hasPhotos) {
            ensure(250f)
            val boxW = (contentW - 20f) / 2
            val boxH = 220f
            listOf(
                Triple("BEFORE", data.photos.beforePath, data.photos.beforeDate),
                Triple("AFTER", data.photos.afterPath, data.photos.afterDate),
            ).forEachIndexed { i, (lbl, path, date) ->
                val x = margin + i * (boxW + 20f)
                c.drawText(lbl + if (date.isNotBlank()) "  •  ${date.short()}" else "", x, y + 9f, paint(8.5f, grey, bold = true))
                val r = RectF(x, y + 14f, x + boxW, y + 14f + boxH)
                c.drawRect(r, Paint().apply { color = lineCol; style = Paint.Style.STROKE })
                loadPhoto(path, 900)?.let { bmp ->
                    val s = minOf(r.width() / bmp.width, r.height() / bmp.height)
                    val w = bmp.width * s; val h = bmp.height * s
                    val dst = RectF(r.centerX() - w / 2, r.centerY() - h / 2, r.centerX() + w / 2, r.centerY() + h / 2)
                    c.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                }
            }
            y += boxH + 26f
        }
        if (ms.isNotEmpty()) {
            val fields = measureFields(data.settings)
            // Paper-style table: metrics down the side, up to 7 dates across per block.
            ms.chunked(7).forEach { block ->
                table(
                    listOf("Date") + block.map { it.date.short() },
                    fields.map { f -> listOf("${f.label} (${f.unit(data.settings)})".replace(" ()", "")) + block.map { f.get(it) } },
                    listOf(1.6f) + block.map { 1f },
                )
            }
        }
    }

    private fun workoutDay(l: DayLog) {
        val s = data.settings
        keyValues(
            listOf(
                "Workout" to l.workoutName,
                "Warm up / Cool down" to listOfNotNull("Warm up".takeIf { l.warmUp }, "Cool down".takeIf { l.coolDown }).joinToString(" + "),
                "Duration" to l.duration.withUnit("min"),
                "Rating" to (if (l.rating > 0) "${l.rating}%" else ""),
                "Weight" to l.weight.withUnit(s.weightUnit),
                "Water" to l.water.withUnit(s.waterUnit),
                "Sleep" to l.sleep.withUnit("hrs"),
            ),
        )
        val ex = l.exercises.filter { !it.isEmpty() }
        if (ex.isNotEmpty()) {
            table(
                listOf("Exercise") + (1..6).map { "Set $it" },
                ex.map { e ->
                    listOf(e.name) + e.sets.map { st ->
                        when {
                            st.weight.isBlank() && st.reps.isBlank() -> ""
                            st.weight.isBlank() -> "× ${st.reps}"
                            st.reps.isBlank() -> st.weight
                            else -> "${st.weight} × ${st.reps}"
                        }
                    }
                },
                listOf(2.6f, 1f, 1f, 1f, 1f, 1f, 1f),
            )
        }
        val cardio = l.cardio.filter { !it.isEmpty() }
        if (cardio.isNotEmpty()) {
            table(
                listOf("Cardio", "Time", "Distance (${s.distanceUnit})", "Calories"),
                cardio.map { listOf(it.type, it.time, it.distance, it.calories) },
                listOf(2.4f, 1f, 1f, 1f),
            )
        }
        if (l.notes.isNotBlank()) paragraph("Notes: ${l.notes}", 9f, grey)
    }

    private fun dietDay(l: DayLog) {
        val s = data.settings
        val flags = listOfNotNull(
            "Sugar-free".takeIf { l.sugarFree }, "Gluten-free".takeIf { l.glutenFree },
            "Lactose-free".takeIf { l.lactoseFree }, "Dairy-free".takeIf { l.dairyFree },
        ).joinToString(", ")
        keyValues(
            listOf(
                "Mood" to (if (l.mood > 0) "${MoodFaces[l.mood - 1].let { moodWord(l.mood) }} (${l.mood}/5)" else ""),
                "Energy" to (if (l.energy > 0) "${l.energy}/5" else ""),
                "Activity" to (if (l.activity > 0) "${l.activity}/5" else ""),
                "Body fat" to l.bodyFat.withUnit("%"),
                "Caffeine drinks" to l.caffeine,
                "Fruit" to l.fruit,
                "Veggies" to l.veggies,
                "Diet" to flags,
            ),
        )
        val extra = s.extraNutrientLabel.ifBlank { "Extra" }
        val rows = mutableListOf<List<String>>()
        val bold = mutableSetOf<Int>()
        l.meals.forEach { m ->
            val items = m.items.filter { !it.isEmpty() }
            if (items.isEmpty()) return@forEach
            items.forEachIndexed { i, f ->
                val name = if (i == 0) "${m.name}${if (m.time.isNotBlank()) " ${m.time}" else ""}: ${f.product}" else "   ${f.product}"
                rows += listOf(name, f.serving, f.calories, f.protein, f.fat, f.carbs, f.extra)
            }
            val sub = m.macros()
            bold += rows.size
            rows += listOf("${m.name} subtotal", "") + sub.cells()
        }
        if (rows.isNotEmpty()) {
            val t = l.macros()
            bold += rows.size
            rows += listOf("TOTAL", "") + t.cells()
            val g = goals()
            if (g != null) {
                bold += rows.size
                rows += listOf("VS. GOAL", "") + listOf(
                    t.calories - g.calories, t.protein - g.protein, t.fat - g.fat, t.carbs - g.carbs, t.extra - g.extra,
                ).map { if (it.isNaN()) "–" else (if (it > 0) "+" else "") + it.fmt() }
            }
            table(
                listOf("Products", "Serv.", "Cal", "Prot", "Fat", "Carbs", extra),
                rows, listOf(3.4f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f, 0.9f), boldRows = bold,
            )
        }
    }

    private fun Macros.cells() = listOf(calories, protein, fat, carbs, extra).map { it.fmt() }

    private fun goals(): Macros? {
        val s = data.settings
        if (listOf(s.calorieGoal, s.proteinGoal, s.fatGoal, s.carbsGoal, s.extraGoal).all { it.isBlank() }) return null
        return Macros(
            s.calorieGoal.num() ?: Double.NaN, s.proteinGoal.num() ?: Double.NaN, s.fatGoal.num() ?: Double.NaN,
            s.carbsGoal.num() ?: Double.NaN, s.extraGoal.num() ?: Double.NaN,
        )
    }

    private fun dailySection(logs: List<DayLog>) {
        val show = logs.filter { (opt.workouts && it.hasWorkout()) || (opt.nutrition && it.hasDiet()) }
        if (show.isEmpty()) return
        sectionTitle(
            when {
                opt.workouts && opt.nutrition -> "Daily Workout & Diet Log"
                opt.workouts -> "Daily Workout Log"
                else -> "Daily Diet Log"
            },
        )
        show.forEach { l ->
            ensure(90f)
            val d = runCatching { LocalDate.parse(l.date).format(dayFmt) }.getOrDefault(l.date)
            c.drawRect(margin, y, margin + contentW, y + 18f, Paint().apply { color = shade })
            c.drawText(d, margin + 6f, y + 13f, paint(10.5f, accent, bold = true))
            y += 24f
            if (opt.workouts && l.hasWorkout()) {
                c.drawText("WORKOUT", margin, y + 8f, paint(7.5f, accent2, bold = true)); y += 12f
                workoutDay(l)
            }
            if (opt.nutrition && l.hasDiet()) {
                ensure(40f)
                c.drawText("DIET", margin, y + 8f, paint(7.5f, accent2, bold = true)); y += 12f
                dietDay(l)
            }
            y += 6f
        }
    }

    private fun trackersSection() {
        val ts = data.trackers.filter { t -> t.weeks.any { it.isNotBlank() } }
        if (ts.isEmpty()) return
        sectionTitle("Progress Tracker")
        val start = runCatching { LocalDate.parse(data.settings.trackerStartDate) }.getOrNull()
        if (start != null) paragraph("Week 1 began ${start.format(dateFmt)}.", 9f, grey)
        ts.forEach { t ->
            subTitle(t.name.ifBlank { "Item to track" } + if (t.unit.isNotBlank()) " (${t.unit})" else "")
            // 3 rows of 11 weeks, exactly like the paper tracker.
            (0 until 33).chunked(11).forEach { wk ->
                table(
                    wk.map { "Wk ${it + 1}" },
                    listOf(wk.map { t.weeks.getOrElse(it) { "" } }),
                    wk.map { 1f }, fontSize = 7.5f, firstColLeft = false,
                )
                y -= 4f
            }
            y += 6f
        }
    }

    private fun chartsSection() {
        val kinds = ChartData.allKinds(data)
        val series = kinds.map { k ->
            ChartData.build(data, k, if (k is ChartKind.TrackerItem) null else opt.from, if (k is ChartKind.TrackerItem) null else opt.to)
        }.filter { it.points.size >= 2 }
        if (series.isEmpty()) return
        sectionTitle("Graphs")
        val colors = ChartColors(
            line = accent, fill = Color.argb(40, Color.red(accent), Color.green(accent), Color.blue(accent)),
            goal = accent2, text = grey, grid = Color.rgb(225, 225, 230), point = accent,
        )
        val chartH = 170f
        series.forEach { s ->
            ensure(chartH + 50f)
            subTitle(s.title + if (s.unit.isNotBlank()) " (${s.unit})" else "")
            c.save()
            c.translate(margin, y)
            ChartRenderer.draw(c, contentW, chartH, s, colors, 0.85f)
            c.restore()
            y += chartH + 6f
            paragraph(s.summary(), 8.5f, grey)
            y += 8f
        }
    }

    // ---------------- entry point ----------------
    fun writeTo(file: File): File {
        write(PdfDocumentSink(file))
        return file
    }

    fun write(sink: PageSink) {
        this.sink = sink
        val logs = data.logs.values.filter { inRange(it.date) && !it.isEmpty() }.sortedBy { it.date }
        cover(logs)
        if (opt.goals) goalsSection()
        if (opt.measurements) measurementsSection()
        if (opt.trackers) trackersSection()
        if (opt.charts) chartsSection()
        if (opt.workouts || opt.nutrition) dailySection(logs)
        if (pageOpen) finishPage()
        sink.close()
    }

    private fun inRange(date: String): Boolean {
        val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        return !d.isBefore(opt.from) && !d.isAfter(opt.to)
    }

    private fun String.short(): String = runCatching { LocalDate.parse(this).format(DateTimeFormatter.ofPattern("M/d/yy")) }.getOrDefault(this)
    private fun String.withUnit(u: String) = if (isBlank()) "" else "$this $u"
    private fun moodWord(m: Int) = listOf("Awful", "Low", "Okay", "Good", "Great")[m - 1]
}
