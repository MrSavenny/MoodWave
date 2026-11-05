package com.example.moodwave.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.moodwave.analytics.MoodAnalytics
import com.example.moodwave.model.MoodEntry
import com.example.moodwave.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.LocalDate

private typealias ExportReport = (List<MoodEntry>, YearMonth, Boolean) -> Unit
private val LocalExportReport = staticCompositionLocalOf<ExportReport> { error("Exportador de reportes no disponible") }

/** Keep the picker callback registered even when the diary locks or the report button leaves the list. */
@Composable
fun MonthlyReportExportHost(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    fun notify(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val file = pending?.let { java.io.File(context.cacheDir, it) }
        if (file != null) scope.launch {
            try {
                if (uri != null) {
                    withContext(Dispatchers.IO) {
                        val size = file.length()
                        check(size > 0) { "El reporte preparado no está disponible." }
                        val stream = requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "No se pudo abrir el destino." }
                        stream.use { out -> file.inputStream().use { check(it.copyTo(out) == size) }; out.flush() }
                    }
                    notify("Reporte mensual guardado")
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { notify("Error al guardar: ${e.localizedMessage}") }
            finally { pending = null; file.delete() }
        }
    }
    val export: ExportReport = { rows, month, demo ->
        if (pending == null) {
            val file = java.io.File(context.cacheDir, "monthly-${java.util.UUID.randomUUID()}.pdf")
            pending = file.name
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        file.outputStream().use { PdfReportGenerator.write(it, rows.filter { row -> MoodAnalytics.isDemo(row) == demo }, month, demo) }
                        check(file.length() > 0) { "No se pudo generar el reporte." }
                    }
                    save.launch("MoodWave_$month.pdf")
                } catch (e: Exception) {
                    file.delete()
                    pending = null
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    notify("Error al preparar el reporte: ${e.localizedMessage}")
                }
            }
        }
    }
    CompositionLocalProvider(LocalExportReport provides export, content = content)
}

@Composable
fun MonthlyReportButton(entries: List<MoodEntry>, demo: Boolean = false) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val sourceRows = remember(entries, demo) { entries.filter { MoodAnalytics.isDemo(it) == demo } }
    val recommendedMonth = remember(sourceRows) {
        val rowsByMonth = sourceRows.mapNotNull { row ->
            runCatching { YearMonth.from(LocalDate.parse(row.date)) }.getOrNull()?.let { it to row.date }
        }.groupBy({ it.first }, { it.second })
        rowsByMonth.filterValues { dates -> dates.distinct().size >= MoodAnalytics.MIN_DAYS }
            .keys.maxOrNull() ?: YearMonth.now()
    }
    var period by rememberSaveable(demo, entries.size) { mutableStateOf(recommendedMonth.toString()) }
    val month = YearMonth.parse(period)
    val rowsInMonth = MoodAnalytics.inPeriod(sourceRows, month.atDay(1), month.atEndOfMonth())
    val daysInMonth = rowsInMonth.map { it.date }.distinct().size
    val export = LocalExportReport.current
    OutlinedButton(onClick = { choosing = true }) { Text("Reporte PDF mensual") }
    if (choosing) AlertDialog(
        onDismissRequest = { choosing = false },
        title = { Text("Mes del reporte") },
        text = { Column {
            Text(if (demo) "Origen: demostración" else "Origen: registros personales")
            Text("Periodo: $period · ${rowsInMonth.size} registros en $daysInMonth días")
            if (daysInMonth < MoodAnalytics.MIN_DAYS) {
                Text("Este mes puede generar un reporte breve. Para ver actividades y conclusiones se necesitan al menos 14 días registrados.")
            }
            Row {
                TextButton(onClick = { period = month.minusMonths(1).toString() }) { Text("Anterior") }
                TextButton(enabled = month < YearMonth.now(), onClick = { period = month.plusMonths(1).toString() }) { Text("Siguiente") }
            }
        } },
        confirmButton = { TextButton(onClick = {
            choosing = false
            export(entries, month, demo)
        }) { Text("Guardar PDF") } },
        dismissButton = { TextButton(onClick = { choosing = false }) { Text("Cancelar") } }
    )
}

