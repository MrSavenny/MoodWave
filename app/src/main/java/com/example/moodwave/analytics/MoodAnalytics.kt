package com.example.moodwave.analytics

import com.example.moodwave.model.MoodEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.sqrt

/** Motor estadístico puro: no depende de componentes de Android. */
object MoodAnalytics {
    const val MIN_DAYS = 14
    const val MIN_ACTIVITY_N = 5
    const val DEMO_PREFIX = "[DATOS_DEMO_MOODWAVE]"

    // moodLevel es un ÍNDICE ORDINAL de 20 categorías ordenadas por la aplicación,
    // NO una escala clínica ni una medida de felicidad científicamente validada.
    data class Stats(
        val n: Int,
        val distinctDays: Int,
        val mean: Double?,
        val median: Double?,
        val modes: List<Int>,
        val sampleStdDev: Double?,
        val currentStreak: Int,
        val longestStreak: Int,
        val energyMean: Double?
    )

    data class DayPoint(val day: LocalDate, val mean: Double?, val movingAverage: Double?)

    data class ActivityResult(
        val name: String,
        val n: Int,
        val otherN: Int,
        val meanMood: Double?,
        val meanEnergy: Double?,
        val generalDifference: Double?,
        val otherMeanMood: Double?,
        val contrastDifference: Double?,
        val cohenD: Double?,
        val daysWithActivity: List<LocalDate>
    ) {
        val adequate: Boolean get() = n >= MIN_ACTIVITY_N && otherN >= MIN_ACTIVITY_N
        val effectLabel: String get() = when {
            cohenD == null -> "No estimable"
            abs(cohenD) < 0.2 -> "Muy pequeño"
            abs(cohenD) < 0.5 -> "Pequeño"
            abs(cohenD) < 0.8 -> "Moderado"
            else -> "Grande"
        }
    }

    data class Insight(
        val text: String,
        val explanation: String,
        val activityName: String,
        val nWith: Int,
        val nWithout: Int
    )

    data class Snapshot(
        val rangeDays: Int,
        val rangeStats: Stats,
        val weekStats: Stats,
        val monthStats: Stats,
        val points: List<DayPoint>,
        val activities: List<ActivityResult>,
        val insights: List<Insight>,
        val scatter: List<Pair<Int, Int>>,
        val lowMoodStreak: Boolean,
        val forecast: NextDayForecast.Result,
        val selfHarmSignal: Boolean
    )

    fun isDemo(entry: MoodEntry): Boolean = entry.diaryText.startsWith(DEMO_PREFIX)

    fun inPeriod(entries: List<MoodEntry>, from: LocalDate, to: LocalDate): List<MoodEntry> =
        entries.filter { entry ->
            val d = runCatching { LocalDate.parse(entry.date) }.getOrNull()
            d != null && !d.isBefore(from) && !d.isAfter(to)
        }

    fun stats(entries: List<MoodEntry>, today: LocalDate = LocalDate.now()): Stats {
        val scores = entries.map { it.moodLevel.toDouble() }.sorted()
        val dates = entries.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .filter { !it.isAfter(today) }.toSet()
        val n = scores.size
        val mean = if (n == 0) null else scores.average()
        val median = if (n == 0) null else if (n % 2 == 1) scores[n / 2]
            else (scores[n / 2 - 1] + scores[n / 2]) / 2.0
        val frequency = entries.groupingBy { it.moodLevel }.eachCount()
        val maxFrequency = frequency.values.maxOrNull() ?: 0
        val modes = if (maxFrequency < 2) emptyList() else frequency
            .filterValues { it == maxFrequency }.keys.sorted()
        val std = if (n < 2 || mean == null) null else sqrt(
            scores.sumOf { (it - mean) * (it - mean) } / (n - 1)
        )
        val longest = longestStreak(dates)
        val anchor = when {
            today in dates -> today
            today.minusDays(1) in dates -> today.minusDays(1)
            else -> null
        }
        var current = 0
        var date = anchor
        while (date != null && date in dates) {
            current++
            date = date.minusDays(1)
        }
        return Stats(n, dates.size, mean, median, modes, std, current, longest,
            entries.map { it.energyLevel }.takeIf { it.isNotEmpty() }?.average())
    }

    private fun longestStreak(dates: Set<LocalDate>): Int {
        var best = 0
        var current = 0
        var previous: LocalDate? = null
        for (date in dates.sorted()) {
            current = if (previous != null && date == previous.plusDays(1)) current + 1 else 1
            if (current > best) best = current
            previous = date
        }
        return best
    }

    fun trend(entries: List<MoodEntry>, days: Int, today: LocalDate = LocalDate.now()): List<DayPoint> {
        require(days in 1..365)
        val byDay = inPeriod(entries, today.minusDays(days.toLong() + 5), today)
            .groupBy { LocalDate.parse(it.date) }.mapValues { (_, list) -> list.map { it.moodLevel }.average() }
        val allDates = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
        return allDates.map { day ->
            val rolling = (0..6).mapNotNull { offset -> byDay[day.minusDays(offset.toLong())] }
            DayPoint(day, byDay[day], if (rolling.size >= 3) rolling.average() else null)
        }
    }

