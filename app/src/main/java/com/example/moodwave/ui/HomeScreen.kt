package com.example.moodwave.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.data.PreferencesHelper
import com.example.moodwave.model.MoodEntry
import com.example.moodwave.ui.components.FacebookStyleEmoji
import com.example.moodwave.ui.theme.*

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val JournalAccent @Composable get() = if (AppThemeState.isDarkMode) Color(0xFFC7BEFF) else PatternPurple
private val JournalSelected @Composable get() = if (AppThemeState.isDarkMode) Color(0xFF37324F) else PastelLavender

data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun DisintegratingCard(
    isDeleting: Boolean,
    onDeleteAnimationEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            val animation = Animatable(0f)
            animation.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing)
            ) {
                progress = this.value
            }
            onDeleteAnimationEnd()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    alpha = if (isDeleting) (1f - progress).coerceIn(0f, 1f) else 1f,
                    scaleX = if (isDeleting) (1f - progress * 0.25f).coerceIn(0f, 1f) else 1f,
                    scaleY = if (isDeleting) (1f - progress * 0.25f).coerceIn(0f, 1f) else 1f
                )
        ) {
            content()
        }

        if (isDeleting) {
            val particles = remember {
                List(40) {
                    val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
                    val speed = Random.nextFloat() * 260f + 80f
                    val size = Random.nextFloat() * 12f + 6f
                    val color = listOf(
                        Color(0xFF818CF8), Color(0xFFFB7185), Color(0xFF34D399),
                        Color(0xFFFBBF24), Color(0xFFA78BFA), Color(0xFF38BDF8)
                    ).random()
                    Particle(angle, speed, size, color)
                }
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                particles.forEach { p ->
                    val dist = p.speed * progress
                    val x = center.x + cos(p.angle) * dist
                    val y = center.y + sin(p.angle) * dist
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    val particleSize = p.size * (1f - progress * 0.4f)

                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = particleSize,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MoodViewModel,
    onNavigateToAdd: () -> Unit,
    onLockApp: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val moodList by viewModel.allMoods.collectAsState(initial = emptyList())
    var selectedTab by rememberSaveable { mutableStateOf("timeline") } // "timeline", "calendar", or "stats"

    var entryToDelete by remember { mutableStateOf<MoodEntry?>(null) }
    var deletingMoodId by remember { mutableStateOf<Int?>(null) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var dismissedRecommendationId by rememberSaveable { mutableStateOf<Int?>(null) }

    val userName = PreferencesHelper.getUserName(context)
    val profileAvatar = PreferencesHelper.getProfileImage(context).ifBlank { "🌸" }

    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 6..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    // Calculate weekly stats
    val today = LocalDate.now()
    val recentMoods = moodList.filter { entry ->
        try {
            val entryDate = LocalDate.parse(entry.date)
            entryDate.isAfter(today.minusDays(7)) && !entryDate.isAfter(today)
        } catch (_: Exception) {
            false
        }
    }

    val avgMood = if (recentMoods.isNotEmpty()) recentMoods.map { it.moodLevel }.average().toInt() else 10
    val avgEnergy = if (recentMoods.isNotEmpty()) recentMoods.map { it.energyLevel }.average() else 0.0

    Scaffold(
        containerColor = PastelBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AestheticFloatingBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        // Unified Scrollable LazyColumn for entire screen so landscape rotation works smoothly without cramping
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (selectedTab == "timeline") 0.dp else 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)
        ) {
            if (selectedTab == "timeline") item {
                Column(Modifier.padding(bottom = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(today.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-MX")))
                            .uppercase(Locale.forLanguageTag("es-MX")), Modifier.weight(1f), fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, color = PastelMuted)
                        IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Ajustes", tint = JournalAccent) }
                        IconButton(onClick = onLockApp) { Icon(Icons.Default.Lock, "Bloquear", tint = JournalAccent) }
                    }
                    Text("Mi diario", fontSize = 36.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.Bold, color = PastelText)
                    Text("$greeting, $userName", color = PastelMuted, fontSize = 15.sp)
                }
            } else item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PastelLavender),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileImageAvatar(
                                imageUriOrEmoji = profileAvatar,
                                modifier = Modifier.fillMaxSize(),
                                iconSize = 24.sp
                            )
                        }
                        Column {
                            Text(
                                text = "$greeting, $userName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PastelMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tu Bienestar",
                                style = MaterialTheme.typography.headlineLarge,
                                color = PastelText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PastelCard)
                                .clickable { onNavigateToSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PastelSky)
                                .clickable { onLockApp() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Bloquear",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Stats Cards Row
            if (selectedTab == "calendar") item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Weekly Mood Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PastelCard),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "ÁNIMO SEMANAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FacebookStyleEmoji(
                                    emoji = if (avgMood > 0) getMoodEmoji(avgMood) else "🌱",
                                    fontSize = 32.sp
                                )
                                Text(
                                    text = if (avgMood > 0) getMoodWord(avgMood) else "--",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelText,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    // Average Energy Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PastelCard),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "ENERGÍA PROMEDIO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Energía",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(32.dp).padding(bottom = 4.dp)
                                )
                                Text(
                                    text = if (avgEnergy > 0) String.format(Locale.getDefault(), "%.1f / 10", avgEnergy) else "--",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelText,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }



            // Tab Content
            when (selectedTab) {
                "timeline" -> {
                    item {
                        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(if (AppThemeState.isDarkMode) PastelCard else Color(0xFFF0ECFB), if (AppThemeState.isDarkMode) JournalSelected else Color(0xFFF9F6FF))), RoundedCornerShape(18.dp))
                            .padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("REGISTRO DE HOY", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = JournalAccent)
                            Text("¿Cómo te sientes hoy?", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = PastelText)
                            Button(onClick = onNavigateToAdd, modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = JournalAccent)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Registrar emoción", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (recentMoods.isNotEmpty()) item {
                        Text("Últimos 7 días", Modifier.padding(top = 20.dp, bottom = 8.dp), color = PastelText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Row(Modifier.fillMaxWidth().background(PastelCard, RoundedCornerShape(16.dp)).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Ánimo promedio", fontSize = 13.sp, color = PastelMuted)
                                Text(getMoodWord(avgMood), color = PastelText, fontWeight = FontWeight.SemiBold)
                            }
                            VerticalDivider(Modifier.height(36.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Energía promedio", fontSize = 13.sp, color = PastelMuted)
                                Text(String.format(Locale.getDefault(), "%.1f / 10", avgEnergy), color = PastelText, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    val latestEntry = moodList.maxWithOrNull(compareBy<MoodEntry> { it.date }.thenBy { it.id })
                    if (latestEntry != null && latestEntry.id != dismissedRecommendationId) item {
                        Column(Modifier.padding(top = 18.dp)) {
                            WellnessRecommendationCard(entry = latestEntry, onDismiss = { dismissedRecommendationId = latestEntry.id })
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(16.dp))
                            .background(PastelCard).clickable { historyOpen = !historyOpen }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Historial de emociones", Modifier.weight(1f), color = PastelText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(moodList.size.toString(), color = PastelMuted)
                            Icon(Icons.Default.ChevronRight, if (historyOpen) "Contraer historial" else "Desplegar historial",
                                Modifier.graphicsLayer { rotationZ = if (historyOpen) 90f else 0f }, tint = JournalAccent)
                        }
                    }
                    if (historyOpen) {
                    if (moodList.isEmpty()) {
                        item { Text("Aquí aparecerá tu primera entrada.", color = PastelMuted, fontSize = 14.sp) }
                    } else {
                        // Newest entries stay at the top (sorted by date descending, then id descending)
                        itemsIndexed(moodList.sortedWith(compareByDescending<MoodEntry> { it.date }.thenByDescending { it.id }), key = { _, entry -> entry.id }) { index, mood ->
                            val isDeletingThis = mood.id == deletingMoodId

                            DisintegratingCard(
                                isDeleting = isDeletingThis,
                                onDeleteAnimationEnd = {
                                    viewModel.deleteMood(mood)
                                    deletingMoodId = null
                                }
                            ) {
                                DiaryEntryRow(mood, index == 0, index == moodList.lastIndex, onDelete = { entryToDelete = mood })

                            }
                        }
                    }
                }
                }
                "calendar" -> {
                    item {
                        CalendarView(moodList = moodList)
                    }
                }
                "stats" -> {
                    item {
                        AnalyticsDashboard(
                            entries = moodList,
                            onGenerateDemo = { viewModel.generateDemoData() },
                            onClearDemo = { viewModel.clearDemoData() },
                            onEraseAll = { com.example.moodwave.sync.CloudScheduler.disable(context); viewModel.eraseAllData() }
                        )
                    }
                }
            }
        }
    }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Eliminar Registro", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar este registro del ${entryToDelete?.date}?") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetMood = entryToDelete
                        entryToDelete = null
                        targetMood?.let { deletingMoodId = it.id }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7185))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancelar", color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@Composable
