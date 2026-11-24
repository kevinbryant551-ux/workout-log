package com.workoutlog.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.workoutlog.app.charts.ChartColors
import com.workoutlog.app.charts.ChartData
import com.workoutlog.app.charts.ChartKind
import com.workoutlog.app.charts.ChartRenderer
import com.workoutlog.app.charts.Series
import com.workoutlog.app.model.AppData
import com.workoutlog.app.ui.components.Chooser
import com.workoutlog.app.ui.components.SectionCard
import com.workoutlog.app.ui.theme.LocalThemeStyle
import java.time.LocalDate

val RangeOptions = listOf("30 days", "90 days", "1 year", "All time")

fun rangeStart(r: String): LocalDate? = when (r) {
    "30 days" -> LocalDate.now().minusDays(30)
    "90 days" -> LocalDate.now().minusDays(90)
    "1 year" -> LocalDate.now().minusYears(1)
    else -> null
}

@Composable
fun ChartsScreen(data: AppData) {
    val kinds = remember(data) { ChartData.allKinds(data) }
    val categories = kinds.map { it.category }.distinct()
    var category by rememberSaveable { mutableStateOf("Body") }
    var range by rememberSaveable { mutableStateOf("90 days") }
    var selectedLabel by rememberSaveable { mutableStateOf<String?>(null) }

    val inCat = kinds.filter { it.category == category }
    val kind = inCat.firstOrNull { it.label == selectedLabel } ?: inCat.firstOrNull()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { c ->
                FilterChip(selected = c == category, onClick = { category = c; selectedLabel = null }, label = { Text(c) })
            }
        }
        if (inCat.isNotEmpty()) {
            Chooser("Graph", inCat, kind, { it.label }, { selectedLabel = it.label },
                Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        } else {
            Text(
                when (category) {
                    "Strength" -> "Log some exercises on the Log tab to see strength graphs."
                    "Cardio" -> "Log some cardio on the Log tab to see cardio graphs."
                    else -> "Nothing to graph yet."
                },
                Modifier.padding(16.dp),
            )
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RangeOptions.forEach { r -> FilterChip(selected = r == range, onClick = { range = r }, label = { Text(r) }) }
        }

        if (kind != null) {
            val from = if (kind is ChartKind.TrackerItem) null else rangeStart(range)
            val series = remember(data, kind, range) { ChartData.build(data, kind, from, null) }
            SectionCard(series.title + if (series.unit.isNotBlank()) " (${series.unit})" else "") {
                LineChart(series, Modifier.fillMaxWidth().height(260.dp))
                Text(series.summary(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                "Tip: the PDF button (top right) can include these graphs in a printable report.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun LineChart(series: Series, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val style = LocalThemeStyle.current
    val density = LocalDensity.current.density
    val colors = ChartColors(
        line = style.chartLine.toArgb(), fill = style.chartFill.toArgb(), goal = style.chartGoal.toArgb(),
        text = cs.onSurfaceVariant.toArgb(), grid = cs.outlineVariant.toArgb(), point = style.chartLine.toArgb(),
    )
    Canvas(modifier) {
        drawIntoCanvas { ChartRenderer.draw(it.nativeCanvas, size.width, size.height, series, colors, density) }
    }
}
