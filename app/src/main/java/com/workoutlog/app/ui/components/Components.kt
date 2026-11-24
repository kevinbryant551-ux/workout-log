package com.workoutlog.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workoutlog.app.ui.theme.LocalThemeStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val ShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yy")
val LongDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

fun String.prettyDate(): String = try { LocalDate.parse(this).format(ShortDate) } catch (_: Exception) { this }

/** A rounded card with a header, used for each "box" from the paper log. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalThemeStyle.current
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    style.headerTitle(title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** Standard labeled text box. */
@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

/** Small spreadsheet-style cell, like the boxes in the set/food tables. */
@Composable
fun CellField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = true,
    placeholder: String = "",
    shaded: Boolean = false,
    align: TextAlign = TextAlign.Center,
) {
    val cs = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface, textAlign = align),
        cursorBrush = SolidColor(cs.primary),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = modifier
            .height(40.dp)
            .background(if (shaded) cs.surfaceVariant.copy(alpha = 0.6f) else cs.surface, MaterialTheme.shapes.extraSmall)
            .border(1.dp, cs.outlineVariant, MaterialTheme.shapes.extraSmall),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 4.dp), contentAlignment = if (align == TextAlign.Start) Alignment.CenterStart else Alignment.Center) {
                if (value.isEmpty() && placeholder.isNotEmpty())
                    Text(placeholder, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant.copy(alpha = 0.6f), maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
fun SmallLabel(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun CheckItem(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clickable { onChange(!checked) }.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Row of 5 tappable boxes (Energy / Activity scales on the diet page). */
@Composable
fun BoxScale(label: String, value: Int, onChange: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        for (i in 1..5) {
            val on = i <= value
            Box(
                Modifier
                    .padding(3.dp)
                    .size(34.dp)
                    .background(if (on) cs.primary else cs.surface, MaterialTheme.shapes.extraSmall)
                    .border(1.5.dp, if (on) cs.primary else cs.outline, MaterialTheme.shapes.extraSmall)
                    .clickable { onChange(if (value == i) 0 else i) },
                contentAlignment = Alignment.Center,
            ) {
                Text("$i", color = if (on) cs.onPrimary else cs.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

/** The five mood faces from the diet page. */
val MoodFaces = listOf("😣", "🙁", "😐", "🙂", "😄")

@Composable
fun MoodScale(value: Int, onChange: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text("Mood", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        MoodFaces.forEachIndexed { idx, face ->
            val i = idx + 1
            val on = value == i
            Box(
                Modifier
                    .padding(3.dp)
                    .size(34.dp)
                    .background(if (on) cs.primaryContainer else cs.surface, MaterialTheme.shapes.extraLarge)
                    .border(if (on) 2.dp else 1.dp, if (on) cs.primary else cs.outlineVariant, MaterialTheme.shapes.extraLarge)
                    .clickable { onChange(if (on) 0 else i) },
                contentAlignment = Alignment.Center,
            ) { Text(face, fontSize = 18.sp) }
        }
    }
}

/** Button that opens a calendar date picker. Value is an ISO date string. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateButton(
    label: String,
    isoDate: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelSmall)
            Text(if (isoDate.isBlank()) "Pick date" else isoDate.prettyDate(), fontWeight = FontWeight.SemiBold)
        }
    }
    if (open) {
        val initial = try {
            LocalDate.parse(isoDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) { System.currentTimeMillis() }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                if (isoDate.isNotBlank()) TextButton(onClick = { onPick(""); open = false }) { Text("Clear") }
                TextButton(onClick = { open = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }
}

/** Simple dropdown chooser. */
@Composable
fun <T> Chooser(
    label: String,
    options: List<T>,
    selected: T?,
    display: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(
                    selected?.let(display) ?: "—",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(display(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

/** Day-of-week boxes (S M T W T F S) like the top of the workout page. */
@Composable
fun WeekdayBoxes(date: LocalDate) {
    val cs = MaterialTheme.colorScheme
    val letters = listOf("S", "M", "T", "W", "T", "F", "S")
    val today = date.dayOfWeek.value % 7 // Sunday = 0
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        letters.forEachIndexed { i, l ->
            val on = i == today
            Box(
                Modifier.size(22.dp)
                    .background(if (on) cs.primary else cs.surface, MaterialTheme.shapes.extraSmall)
                    .border(1.dp, if (on) cs.primary else cs.outlineVariant, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) { Text(l, fontSize = 10.sp, color = if (on) cs.onPrimary else cs.onSurfaceVariant) }
        }
    }
}

@Composable
fun IconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = desc) }
}