private fun DiaryEntryRow(mood: MoodEntry, first: Boolean, last: Boolean, onDelete: () -> Unit) {
    var expanded by rememberSaveable(mood.id) { mutableStateOf(false) }
    val demo = com.example.moodwave.analytics.MoodAnalytics.isDemo(mood)
    val note = mood.diaryText.removePrefix(com.example.moodwave.analytics.MoodAnalytics.DEMO_PREFIX).trim()
    val angle by animateFloatAsState(if (expanded) 90f else 0f, tween(210), label = "Detalle del diario")
    val top = if (first) 16.dp else 0.dp
    val bottom = if (last) 16.dp else 0.dp
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom)).background(PastelCard).animateContentSize(tween(210)).padding(horizontal = 14.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "Ocultar detalle" else "Ver detalle") { expanded = !expanded }
            .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(getMoodColor(mood.moodLevel).copy(alpha = .25f), CircleShape),
                contentAlignment = Alignment.Center) { Text(getMoodEmoji(mood.moodLevel), fontSize = 22.sp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(getMoodWord(mood.moodLevel), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = PastelText)
                Text(listOf(mood.date, mood.time, if (demo) "Demo" else "").filter { it.isNotBlank() }.joinToString(" · "),
                    color = PastelMuted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, if (expanded) "Ocultar detalle" else "Ver detalle", Modifier.size(20.dp).graphicsLayer { rotationZ = angle }, tint = PastelMuted)
        }
        if (expanded) {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Intensidad ${mood.moodIntensity}/5 · Energía ${mood.energyLevel}/10", fontSize = 13.sp)
                if (note.isNotEmpty()) Text(note, fontSize = 14.sp)
                if (mood.activities.isNotBlank()) Text("Actividades: ${mood.activities}", fontSize = 13.sp, color = PastelMuted)
            }
        }
        if (expanded) TextButton(onClick = onDelete) { Text("Eliminar entrada", color = Color(0xFFBD2733)) }
        if (!last) HorizontalDivider(color = Color(0xFFE2E2E7))

    }
}

