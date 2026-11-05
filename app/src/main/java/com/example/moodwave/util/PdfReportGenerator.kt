package com.example.moodwave.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.moodwave.analytics.MoodAnalytics
import com.example.moodwave.model.MoodEntry
import java.io.OutputStream
import java.time.YearMonth
import java.util.Locale

object PdfReportGenerator {
    fun write(out: OutputStream, entries: List<MoodEntry>, month: YearMonth, demo: Boolean) {
        val rows = MoodAnalytics.inPeriod(entries, month.atDay(1), month.atEndOfMonth())
        val summary = MoodAnalytics.stats(rows, month.atEndOfMonth())
        val points = MoodAnalytics.trend(rows, month.lengthOfMonth(), month.atEndOfMonth())
        val analysis = MoodAnalytics.build(rows, 90, month.atEndOfMonth())
        val ink = Color.rgb(40, 46, 70)
        val violet = Color.rgb(78, 69, 156)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun number(value: Double?) = value?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—"
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            fun text(s: String, y: Float, size: Float = 13f) {
                paint.color = ink
                paint.textSize = size
                canvas.drawText(s, 44f, y, paint)
            }
            text("MoodWave   /   Reporte mensual", 65f, 26f)
            text("$month   ·   " + if (demo) "Datos de demostración" else "Registros personales", 95f)
            text("Muestra: ${summary.n} registros en ${summary.distinctDays} de ${month.lengthOfMonth()} días", 145f)
            text("Índice emocional medio: ${number(summary.mean)} / 20     Energía media: ${number(summary.energyMean)} / 10", 172f)
            text("Origen: " + if (demo) "solo registros de demostración" else "solo registros personales", 197f, 11f)
            text("Índice emocional diario (1–20)", 230f, 17f)
            fun x(i: Int) = 59f + i * 478f / (points.size - 1)
            fun y(v: Double) = 390f - (v.toFloat() - 1f) * 140f / 19f
            paint.color = Color.LTGRAY
            canvas.drawLine(x(0), 390f, x(points.lastIndex), 390f, paint)
            paint.color = violet
            var previous: Double? = null
            points.forEachIndexed { i, point ->
                point.mean?.let { mean ->
                    previous?.let { canvas.drawLine(x(i-1), y(it), x(i), y(mean), paint) }
                    canvas.drawCircle(x(i), y(mean), 3f, paint)
                }
                previous = point.mean
            }
            text("Día 1", 412f)
            paint.textSize = 13f
            canvas.drawText("Día ${points.size}", 510f, 412f, paint)
            text("Cada punto promedia los registros del día. Los huecos indican días sin registro.", 440f, 12f)
            text("Actividades más frecuentes · índice medio / 20", 495f, 17f)
            val bars = analysis.activities.filter { it.n >= MoodAnalytics.MIN_ACTIVITY_N }.take(3)
            if (bars.isEmpty()) {
                val reason = when {
                    summary.distinctDays < MoodAnalytics.MIN_DAYS -> "Aún no hay suficientes datos: se requieren 14 días con registros."
                    else -> "No hay una actividad con al menos 5 registros en cada grupo este mes."
                }
                text(reason, 532f, 12f)
            }
            bars.forEachIndexed { i, activity ->
                val yy = 530f + 32f * i
                text("${activity.name.take(22)} (n=${activity.n})", yy, 12f)
                paint.color = violet
                val mean = activity.meanMood ?: 0.0
                canvas.drawRect(265f, yy-10, 265f + 10f * mean.toFloat(), yy, paint)
                paint.color = ink
                canvas.drawText(number(mean), 485f, yy, paint)
            }
            text("Conclusión exploratoria", 666f, 17f)
            val insight = analysis.insights.firstOrNull()
            val message = when {
                analysis.selfHarmSignal || analysis.lowMoodStreak -> "Si estás pasando por un momento difícil, considera hablar con alguien de confianza o buscar apoyo profesional."
                insight != null -> insight.text
                summary.distinctDays < MoodAnalytics.MIN_DAYS -> "Aún no hay datos suficientes. Se requieren al menos 14 días con registros."
                analysis.activities.none { it.adequate } -> "Aún no hay suficientes datos por actividad: se requieren al menos 5 registros con y 5 sin cada actividad."
                else -> "Hay datos para comparar, pero ninguna diferencia alcanzó el umbral de tamaño de efecto definido. Las comparaciones son exploratorias."
            }
            paint.textSize = 13f
            var line = ""
            var height = 697f
            for (word in message.split(' ')) {
                if (paint.measureText("$line $word") > 505f && line.isNotEmpty()) {
                    text(line, height)
                    height += 18f
                    line = ""
                }
                line = if (line.isEmpty()) word.take(40) else "$line ${word.take(40)}"
            }
            text(line, height)
            text("MoodWave no realiza diagnósticos médicos ni sustituye atención profesional.", 810f, 11f)
            doc.finishPage(page)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }
}


