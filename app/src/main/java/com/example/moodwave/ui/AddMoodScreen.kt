package com.example.moodwave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.data.PreferencesHelper
import com.example.moodwave.model.MoodEntry
import com.example.moodwave.ui.components.FacebookStyleEmoji
import com.example.moodwave.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMoodScreen(
    viewModel: MoodViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var moodLevel by remember { mutableStateOf(11) } // Default Neutral (11)
    var moodIntensity by remember { mutableStateOf(3) } // Scale 1 to 5
    var energyLevel by remember { mutableStateOf(5f) }
    var diaryText by remember { mutableStateOf("") }
    val selectedActivities = remember { mutableStateListOf<String>() }

    val defaultActivities = listOf(
        "Trabajo", "Estudio", "Ejercicio", "Familia", "Amigos",
        "Lectura", "Música", "Naturaleza", "Meditación", "Caminar",
        "Descanso", "Creativo", "Cine/Series", "Juegos"
    )
    val customActivities = remember { mutableStateListOf<String>().apply { addAll(PreferencesHelper.getCustomActivities(context)) } }
    val allActivities = (defaultActivities + customActivities).distinct()

    var showAddActivityDialog by remember { mutableStateOf(false) }
    var newActivityName by remember { mutableStateOf("") }

    val moodLevels = (1..20).toList()

    fun hasIntensity(level: Int): Boolean {
        // Neutral (11), Calmado (12), En Paz (13), Renovado (14) do not require intensity scaling
        return level !in listOf(11, 12, 13, 14)
    }

    fun getIntensityLabel(level: Int): String {
        return when (level) {
            1 -> "Leve"
            2 -> "Moderado"
            3 -> "Notable"
            4 -> "Fuerte"
            5 -> "Muy Intenso"
            else -> "Notable"
        }
    }

    Scaffold(
        containerColor = PastelBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Cómo estás hoy?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PastelText
                )
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF4B5563))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Mood Selection (20 distinct levels with Facebook-style animated emojis & labels below)
            Column {
                Text(
                    text = "1. SELECCIONA TU EMOCIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    moodLevels.chunked(4).forEach { rowLevels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowLevels.forEach { level ->
                                val isSelected = moodLevel == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.9f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) getMoodColor(level) else PastelCard)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF3F4F6),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { moodLevel = level },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        FacebookStyleEmoji(emoji = getMoodEmoji(level), fontSize = 24.sp, isAnimated = isSelected)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = getMoodWord(level),
                                            fontSize = 8.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF1F2937) else Color(0xFF6B7280),
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = getMoodColor(moodLevel),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "${getMoodWord(moodLevel)} ${getMoodEmoji(moodLevel)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Mood Intensity Scale (1 to 5) - Only visible for emotions with variable intensity
            if (hasIntensity(moodLevel)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. INTENSIDAD DE ${getMoodWord(moodLevel).uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelMuted,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = getMoodColor(moodLevel),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "$moodIntensity/5 • ${getIntensityLabel(moodIntensity)}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { lvl ->
                            val isSelected = moodIntensity == lvl
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) getMoodColor(moodLevel) else PastelCard)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { moodIntensity = lvl },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$lvl",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1F2937)
                                )
                            }
                        }
                    }
                }
            }

            // Energy Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasIntensity(moodLevel)) "3. NIVEL DE ENERGÍA" else "2. NIVEL DE ENERGÍA",
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
                            text = "${energyLevel.toInt()}/10",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = energyLevel,
                    onValueChange = { energyLevel = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF10B981),
                        activeTrackColor = Color(0xFF10B981),
                        inactiveTrackColor = Color(0xFFE5E7EB)
                    )
                )
            }

            // Activities Tags
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasIntensity(moodLevel)) "4. ACTIVIDADES DE HOY" else "3. ACTIVIDADES DE HOY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelMuted,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allActivities.forEach { act ->
                        val isSelected = selectedActivities.contains(act)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedActivities.remove(act)
                                else selectedActivities.add(act)
                            },
                            label = { Text(act) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PastelPink,
                                selectedLabelColor = Color(0xFF880E4F),
                                containerColor = PastelCard,
                                labelColor = Color(0xFF4B5563)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // Button + Agregar actividad
                    AssistChip(
                        onClick = {
                            newActivityName = ""
                            showAddActivityDialog = true
                        },
                        label = { Text("+ Agregar actividad", fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PastelLavender,
                            labelColor = Color(0xFF312E81)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = null
                    )
                }
            }

            // Journal / Diary
            Column {
                Text(
                    text = if (hasIntensity(moodLevel)) "5. DIARIO ÍNTIMO" else "4. DIARIO ÍNTIMO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = diaryText,
                    onValueChange = { diaryText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("Escribe tus pensamientos aquí...", color = PastelMuted) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFFDFB),
                        unfocusedContainerColor = Color(0xFFFFFDFB),
                        focusedBorderColor = PastelLavender,
                        unfocusedBorderColor = Color(0xFFF3F4F6)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button with Time stamp
            Button(
                onClick = {
                    val currentTimeFormatted = try {
                        LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es")))
                    } catch (_: Exception) { "" }

                    val finalIntensity = if (hasIntensity(moodLevel)) moodIntensity else 1

                    val newEntry = MoodEntry(
                        date = LocalDate.now().toString(),
                        time = currentTimeFormatted,
                        moodLevel = moodLevel,
                        moodIntensity = finalIntensity,
                        energyLevel = energyLevel.toInt(),
                        diaryText = diaryText,
                        activities = selectedActivities.joinToString(",")
                    )
                    viewModel.saveMood(newEntry)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Guardar Memoria", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    if (showAddActivityDialog) {
        AlertDialog(
            onDismissRequest = { showAddActivityDialog = false },
            title = { Text("Nueva Actividad", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newActivityName,
                    onValueChange = { newActivityName = it },
                    label = { Text("Nombre de la actividad") },
                    placeholder = { Text("Ej: Yoga, Viaje, Mascotas...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newActivityName.trim()
                        if (trimmed.isNotBlank()) {
                            PreferencesHelper.addCustomActivity(context, trimmed)
                            if (!customActivities.contains(trimmed)) {
                                customActivities.add(trimmed)
                            }
                            if (!selectedActivities.contains(trimmed)) {
                                selectedActivities.add(trimmed)
                            }
                        }
                        showAddActivityDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddActivityDialog = false }) {
                    Text("Cancelar", color = Color(0xFF6B7280))
                }
            }
        )
    }
}