@Composable
fun AestheticFloatingBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf(Triple("timeline", "Inicio", Icons.Default.Home),
        Triple("calendar", "Calendario", Icons.Default.DateRange),
        Triple("stats", "Estadísticas", Icons.Default.BarChart))
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = PastelCard, shadowElevation = 5.dp,
            border = BorderStroke(1.dp, PastelMuted.copy(alpha = .16f))) {
            BoxWithConstraints(Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(5.dp)) {
                val width = maxWidth / tabs.size
                val position by animateDpAsState(width * tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
                    tween(240), label = "Pestaña activa")
                Box(Modifier.offset(x = position).width(width).height(50.dp).background(JournalSelected, CircleShape))
                Row {
                    tabs.forEach { (key, label, icon) ->
                        val selected = selectedTab == key
                        val tint by animateColorAsState(if (selected) JournalAccent else PastelMuted,
                            tween(180), label = "Color de pestaña")
                        Column(Modifier.width(width).heightIn(min = 50.dp).clip(CircleShape)
                            .selectable(selected, role = Role.Tab, onClick = { onTabSelected(key) }),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(icon, null, Modifier.size(20.dp), tint = tint)
                            Text(label, fontSize = 11.sp, color = tint,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WellnessRecommendationCard(
    entry: MoodEntry,
    onDismiss: () -> Unit
) {
    val level = entry.moodLevel
    val intensity = entry.moodIntensity

    val (title, text, bgColor) = when (level) {
        1, 2, 3 -> Triple(
            "🌱 Espacio de Cuidado y Empatía (Tristeza / Melancolía)",
            if (intensity >= 4)
                "Has registrado tristeza con intensidad alta ($intensity/5). Permítete sentir sin juzgarte, abrígate con una cobija cómoda y, si lo deseas, platica con alguien de confianza."
            else
                "Un toque de melancolía o tristeza es completamente natural. Tómate una pausa, escucha música suave o escribe lo que sientes.",
            Color(0xFFEEF2FF)
        )
        4, 7 -> Triple(
            "🫁 Respiración Descompresiva (Estrés / Ansiedad)",
            if (intensity >= 4)
                "Nivel alto de tensión detectado ($intensity/5). Inhala profundo 4 segundos, sostén 7 y exhala lentamente en 8 segundos. Repite 5 veces."
            else
                "Sientes ligera tensión. Da un paseo breve de 5 minutos, estira los hombros y respira aire fresco.",
            Color(0xFFFFF1F2)
        )
        5, 6 -> Triple(
            "🌊 Canalización del Enojo y Frustración",
            if (intensity >= 4)
                "Intensidad alta de frustración ($intensity/5). Aléjate unos minutos de la situación, bebe un vaso de agua fría y haz respiraciones profundas."
            else
                "Un momento de molestia. Respira despacio y concéntrate únicamente en lo que está bajo tu control.",
            Color(0xFFFFEDD5)
        )
        8 -> Triple(
            "😴 Descanso y Recuperación (Cansancio)",
            "Tu energía está baja hoy. Regálate una pausa sin pantallas, prepara una infusión caliente y descansa cuanto lo necesites.",
            Color(0xFFF1F5F9)
        )
        in 9..12 -> Triple(
            "🍃 Calma y Centramiento",
            "Tienes una energía estable y reflexiva. Es un momento ideal para disfrutar el presente y conectar con tu respiración.",
            Color(0xFFF0FDF4)
        )
        else -> Triple(
            "✨ Celebración y Alegría",
            "¡Qué hermosa vibración tienes hoy! Aprovecha este bienestar para agradecer 3 cosas bellas o compartir tu alegría.",
            Color(0xFFECFDF5)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = "Bienestar",
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = 17.sp
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Descartar consejo",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarView(moodList: List<MoodEntry>) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }

    val entriesByDate = remember(moodList) {
        moodList.groupBy { it.date }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = Color(0xFF6B7280))
                    }

                    Text(
                        text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es"))).replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937)
                    )

                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = Color(0xFF6B7280))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("D", "L", "M", "M", "J", "V", "S").forEach { day ->
                        Text(
                            text = day,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PastelMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val firstDayOfMonth = currentYearMonth.atDay(1)
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

                val totalCells = startDayOfWeek + daysInMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - startDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val dateStr = currentYearMonth.atDay(dayNumber).toString()
                                    val dayEntries = entriesByDate[dateStr] ?: emptyList()
                                    val hasEntry = dayEntries.isNotEmpty()
                                    val avgMoodLevel = if (hasEntry) dayEntries.map { it.moodLevel }.average().toInt().coerceIn(1, 20) else 10
                                    val isToday = dateStr == LocalDate.now().toString()
                                    val isSelectedDay = dateStr == selectedDate

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelectedDay -> Color(0xFFC7D2FE)
                                                    hasEntry -> getMoodColor(avgMoodLevel)
                                                    else -> Color(0xFFF9FAFB)
                                                }
                                            )
                                            .border(
                                                width = if (isSelectedDay) 2.dp else 0.dp,
                                                color = if (isSelectedDay) Color(0xFF4F46E5) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedDate = dateStr },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (hasEntry) getMoodEmoji(avgMoodLevel) else dayNumber.toString(),
                                            fontSize = if (hasEntry) 16.sp else 13.sp,
                                            fontWeight = if (isToday || isSelectedDay) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (hasEntry) Color.DarkGray else Color(0xFF9CA3AF)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Daily Statistics & Entries for selected calendar day
        if (selectedDate != null) {
            val dateStr = selectedDate!!
            val dayEntries = entriesByDate[dateStr] ?: emptyList()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PastelCard),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTADÍSTICAS DEL DÍA ($dateStr)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelMuted,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = { selectedDate = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                        }
                    }

                    if (dayEntries.isNotEmpty()) {
                        val dayAvgMoodLevel = dayEntries.map { it.moodLevel }.average().toInt().coerceIn(1, 20)
                        val dayAvgMoodExact = dayEntries.map { it.moodLevel }.average()
                        val dayAvgEnergy = dayEntries.map { it.energyLevel }.average()
                        val dayTotalEntries = dayEntries.size
                        val dayMostFrequentMood = dayEntries.groupBy { it.moodLevel }.maxByOrNull { it.value.size }?.key ?: dayAvgMoodLevel
                        val dayActivities = dayEntries.flatMap { it.activities.split(",") }
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()

                        // Daily Executive Summary Card
                        Surface(
                            color = Color(0xFFFFFDFB),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Ánimo promedio", fontSize = 11.sp, color = PastelMuted)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${getMoodWord(dayAvgMoodLevel)} ${getMoodEmoji(dayAvgMoodLevel)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f / 20", dayAvgMoodExact),
                                            fontSize = 11.sp,
                                            color = PastelMuted
                                        )
                                    }

                                    Column {
                                        Text(text = "Energía promedio", fontSize = 11.sp, color = PastelMuted)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f / 10 ⚡", dayAvgEnergy),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Total de registros", fontSize = 11.sp, color = PastelMuted)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$dayTotalEntries ${if (dayTotalEntries == 1) "registro" else "registros"}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }

                                    Column {
                                        Text(text = "Más frecuente", fontSize = 11.sp, color = PastelMuted)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${getMoodWord(dayMostFrequentMood)} ${getMoodEmoji(dayMostFrequentMood)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937)
                                        )
                                    }
                                }
                            }
                        }

                        // Daily Activities Summary
                        if (dayActivities.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "ACTIVIDADES DEL DÍA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelMuted,
                                    letterSpacing = 1.sp
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    dayActivities.forEach { act ->
                                        Surface(
                                            color = PastelLavender,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "${getActivityEmoji(act)} $act",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF312E81)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Detailed Entries List
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "DESGLOSE DE REGISTROS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelMuted,
                                letterSpacing = 1.sp
                            )

                            dayEntries.sortedByDescending { it.id }.forEach { entryForDate ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFFFFDFB))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(getMoodColor(entryForDate.moodLevel)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            FacebookStyleEmoji(emoji = getMoodEmoji(entryForDate.moodLevel), fontSize = 24.sp)
                                        }
                                        Column {
                                            Text(
                                                text = if (entryForDate.time.isNotBlank()) "${getMoodWord(entryForDate.moodLevel)} (${entryForDate.time})" else getMoodWord(entryForDate.moodLevel),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1F2937)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Intensidad: ${entryForDate.moodIntensity}/5 • Energía: ${entryForDate.energyLevel}/10",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    if (entryForDate.diaryText.isNotBlank()) {
                                        Text(
                                            text = "\"${entryForDate.diaryText}\"",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4B5563),
                                            lineHeight = 16.sp
                                        )
                                    }

                                    if (entryForDate.activities.isNotBlank()) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            entryForDate.activities.split(",").forEach { act ->
                                                val trimmed = act.trim()
                                                if (trimmed.isNotBlank()) {
                                                    Surface(
                                                        color = Color(0xFFF3F4F6),
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text(
                                                            text = "${getActivityEmoji(trimmed)} $trimmed",
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF6B7280)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🍃", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sin registros para este día",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = "No se guardó ningún estado de ánimo en esta fecha.",
                                fontSize = 12.sp,
                                color = PastelMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getActivityEmoji(activity: String): String {
    val lower = activity.lowercase().trim()
    return when {
        lower.contains("ejercicio") || lower.contains("deporte") || lower.contains("gym") -> "🏃"
        lower.contains("familia") -> "👨‍👩‍👧"
        lower.contains("juego") || lower.contains("videojuego") -> "🎮"
        lower.contains("trabajo") -> "💼"
        lower.contains("estudio") || lower.contains("escuela") || lower.contains("universidad") -> "📚"
        lower.contains("descanso") || lower.contains("dormir") || lower.contains("siesta") -> "😴"
        lower.contains("medita") -> "🧘"
        lower.contains("lectura") || lower.contains("leer") -> "📖"
        lower.contains("música") || lower.contains("musica") -> "🎵"
        lower.contains("naturaleza") || lower.contains("parque") -> "🌿"
        lower.contains("caminar") || lower.contains("paseo") -> "🚶"
        lower.contains("amigos") || lower.contains("fiesta") -> "👥"
        lower.contains("creativo") || lower.contains("arte") || lower.contains("pintar") -> "🎨"
        lower.contains("cine") || lower.contains("serie") || lower.contains("película") -> "🎬"
        else -> "✨"
    }
}

data class ActivityStat(
    val name: String,
    val count: Int,
    val avgMood: Double,
    val avgEnergy: Double
)

@Composable
fun EnergyWaveChart(energyByDay: List<Pair<String, Int>>) {
    if (energyByDay.isEmpty()) {
        Text("Sin registros de energía en los últimos 7 días.", fontSize = 13.sp, color = PastelMuted)
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = if (energyByDay.size > 1) width / (energyByDay.size - 1) else width

            val points = energyByDay.mapIndexed { index, pair ->
                val x = if (energyByDay.size > 1) index * spacing else width / 2
                val y = height - (pair.second / 10f * height).coerceIn(10f, height - 10f)
                Offset(x, y)
            }

            // Draw dashed reference grid lines (25%, 50%, 75%)
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(0f, height * frac),
                    end = Offset(width, height * frac),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            if (points.size > 1) {
                // Gradient Fill Path below Wave Curve
                val fillPath = Path().apply {
                    moveTo(points.first().x, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cx = (p1.x + p2.x) / 2
                        cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    }
                    lineTo(points.last().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x6610B981), Color(0x0010B981))
                    )
                )

                // Wave Curve Line Path
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cx = (p1.x + p2.x) / 2
                        cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = Color(0xFF10B981),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            // Glowing nodes
            points.forEach { point ->
                drawCircle(color = Color.White, radius = 7f, center = point)
                drawCircle(color = Color(0xFF10B981), radius = 5f, center = point)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            energyByDay.forEach { (dateStr, energyVal) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = PastelMint,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$energyVal ⚡",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = try { dateStr.takeLast(5) } catch (_: Exception) { "" },
                        fontSize = 10.sp,
                        color = PastelMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EmotionDistributionChart(last7DaysMoods: List<MoodEntry>) {
    if (last7DaysMoods.isEmpty()) {
        Text("Sin emociones registradas en los últimos 7 días.", fontSize = 13.sp, color = PastelMuted)
        return
    }

    val totalCount = last7DaysMoods.size
    val frequencyMap = remember(last7DaysMoods) {
        last7DaysMoods.groupBy { it.moodLevel }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        frequencyMap.forEach { (level, count) ->
            val percentage = ((count.toFloat() / totalCount) * 100).toInt()
            val progress = count.toFloat() / totalCount.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFFDFB))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FacebookStyleEmoji(emoji = getMoodEmoji(level), fontSize = 22.sp)
                        Text(
                            text = getMoodWord(level),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1F2937)
                        )
                    }

                    Surface(
                        color = getMoodColor(level),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$count ${if (count == 1) "registro" else "registros"} ($percentage%)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                }

                // Smooth horizontal progress bar track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF3F4F6))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(getMoodColor(level))
                    )
                }
            }
        }
    }
}