    fun activityResults(entries: List<MoodEntry>): List<ActivityResult> {
        val general = entries.map { it.moodLevel }.takeIf { it.isNotEmpty() }?.average()
        val groups = mutableMapOf<String, MutableList<MoodEntry>>()
        entries.forEach { row ->
            row.activities.split(',').map { it.trim() }.filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }.forEach { name ->
                    val existing = groups.keys.firstOrNull { it.equals(name, ignoreCase = true) } ?: name
                    groups.getOrPut(existing) { mutableListOf() }.add(row)
                }
        }
        return groups.map { (name, with) ->
            val withSet = with.toSet()
            val without = entries.filter { it !in withSet }
            val mean = with.map { it.moodLevel.toDouble() }.average()
            val energy = with.map { it.energyLevel.toDouble() }.average()
            val otherMean = without.takeIf { it.isNotEmpty() }?.map { it.moodLevel.toDouble() }?.average()
            val diff = otherMean?.let { mean - it }
            val d = if (with.size >= MIN_ACTIVITY_N && without.size >= MIN_ACTIVITY_N) {
                cohenD(with.map { it.moodLevel.toDouble() }, without.map { it.moodLevel.toDouble() })
            } else null
            ActivityResult(
                name = name, n = with.size, otherN = without.size,
                meanMood = mean, meanEnergy = energy,
                generalDifference = general?.let { mean - it },
                otherMeanMood = otherMean, contrastDifference = diff, cohenD = d,
                daysWithActivity = with.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.distinct().sorted()
            )
        }.sortedWith(compareByDescending<ActivityResult> { it.n }.thenBy { it.name })
    }

    private fun cohenD(a: List<Double>, b: List<Double>): Double? {
        if (a.size < 2 || b.size < 2) return null
        val meanA = a.average()
        val meanB = b.average()
        val sumSq = a.sumOf { (it - meanA) * (it - meanA) } +
            b.sumOf { (it - meanB) * (it - meanB) }
        val pooled = sqrt(sumSq / (a.size + b.size - 2))
        return if (pooled > 0.0000001) (meanA - meanB) / pooled else null
    }

    fun build(entries: List<MoodEntry>, rangeDays: Int, today: LocalDate = LocalDate.now()): Snapshot {
        require(rangeDays in listOf(7, 30, 90))
        val range = inPeriod(entries, today.minusDays(rangeDays.toLong() - 1), today)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val monthStart = today.withDayOfMonth(1)
        val activities = if (range.map { it.date }.distinct().size >= MIN_DAYS) activityResults(range)
            else emptyList()
        val insights = activities.filter { activity ->
            activity.adequate && activity.cohenD != null &&
                abs(activity.cohenD) >= 0.5 && abs(activity.contrastDifference ?: 0.0) >= 1.0
        }.take(3).map { activity ->
            val delta = activity.contrastDifference!!
            val direction = if (delta > 0) "mayor" else "menor"
            val difference = String.format(java.util.Locale.US, "%.1f", abs(delta))
            Insight(
                text = "Se observa una relación exploratoria: al registrar ${activity.name}, " +
                    "el índice emocional promedio fue $difference puntos $direction que en registros sin esa actividad " +
                    "(n=${activity.n} frente a n=${activity.otherN}). Esto no demuestra causalidad.",
                explanation = "Método: diferencia de medias entre registros con y sin la actividad. " +
                    "Promedios ${format(activity.meanMood)} y ${format(activity.otherMeanMood)} sobre 20; " +
                    "efecto d de Cohen ${format(activity.cohenD)} (${activity.effectLabel}). " +
                    "Fechas con actividad: ${activity.daysWithActivity.joinToString()}. " +
                    "Comparación exploratoria, sin pruebas de significación ni ajuste por factores de confusión.",
                activityName = activity.name, nWith = activity.n, nWithout = activity.otherN
            )
        }
        return Snapshot(
            rangeDays, stats(range, today),
            stats(inPeriod(entries, weekStart, today), today),
            stats(inPeriod(entries, monthStart, today), today),
            trend(entries, rangeDays, today), activities, insights,
            range.map { it.energyLevel to it.moodLevel },
            hasSustainedLowMood(inPeriod(entries, today.minusDays(13), today), today),
            NextDayForecast.evaluate(range, today),
            range.any { indicatesSelfHarm(it.diaryText) && !isDemo(it) }
        )
    }

    private fun format(value: Double?): String = value?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "n/d"

    private fun hasSustainedLowMood(entries: List<MoodEntry>, today: LocalDate): Boolean {
        val lowDays = entries.groupBy { it.date }.mapNotNull { (date, rows) ->
            runCatching { LocalDate.parse(date) }.getOrNull()?.takeIf { !it.isAfter(today) && rows.map { e -> e.moodLevel }.average() <= 5.0 }
        }.toSet()
        return lowDays.any { day -> (0L..4L).all { day.minusDays(it) in lowDays } }
    }

    /** Heurística local simple. No diagnostica; las negaciones y el sarcasmo pueden fallar. */
    fun indicatesSelfHarm(text: String): Boolean {
        val normalized = java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        val riskPhrases = listOf(
            "quiero suicidarme", "me quiero suicidar", "voy a suicidarme", "voy a suicidarme hoy",
            "quitarme la vida", "quiero quitarme la vida", "no quiero seguir viviendo",
            "no quiero vivir", "ya no quiero vivir", "quiero morir", "me quiero morir",
            "hacerme dano", "quiero hacerme dano", "voy a lastimarme", "quiero lastimarme",
            "voy a cortarme", "quiero cortarme", "autolesionarme"
        )
        return riskPhrases.any { normalized.contains(it) }
    }
}


