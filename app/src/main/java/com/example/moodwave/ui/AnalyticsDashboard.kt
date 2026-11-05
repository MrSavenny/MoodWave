package com.example.moodwave.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.analytics.MoodAnalytics
import com.example.moodwave.model.MoodEntry
import com.example.moodwave.ui.theme.PastelCard
import com.example.moodwave.ui.theme.PastelLavender
import com.example.moodwave.ui.theme.PastelMuted
import com.example.moodwave.ui.theme.PastelPink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

private val LightPanelInk = Color(0xFF242D43)
private val Ink @Composable get() = com.example.moodwave.ui.theme.PastelText
private val Indigo = com.example.moodwave.ui.theme.PatternPurple
private val Mint = Color(0xFF2AA997)

/** Segunda etapa: tres gráficas (línea, barras y dispersión), estadísticas e insights. */
@Composable
fun AnalyticsDashboard(
    entries: List<MoodEntry>,
    onGenerateDemo: () -> Unit,
    onClearDemo: () -> Unit,
    onEraseAll: () -> Unit
) {
    var view by rememberSaveable { mutableStateOf("Estadísticas") }
    var manage by rememberSaveable { mutableStateOf(false) }
    var days by rememberSaveable { mutableStateOf(30) }
    var demoMode by rememberSaveable { mutableStateOf(false) }
    var confirmDemo by remember { mutableStateOf(false) }
    var confirmClearDemo by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }

    val data = remember(entries, demoMode) {
        entries.filter { MoodAnalytics.isDemo(it) == demoMode }
    }
    val analysis by produceState<MoodAnalytics.Snapshot?>(
        initialValue = null, key1 = data, key2 = days
    ) {
        value = withContext(Dispatchers.Default) { MoodAnalytics.build(data, days) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Descubre tus patrones", fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Bold)
        SegmentSelector(listOf("Estadísticas", "Actividades", "Insights"), view) { view = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Periodo", color = Ink, fontWeight = FontWeight.Medium)
            listOf(7, 30, 90).forEach { duration ->
                FilterChip(selected = days == duration, onClick = { days = duration }, label = { Text("$duration d") })
            }
        }
        SegmentSelector(listOf("Reales", "Demostración"), if (demoMode) "Demostración" else "Reales") {
            demoMode = it == "Demostración"
        }
        if (demoMode) {
            Text("Datos simulados · no representan experiencias personales.", fontSize = 12.sp, color = PastelMuted)
            if (data.isEmpty()) Button(onClick = { confirmDemo = true }) { Text("Generar 90 días de prueba") }
        }
        when {
            analysis == null -> Text("Calculando estadísticas…", color = PastelMuted)
            else -> {
                val result = analysis!!
                val stats = result.rangeStats
                if (result.selfHarmSignal || result.lowMoodStreak) {
                    SectionCard {
                        Text("Un espacio de apoyo 💜", fontWeight = FontWeight.Bold, color = Ink)
                        Text("Algunos registros pueden reflejar momentos difíciles. Esto no es un diagnóstico y la detección puede equivocarse. Por ahora pausamos los patrones y la estimación. Si te sirve, habla con alguien de confianza o busca apoyo profesional. Si hay peligro inmediato, llama al 911 (México) o al número local de emergencias.", color = Ink, fontSize = 13.sp)
                    }
                } else {
                when (view) {
                    "Estadísticas" -> TrendCover(result)
                    "Actividades" -> ActivityRing(result)
                    "Insights" -> InsightCover(result.insights.size)
                }
                if (view == "Estadísticas") SectionCard {
                    Text("Tu ritmo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ValueTile("Días registrados", "${stats.distinctDays}", Modifier.weight(1f))
                        ValueTile("Días seguidos", "${stats.currentStreak}", Modifier.weight(1f))
                    }
                    Text(if (stats.n == 0) "Empieza con cómo te sientes hoy. Cada registro te ayuda a conocerte."
                         else "Un momento para ti, a tu ritmo. No necesitas registrar un día perfecto.", color = PastelMuted, fontSize = 13.sp)
                    EverydayDetails("Ver estadísticas detalladas") {
                    Text("Resumen de $days días", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Muestra: n=${stats.n} registros de ${stats.distinctDays} días distintos", fontSize = 12.sp, color = PastelMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ValueTile("Promedio", format(stats.mean), Modifier.weight(1f))
                        ValueTile("Mediana", format(stats.median), Modifier.weight(1f))
                        ValueTile("Desviación", format(stats.sampleStdDev), Modifier.weight(1f))
                    }
                    Text("Moda: ${if (stats.modes.isEmpty()) "Sin moda repetida" else stats.modes.joinToString()} / 20", fontSize = 13.sp, color = Ink)
                    Text("Racha actual: ${stats.currentStreak} días · Racha máxima: ${stats.longestStreak} días", fontSize = 13.sp, color = Ink)
                    Text("Energía promedio: ${format(stats.energyMean)} / 10", fontSize = 13.sp, color = Ink)
                    HorizontalDivider(color = PastelLavender)
                    StatSummary("Esta semana (lunes a hoy)", result.weekStats)
                    StatSummary("Este mes (día 1 a hoy)", result.monthStats)
                    Text("Desviación estándar muestral. La racha actual incluye ayer si todavía no registraste hoy.", fontSize = 11.sp, color = PastelMuted)
                    }
                }

                if (view == "Actividades") SectionCard {
                    Text("¿Qué actividades acompañaron mis emociones?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Text("Gráfica de barras · índice emocional medio / 20", fontSize = 11.sp, color = PastelMuted)
                    if (result.rangeStats.distinctDays < MoodAnalytics.MIN_DAYS) {
                        Insufficient("Se requieren 14 días distintos con registros para analizar actividades.")
                    } else {
                        val usable = result.activities.filter { it.n >= MoodAnalytics.MIN_ACTIVITY_N }
                        if (usable.isEmpty()) Insufficient("Cada actividad necesita al menos 5 registros.")
                        else usable.take(8).forEach { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${item.name} · n=${item.n} · ${format(item.meanMood)}/20", color = Ink, fontSize = 12.sp)
                                Row(modifier = Modifier.fillMaxWidth().height(13.dp).background(PastelLavender, RoundedCornerShape(8.dp))) {
                                    Box(Modifier.fillMaxWidth(((item.meanMood ?: 0.0) / 20.0).toFloat().coerceIn(0f, 1f)).height(13.dp).background(Indigo, RoundedCornerShape(8.dp)))
                                }
                            }
                        }
                    }
                }

                if (view == "Actividades") EverydayDetails("Explorar las comparaciones") {
                SectionCard {
                    Text("¿Cómo se relacionan energía y ánimo?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Text("Gráfica de dispersión · n=${stats.n} · X: energía (1–10), Y: emoción (1–20)", fontSize = 11.sp, color = PastelMuted)
                    ScatterChart(result.scatter)
                    Text("Cada punto representa un registro; puntos iguales pueden superponerse.", fontSize = 11.sp, color = PastelMuted)
                }

                SectionCard {
                    Text("Actividad y estado de ánimo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Text("Método: diferencia de medias entre registros con y sin cada actividad; d de Cohen como tamaño de efecto. No se calculan valores p ni se afirma significación.", fontSize = 12.sp, color = PastelMuted)
                    if (result.activities.isEmpty()) {
                        Insufficient("Aún no hay datos suficientes: se requieren 14 días distintos.")
                    } else result.activities.take(12).forEach { activity ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${activity.name} · n=${activity.n} con actividad, n=${activity.otherN} sin ella", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 13.sp)
                            if (!activity.adequate) {
                                Insufficient("Aún no hay datos suficientes para comparar esta actividad (mínimo 5 en cada grupo).")
                            } else {
                                Text("Ánimo ${format(activity.meanMood)}/20 · Energía ${format(activity.meanEnergy)}/10", fontSize = 12.sp, color = Ink)
                                Text("Vs. media general: ${signed(activity.generalDifference)} · Vs. días sin actividad: ${signed(activity.contrastDifference)}", fontSize = 12.sp, color = Ink)
                                Text("Tamaño de efecto: d=${format(activity.cohenD)} (${activity.effectLabel})", fontSize = 12.sp, color = PastelMuted)
                            }
                            HorizontalDivider(color = PastelLavender)
                        }
                    }
                    if (result.activities.size > 12) Text("Se muestran las 12 actividades más frecuentes de ${result.activities.size}.", fontSize = 11.sp, color = PastelMuted)
                    if (result.activities.size > 1) Text("Se exploran múltiples actividades; algunas diferencias pueden aparecer por azar. Estos resultados son descriptivos, no confirmatorios.", fontSize = 11.sp, color = PastelMuted)
                }

                }

                if (view == "Insights") SectionCard {
                    Text("Lo que cuentan tus registros", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    if (result.insights.isEmpty()) {
                        Insufficient("Aún no hay datos suficientes o no se observa una diferencia de tamaño relevante con estos criterios.")
                    } else result.insights.forEach { insight ->
                        var expanded by remember(insight.activityName, days, demoMode) { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(insight.text, fontSize = 13.sp, color = Ink)
                            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar explicación" else "¿Por qué veo esto?") }
                            AnimatedVisibility(expanded) { Text(insight.explanation, fontSize = 12.sp, color = PastelMuted) }
                            HorizontalDivider(color = PastelLavender)
                        }
                    }
                }
                if (view == "Insights" && !result.selfHarmSignal && !result.lowMoodStreak) SectionCard {
                    val forecast = result.forecast
                    Text("Una mirada a mañana", fontWeight = FontWeight.Bold, color = Ink)
                    when {
                        forecast.pairs < com.example.moodwave.analytics.NextDayForecast.MIN_PAIRS -> Insufficient("Se necesitan 14 pares de días consecutivos para estimar.")
                        forecast.estimate == null -> Text("Registra el día de hoy para obtener la estimación de mañana.")
                        else -> Text("Índice estimado: ${format(forecast.estimate)} / 20. Es una aproximación.", color = Ink)
                    }
                    EverydayDetails("¿Cómo se calcula esta estimación?") {
                    Text("Se buscan cinco días parecidos por ánimo, energía y actividades y se promedia el índice registrado al día siguiente.", fontSize = 12.sp, color = Ink)
                    Text("Periodo seleccionado: ${forecast.pairs} pares de días consecutivos.", fontSize = 12.sp, color = PastelMuted)
                    if (forecast.evaluated > 0) {
                        Text("Error absoluto medio en ${forecast.evaluated} días evaluados: modelo ${format(forecast.modelMae)} puntos; repetir el último ánimo ${format(forecast.baselineMae)} puntos. Un error menor es mejor.", fontSize = 12.sp, color = Ink)
                    } else Text("Todavía faltan días posteriores al entrenamiento para medir el error.", fontSize = 12.sp, color = PastelMuted)
                    }
                }
                }
            }
        }

        Text("MoodWave no realiza diagnósticos médicos ni sustituye atención profesional.", fontSize = 12.sp, color = PastelMuted)
        OutlinedButton(onClick = { manage = !manage }, modifier = Modifier.fillMaxWidth()) {
            Text(if (manage) "Cerrar herramientas" else "Reportes, respaldo y datos de prueba")
        }
        if (manage) SectionCard {
            Text("Pruebas, exportación y control", fontWeight = FontWeight.Bold, color = Ink)
            Text("Los registros DEMO se guardan separados lógicamente mediante un marcador; puedes eliminarlos sin borrar los reales.", fontSize = 12.sp, color = PastelMuted)
            Button(onClick = { confirmDemo = true }, modifier = Modifier.fillMaxWidth()) { Text("Generar 90 días de prueba") }
            OutlinedButton(onClick = { confirmClearDemo = true }, modifier = Modifier.fillMaxWidth()) { Text("Borrar solo datos DEMO") }
            MonthlyReportButton(entries, demoMode)
            BackupButtons(entries)
            OutlinedButton(onClick = { confirmErase = true }, modifier = Modifier.fillMaxWidth()) { Text("Borrar TODOS mis registros") }
        }
    }

    if (confirmDemo) ConfirmDialog(
        "Generar registros de demostración",
        "Se agregarán 90 registros ficticios, sin eliminar los personales. Se reemplazarán los DEMO anteriores.",
        onDismiss = { confirmDemo = false }, onConfirm = {
            demoMode = true
            onGenerateDemo()
            confirmDemo = false
        }
    )
    if (confirmClearDemo) ConfirmDialog(
        "Eliminar datos de prueba", "Solo se eliminarán registros marcados como DEMO.",
        onDismiss = { confirmClearDemo = false }, onConfirm = { onClearDemo(); confirmClearDemo = false }
    )
    if (confirmErase) ConfirmDialog(
        "Borrar todos los registros",
        "Esta acción es irreversible: eliminará tanto los registros personales como los datos DEMO. Se desconectará la nube; las copias remotas y los archivos exportados se conservan. Puedes eliminarlos desde Respaldo en la nube o desde su destino. Exporta antes si necesitas conservarlos.",
        onDismiss = { confirmErase = false }, onConfirm = { onEraseAll(); confirmErase = false }
    )
}

@Composable
private fun ConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PastelCard)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun ValueTile(label: String, number: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(PastelLavender, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label, fontSize = 11.sp, color = LightPanelInk)
        Text(number, fontSize = 18.sp, color = LightPanelInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatSummary(title: String, value: MoodAnalytics.Stats) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
        Text("n=${value.n} · ${value.distinctDays} días · promedio ${format(value.mean)} · mediana ${format(value.median)}", fontSize = 12.sp, color = Ink)
        Text("Moda ${if (value.modes.isEmpty()) "—" else value.modes.joinToString()} · DE ${format(value.sampleStdDev)} · racha máxima ${value.longestStreak}", fontSize = 12.sp, color = PastelMuted)
    }
}

@Composable
private fun Insufficient(message: String) {
    Text(message, color = PastelMuted, fontSize = 12.sp)
}

private fun format(value: Double?): String =
    value?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—"

private fun signed(value: Double?): String =
    value?.let { String.format(Locale.getDefault(), "%+.2f", it) } ?: "—"

@Composable
private fun LineTrendChart(points: List<MoodAnalytics.DayPoint>, light: Boolean = false) {
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val left = 12f
        val right = size.width - 10f
        val top = 12f
        val bottom = size.height - 12f
        val height = bottom - top
        val width = right - left
        fun x(i: Int) = left + (if (points.size <= 1) 0.5f else i.toFloat() / (points.size - 1)) * width
        fun y(score: Double) = bottom - ((score.toFloat() - 1f) / 19f).coerceIn(0f, 1f) * height
        for (line in 0..4) {
            val yy = top + height * line / 4
            drawLine(if (light) Color.White.copy(alpha = .18f) else PastelLavender, Offset(left, yy), Offset(right, yy), strokeWidth = 2f)
        }
        var previous: Offset? = null
        points.forEachIndexed { i, point ->
            val current = point.mean?.let { Offset(x(i), y(it)) }
            if (previous != null && current != null) drawLine(if (light) Color.White else Indigo, previous!!, current, 3f, cap = StrokeCap.Round)
            if (current != null) drawCircle(if (light) Color.White else Indigo, radius = if (points.size > 30) 2f else 4f, center = current)
            previous = current
        }
        previous = null
        points.forEachIndexed { i, point ->
            val current = point.movingAverage?.let { Offset(x(i), y(it)) }
            if (previous != null && current != null) drawLine(if (light) Color(0xFFAFF3E4) else Mint, previous!!, current, 3f, cap = StrokeCap.Round)
            previous = current
        }
    }
    if (points.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(points.first().day.toString(), fontSize = 10.sp, color = if (light) Color.White else PastelMuted)
            Text(points.last().day.toString(), fontSize = 10.sp, color = if (light) Color.White else PastelMuted)
        }
    }
}