@Composable
fun MoodTrendLineChart(last30DaysMoods: List<MoodEntry>) {
    if (last30DaysMoods.isEmpty()) {
        Text("Aún no hay suficientes datos para mostrar la curva de 30 días.", fontSize = 13.sp, color = PastelMuted)
        return
    }

    val sorted30 = remember(last30DaysMoods) { last30DaysMoods.sortedBy { it.date }.takeLast(12) }
    val avg30 = remember(last30DaysMoods) { last30DaysMoods.map { it.moodLevel }.average() }
    val maxEntry = remember(last30DaysMoods) { last30DaysMoods.maxByOrNull { it.moodLevel } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Explanatory Summary Box
        Surface(
            color = Color(0xFFFFFDFB),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Promedio mensual: ${getMoodWord(avg30.toInt())} ${getMoodEmoji(avg30.toInt())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f / 20", avg30),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                }

                if (maxEntry != null) {
                    Text(
                        text = "Punto más alto: ${getMoodWord(maxEntry.moodLevel)} ${getMoodEmoji(maxEntry.moodLevel)} (${maxEntry.date})",
                        fontSize = 11.sp,
                        color = Color(0xFF047857),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Y-Axis Scale Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "▲ Nivel 20 (Agradecido)", fontSize = 10.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
            Text(text = "▼ Nivel 1 (Tristeza/Cuidado)", fontSize = 10.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
        }

        // Canvas Curve
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = if (sorted30.size > 1) width / (sorted30.size - 1) else width

            val points = sorted30.mapIndexed { index, entry ->
                val x = if (sorted30.size > 1) index * spacing else width / 2
                val y = height - (entry.moodLevel / 20f * height).coerceIn(10f, height - 10f)
                Offset(x, y)
            }

            // Reference Dashed Grid Lines (Levels 4, 8, 12)
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(0f, height * frac),
                    end = Offset(width, height * frac),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            if (points.size > 1) {
                // Gradient fill below wave curve
                val fillPath = Path().apply {
                    moveTo(points.first().x, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cx = (p1.x + p2.x) / 2
                        cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    }
                    lineTo(points.last().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x666366F1), Color(0x006366F1))
                    )
                )

                // Smooth line path
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cx = (p1.x + p2.x) / 2
                        cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = Color(0xFF4F46E5),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            points.forEach { point ->
                drawCircle(color = Color.White, radius = 6f, center = point)
                drawCircle(color = Color(0xFF4F46E5), radius = 4f, center = point)
            }
        }

        // X-Axis Dates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            sorted30.forEach { entry ->
                Text(
                    text = try { entry.date.takeLast(2) } catch (_: Exception) { "" },
                    fontSize = 9.sp,
                    color = PastelMuted
                )
            }
        }

        // Explanatory Note
        Text(
            text = "💡 Explicación: La curva conecta tus registros diarios para mostrar la estabilidad y tendencia de tu estado de ánimo a lo largo de los últimos 30 días.",
            fontSize = 10.sp,
            color = PastelMuted,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun StatsViewAesthetic(
    moodList: List<MoodEntry>,
    context: android.content.Context
) {
    val today = LocalDate.now()

    // Filter for the last 7 days
    val last7DaysMoods = moodList.filter { entry ->
        try {
            val entryDate = LocalDate.parse(entry.date)
            !entryDate.isBefore(today.minusDays(6)) && !entryDate.isAfter(today)
        } catch (_: Exception) {
            false
        }
    }

    // Filter for last 30 days for temporal trend chart & correlations
    val last30DaysMoods = moodList.filter { entry ->
        try {
            val entryDate = LocalDate.parse(entry.date)
            !entryDate.isBefore(today.minusDays(29)) && !entryDate.isAfter(today)
        } catch (_: Exception) {
            false
        }
    }.ifEmpty { moodList }

    val distinctDays7 = last7DaysMoods.map { it.date }.distinct().size
    val totalEntries7 = last7DaysMoods.size
    val avgMood7 = if (last7DaysMoods.isNotEmpty()) last7DaysMoods.map { it.moodLevel }.average() else 10.0
    val avgEnergy7 = if (last7DaysMoods.isNotEmpty()) last7DaysMoods.map { it.energyLevel }.average() else 0.0

    val overallAvgMood = if (last30DaysMoods.isNotEmpty()) last30DaysMoods.map { it.moodLevel }.average() else 10.0

    // Compute activity stats
    val activityStats = last30DaysMoods.let { list ->
        val map = mutableMapOf<String, MutableList<MoodEntry>>()
        list.forEach { entry ->
            entry.activities.split(",").forEach { act ->
                val trimmed = act.trim()
                if (trimmed.isNotBlank()) {
                    map.getOrPut(trimmed) { mutableListOf() }.add(entry)
                }
            }
        }
        map.map { (act, entries) ->
            ActivityStat(
                name = act,
                count = entries.size,
                avgMood = entries.map { it.moodLevel }.average(),
                avgEnergy = entries.map { it.energyLevel }.average()
            )
        }.sortedByDescending { it.count }
    }

    // Best day calculation (highest mood level day of last 7 days)
    val bestDayEntry = last7DaysMoods.maxByOrNull { it.moodLevel }
    val bestDayName = if (bestDayEntry != null) {
        try {
            LocalDate.parse(bestDayEntry.date)
                .format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("es")))
                .replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            "N/A"
        }
    } else "N/A"

    // Most frequent mood in last 7 days
    val moodCounts = mutableMapOf<Int, Int>().apply {
        for (i in 1..16) set(i, 0)
        last7DaysMoods.forEach { set(it.moodLevel, (get(it.moodLevel) ?: 0) + 1) }
    }
    val mostFrequentLevel = moodCounts.entries.maxByOrNull { it.value }?.key ?: avgMood7.toInt()

    // Energy Evolution Chart grouped by date (daily average)
    val energyByDay = remember(last7DaysMoods) {
        last7DaysMoods.groupBy { it.date }
            .map { (date, entries) ->
                val avgEnergy = entries.map { it.energyLevel }.average().toInt()
                date to avgEnergy
            }
            .sortedBy { it.first }
            .takeLast(7)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // PDF Export Action Card (Aesthetic card style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelLavender),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reporte de Bienestar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF312E81)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Exporta tu historial y estadísticas en PDF",
                        fontSize = 12.sp,
                        color = Color(0xFF4338CA)
                    )
                }
                MonthlyReportButton(moodList)
            }
        }

        // Rich MVP Summary Card (Esta semana)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTA SEMANA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelMuted,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "✨ Resumen",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Estado promedio", fontSize = 12.sp, color = PastelMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f / 20 %s", avgMood7, getMoodEmoji(avgMood7.toInt())),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }

                    Column {
                        Text(text = "Energía promedio", fontSize = 12.sp, color = PastelMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f / 10 ⚡", avgEnergy7),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Días con registro", fontSize = 12.sp, color = PastelMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$distinctDays7 días ($totalEntries7 reg.)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }

                    Column {
                        Text(text = "Mejor día", fontSize = 12.sp, color = PastelMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$bestDayName 🟢",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }

                    Column {
                        Text(text = "Más frecuente", fontSize = 12.sp, color = PastelMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${getMoodWord(mostFrequentLevel)} ${getMoodEmoji(mostFrequentLevel)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }
        }

        // DESGLOSE Y FRECUENCIA DE EMOCIONES (Horizontal progress track breakdown)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "DESGLOSE DE EMOCIONES (ÚLTIMOS 7 DÍAS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                EmotionDistributionChart(last7DaysMoods = last7DaysMoods)
            }
        }

        // EVOLUCIÓN DE ENERGÍA (CURVA DE ONDA CONTINUA ⚡)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVOLUCIÓN DE ENERGÍA (ÚLTIMOS 7 DÍAS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelMuted,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = PastelMint,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "Promedio: %.1f / 10", avgEnergy7),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                EnergyWaveChart(energyByDay = energyByDay)
            }
        }

        // TENDENCIA TEMPORAL DE 30 DÍAS (CURVA CONTINUA DE BIENESTAR)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "TENDENCIA TEMPORAL (30 DÍAS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                MoodTrendLineChart(last30DaysMoods = last30DaysMoods)
            }
        }

        // Activity & Mood Correlation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "CORRELACIÓN DE ACTIVIDADES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (activityStats.isEmpty()) {
                    Text(
                        text = "Aún no hay suficientes actividades registradas para mostrar correlaciones.",
                        fontSize = 13.sp,
                        color = PastelMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        activityStats.forEach { stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFFFDFB))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PastelLavender),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = getActivityEmoji(stat.name), fontSize = 20.sp)
                                    }
                                    Column {
                                        Text(text = stat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
                                        Text(text = "${stat.count} registros", fontSize = 11.sp, color = PastelMuted)
                                    }
                                }
                                Surface(
                                    color = Color(0xFFEEF2FF),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "Promedio: %.1f / 20", stat.avgMood),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detected Patterns Card (Patrones detectados 🔎)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PastelCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "PATRONES DETECTADOS 🔎",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelMuted,
                    letterSpacing = 1.sp
                )

                if (activityStats.isEmpty()) {
                    Text(
                        text = "Registra más actividades en tus días para que la app detecte patrones de relación.",
                        fontSize = 13.sp,
                        color = PastelMuted
                    )
                } else {
                    activityStats.forEach { stat ->
                        val diff = stat.avgMood - overallAvgMood
                        val emoji = getActivityEmoji(stat.name)

                        val insightText = when {
                            diff >= 0.8 -> "Se observa una relación: los días con ${stat.name} registraste un estado de ánimo promedio ${String.format(Locale.getDefault(), "+%.1f", diff)} puntos mayor que tu media general."
                            diff <= -0.8 -> "Se observa una relación: los días con ${stat.name} registraste un estado de ánimo promedio menor que tu media general."
                            stat.avgEnergy >= 7.5 -> "Se observa una relación: los días con ${stat.name} registraste mayor nivel de energía promedio."
                            else -> "Se observa una relación estable los días que incluyes ${stat.name} en tus jornadas."
                        }

                        Surface(
                            color = Color(0xFFFFFDFB),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PastelPeach),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                                Text(
                                    text = insightText,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563),
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ℹ️ Nota: Estas observaciones muestran correlaciones estadísticas en tus datos registrados, no causalidad absoluta.",
                        fontSize = 10.sp,
                        color = PastelMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}



