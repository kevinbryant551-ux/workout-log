package com.workoutlog.app.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.DayLog
import com.workoutlog.app.model.Photos
import com.workoutlog.app.model.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Holds all app data in memory and saves it as one JSON file in the app's
 * private storage (/data/data/<app>/files/workout_log.json).
 * Saves are "debounced": typing quickly only writes the file once you pause.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val file = File(app.filesDir, "workout_log.json")
    private val photoDir = File(app.filesDir, "photos").apply { mkdirs() }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data

    private var saveJob: Job? = null

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        // Keep a copy of an unreadable file instead of silently overwriting it.
        file.copyTo(File(file.parentFile, "workout_log.corrupt.json"), overwrite = true)
        AppData()
    }

    fun update(transform: (AppData) -> AppData) {
        _data.value = transform(_data.value)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            writeNow()
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) = update { it.copy(settings = transform(it.settings)) }

    fun updateDay(date: String, transform: (DayLog) -> DayLog) = update { d ->
        val current = d.logs[date] ?: DayLog(date)
        d.copy(logs = d.logs + (date to transform(current)))
    }

    /** Called when the app goes to the background so nothing is lost. */
    fun flush() {
        saveJob?.cancel()
        viewModelScope.launch { writeNow() }
    }

    private suspend fun writeNow() = withContext(Dispatchers.IO) {
        val snapshot = _data.value
        // Drop days that were opened but never filled in.
        val cleaned = snapshot.copy(logs = snapshot.logs.filterValues { !it.isEmpty() })
        val tmp = File(file.parentFile, "workout_log.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), cleaned))
        tmp.renameTo(file) // atomic replace
    }

    // ---------- Backup ----------
    fun exportJson(): String = json.encodeToString(AppData.serializer(), _data.value)

    fun importJson(text: String): Boolean = try {
        val imported = json.decodeFromString(AppData.serializer(), text)
        update { imported.copy(photos = it.photos) }
        true
    } catch (e: Exception) { false }

    // ---------- Photos ----------
    fun savePhoto(uri: Uri, before: Boolean, date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = if (before) "before.jpg" else "after.jpg"
            val dest = File(photoDir, name)
            val ctx = getApplication<Application>()
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                // Downscale big camera photos to keep storage and memory small.
                val bmp = BitmapFactory.decodeStream(input) ?: return@use
                val scale = minOf(1f, 1600f / maxOf(bmp.width, bmp.height))
                val scaled = if (scale < 1f)
                    Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
                else bmp
                dest.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            }
            withContext(Dispatchers.Main) {
                update { d ->
                    val p = d.photos
                    d.copy(
                        photos = if (before) p.copy(beforePath = dest.absolutePath + "?" + System.currentTimeMillis(), beforeDate = p.beforeDate.ifBlank { date })
                        else p.copy(afterPath = dest.absolutePath + "?" + System.currentTimeMillis(), afterDate = p.afterDate.ifBlank { date })
                    )
                }
            }
        }
    }

    fun removePhoto(before: Boolean) = update { d ->
        File(photoDir, if (before) "before.jpg" else "after.jpg").delete()
        d.copy(photos = if (before) d.photos.copy(beforePath = "") else d.photos.copy(afterPath = ""))
    }

    fun updatePhotos(transform: (Photos) -> Photos) = update { it.copy(photos = transform(it.photos)) }

    override fun onCleared() {
        super.onCleared()
    }
}

/** Loads a stored photo path ("file?cacheBuster") into a bitmap, downsampled. */
fun loadPhoto(path: String, maxSize: Int = 1200): Bitmap? {
    if (path.isBlank()) return null
    val f = File(path.substringBefore("?"))
    if (!f.exists()) return null
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.absolutePath, opts)
    var sample = 1
    while (maxOf(opts.outWidth, opts.outHeight) / sample > maxSize) sample *= 2
    return BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
}