@Composable
private fun ScatterChart(points: List<Pair<Int, Int>>) {
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val left = 15f
        val top = 12f
        val width = size.width - 30f
        val height = size.height - 30f
        for (step in 0..4) {
            val yy = top + height * step / 4
            drawLine(PastelLavender, Offset(left, yy), Offset(left + width, yy), 2f)
        }
        points.forEach { (energy, mood) ->
            val x = left + ((energy - 1) / 9f).coerceIn(0f, 1f) * width
            val y = top + height - ((mood - 1) / 19f).coerceIn(0f, 1f) * height
            drawCircle(Indigo.copy(alpha = 0.35f), 6f, Offset(x, y))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Energía 1", fontSize = 10.sp, color = PastelMuted)
        Text("Energía 10", fontSize = 10.sp, color = PastelMuted)
    }
}


@Composable
private fun SegmentSelector(labels: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFEAE7FB), RoundedCornerShape(24.dp)).padding(3.dp)) {
        labels.forEach { label ->
            val active = label == selected
            Box(Modifier.weight(1f).background(if (active) Indigo else Color.Transparent, RoundedCornerShape(22.dp))
                .selectable(selected = active, role = Role.Tab, onClick = { onSelect(label) }).padding(vertical = 11.dp),
                contentAlignment = Alignment.Center) {
                Text(label, color = if (active) Color.White else LightPanelInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TrendCover(result: MoodAnalytics.Snapshot) {
    Column(Modifier.fillMaxWidth().background(
        Brush.verticalGradient(listOf(Color(0xFF7569BD), Color(0xFF50449C))), RoundedCornerShape(26.dp)
    ).padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("TENDENCIA   /   ${result.rangeDays} DÍAS", color = Color.White, fontSize = 12.sp)
        Text(format(result.rangeStats.mean), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Ánimo promedio · índice de 1 a 20", color = Color.White)
        Text("${result.rangeStats.n} registros · ${result.rangeStats.distinctDays} días observados", color = Color.White, fontSize = 12.sp)
        if (result.rangeStats.n > 0) {
            LineTrendChart(result.points, light = true)
            Text("Blanco: cómo te sentiste · Menta: tu tendencia semanal", color = Color.White, fontSize = 12.sp)
        } else Text("La curva aparecerá con tu primer registro.", color = Color.White)
    }
}

@Composable
private fun ActivityRing(result: MoodAnalytics.Snapshot) {
    val counts = result.activities.take(3).map { it.name to it.n }.toMutableList()
    val others = result.activities.drop(3).sumOf { it.n }
    if (others > 0) counts.add("Otras" to others)
    val total = counts.sumOf { it.second }
    val colors = listOf(Color(0xFF7155DE), Color(0xFFEA7D62), Color(0xFFDEBE4A), Color(0xFF53B798))
    SectionCard {
        Text("Actividades y patrones", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink)
        Text("¿Qué actividades aparecen más en mis registros?", color = PastelMuted, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(172.dp).padding(14.dp)) {
                val stroke = Stroke(width = 24.dp.toPx())
                drawCircle(PastelLavender, style = stroke)
                var start = -90f
                counts.forEachIndexed { i, item ->
                    val sweep = 360f * item.second / total
                    drawArc(colors[i], start, sweep, false, style = stroke)
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (result.rangeStats.distinctDays < MoodAnalytics.MIN_DAYS) "—" else total.toString(),
                    fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("menciones", fontSize = 12.sp, color = PastelMuted)
            }
        }
        counts.forEachIndexed { i, (name, n) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(colors[i], RoundedCornerShape(50)))
                Text(name, Modifier.weight(1f), color = Ink)
                Text("$n", color = Ink, fontWeight = FontWeight.Bold)
            }
        }
        Text(if (result.rangeStats.distinctDays < MoodAnalytics.MIN_DAYS) "Se necesitan 14 días registrados para mostrar la distribución."
            else "Un registro puede mencionar varias actividades.", color = PastelMuted, fontSize = 12.sp)
    }
}

@Composable
private fun InsightCover(count: Int) {
    Column(Modifier.fillMaxWidth().background(
        Brush.linearGradient(listOf(Color(0xFFE1F3EB), Color(0xFFF6E4ED), Color(0xFFE8E4F9))),
        RoundedCornerShape(26.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(60.dp).background(Brush.verticalGradient(listOf(Color(0xFF9387E9),
            Color(0xFFDEB7E0), Color(0xFFF3B5A4))), RoundedCornerShape(50)))
        Text("Tus descubrimientos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LightPanelInk)
        Text(if (count == 0) "Vamos conociendo tu ritmo" else "$count relaciones para explorar", fontSize = 13.sp, color = LightPanelInk)
    }
}


@Composable
private fun EverydayDetails(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar detalles" else title) }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
