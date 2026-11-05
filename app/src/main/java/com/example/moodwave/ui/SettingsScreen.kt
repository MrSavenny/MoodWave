package com.example.moodwave.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import android.widget.Toast
import android.net.Uri
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodwave.data.PreferencesHelper
import com.example.moodwave.ui.theme.*

@Composable
fun ProfileImageAvatar(
    imageUriOrEmoji: String,
    modifier: Modifier = Modifier,
    iconSize: TextUnit = 28.sp
) {
    val context = LocalContext.current
    val uriString = imageUriOrEmoji.ifBlank { "🌸" }

    if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
        val bitmap by produceState<android.graphics.Bitmap?>(null, uriString) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try { com.example.moodwave.data.ProfilePhoto.thumbnail(context, Uri.parse(uriString)) }
                catch (_: Exception) { null }
            }
        }
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(CircleShape)
            )
        } else {
            Text(text = "🌸", fontSize = iconSize)
        }
    } else {
        Text(text = uriString, fontSize = iconSize)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit,
    onChoosePhoto: () -> Unit,
    appearance: PreferencesHelper.Appearance,
    onAppearanceChange: (PreferencesHelper.Appearance) -> Unit,
    onHealth: () -> Unit,
    onCloud: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var lockMode by remember { mutableStateOf(PreferencesHelper.getLockMode(context)) }
    var isBiometricEnabled by remember { mutableStateOf(PreferencesHelper.isBiometricEnabled(context)) }
    var storedPin by remember { mutableStateOf(PreferencesHelper.getPin(context)) }

    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }

    var userName by remember { mutableStateOf(PreferencesHelper.getUserName(context)) }


    val avatars = listOf("🌸", "🌱", "🧘", "☕", "🌊", "✨")

    Scaffold(
        containerColor = PastelBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PastelLavender)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color(0xFF4B5563))
                    }
                    Text(
                        text = "Ajustes y Perfil",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PastelText
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp).background(PastelLavender, CircleShape), contentAlignment = Alignment.Center) {
                    ProfileImageAvatar(selectedAvatar, Modifier.fillMaxSize(), 32.sp)
                }
                Column {
                    Text(userName.ifBlank { "Tu espacio" }, color = PastelText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("Haz de MoodWave un lugar para ti", color = PastelMuted, fontSize = 13.sp)
                }
            }
            SettingsSection("Editar perfil", "Tu nombre, foto o emoji", Icons.Default.Person) {
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(PastelLavender),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileImageAvatar(
                            imageUriOrEmoji = selectedAvatar,
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 36.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        avatars.forEach { av ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (selectedAvatar == av) PastelPink else PastelLavender)
                                    .clickable {
                                        onAvatarSelected(av)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = av, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onChoosePhoto,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PatternPurple)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Galería", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Elegir foto de mi Galería", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            PreferencesHelper.setUserName(context, it)
                        },
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PatternPurple,
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        )
                    )
                }
            }

            }
            Card(colors = CardDefaults.cardColors(containerColor = PastelCard), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Apariencia", color = PastelText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreferencesHelper.Appearance.entries.forEach { mode ->
                            val active = mode == appearance
                            val color by animateColorAsState(if (active) PatternPurple else PastelLavender, label = "appearance")
                            val icon = when (mode) {
                                PreferencesHelper.Appearance.SYSTEM -> Icons.Default.SettingsBrightness
                                PreferencesHelper.Appearance.LIGHT -> Icons.Default.LightMode
                                PreferencesHelper.Appearance.DARK -> Icons.Default.DarkMode
                            }
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(color)
                                .selectable(active, role = Role.RadioButton, onClick = { onAppearanceChange(mode) })
                                .padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(icon, null, tint = if (active) Color.White else PatternPurple)
                                Text(mode.title, color = if (active) Color.White else Color(0xFF352D58), fontSize = 13.sp)
                            }
                        }
                    }
                    Text(if (appearance == PreferencesHelper.Appearance.SYSTEM) "Se adapta al tema de tu teléfono." else "Puedes cambiarlo cuando quieras.", color = PastelMuted, fontSize = 12.sp)
                }
            }
            SettingsSection("Recordatorios", "Un momento para registrar cómo te sientes", Icons.Default.NotificationsNone) {
                ReminderSettings()
            }
            SettingsSection("Seguridad", if (lockMode == "NONE") "Sin bloqueo de acceso" else "Protegido con PIN", Icons.Default.Lock) {
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
                    // Lock Option 1: None
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lockMode = "NONE"
                                PreferencesHelper.setLockMode(context, "NONE")
                                Toast.makeText(context, "Bloqueo desactivado", Toast.LENGTH_SHORT).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (lockMode == "NONE") PastelMint else PastelLavender),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Sin bloqueo",
                                    tint = if (lockMode == "NONE") Color(0xFF047857) else Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(text = "Sin Bloqueo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PastelText)
                                Text(text = "Acceso directo a la aplicación", fontSize = 12.sp, color = PastelMuted)
                            }
                        }
                        RadioButton(
                            selected = lockMode == "NONE",
                            onClick = {
                                lockMode = "NONE"
                                PreferencesHelper.setLockMode(context, "NONE")
                                Toast.makeText(context, "Bloqueo desactivado", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    HorizontalDivider(color = PastelLavender, thickness = 1.dp)

                    // Lock Option 2: PIN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (storedPin.isBlank()) {
                                    showPinDialog = true
                                } else {
                                    lockMode = "PIN"
                                    PreferencesHelper.setLockMode(context, "PIN")
                                    Toast.makeText(context, "Bloqueo por PIN activado", Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (lockMode == "PIN") PastelSky else PastelLavender),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "PIN",
                                    tint = if (lockMode == "PIN") Color(0xFF0284C7) else Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "PIN de seguridad",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PastelText
                                )
                                Text(text = "Código de 4 a 10 dígitos", fontSize = 12.sp, color = PastelMuted)
                            }
                        }
                        RadioButton(
                            selected = lockMode == "PIN",
                            onClick = {
                                if (storedPin.isBlank()) {
                                    showPinDialog = true
                                } else {
                                    lockMode = "PIN"
                                    PreferencesHelper.setLockMode(context, "PIN")
                                    Toast.makeText(context, "Bloqueo por PIN activado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (storedPin.isNotBlank()) {
                        TextButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cambiar PIN existente", color = PatternPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = PastelLavender, thickness = 1.dp)

                    // Lock Option 3: Fingerprint / Biometric Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newBio = !isBiometricEnabled
                                isBiometricEnabled = newBio
                                PreferencesHelper.setBiometricEnabled(context, newBio)
                                Toast.makeText(context, if (newBio) "Huella activada" else "Huella desactivada", Toast.LENGTH_SHORT).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isBiometricEnabled) PastelLavender else PastelLavender),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Desbloqueo con Huella",
                                    tint = if (isBiometricEnabled) PatternPurple else Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(text = "Desbloqueo con Huella", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PastelText)
                                Text(text = "Usar huella digital como alternativa", fontSize = 12.sp, color = PastelMuted)
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { checked ->
                                isBiometricEnabled = checked
                                PreferencesHelper.setBiometricEnabled(context, checked)
                                Toast.makeText(context, if (checked) "Huella activada" else "Huella desactivada", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            }
            Card(colors = CardDefaults.cardColors(containerColor = PastelCard), shape = RoundedCornerShape(24.dp)) {
                SettingsLink("Sueño y pasos", "Conecta tus datos de salud", Icons.Default.FavoriteBorder, onHealth)
                HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = PastelLavender)
                SettingsLink("Respaldo en la nube", "Guarda una copia de tu diario", Icons.Default.CloudQueue, onCloud)
            }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }

    // Set or Change PIN Dialog (4-10 digits)
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (storedPin.isBlank()) "Crear PIN de Seguridad" else "Cambiar PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Introduce un PIN numérico de entre 4 y 10 dígitos.", fontSize = 13.sp, color = PastelMuted)

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) newPin = it },
                        label = { Text("Nuevo PIN (4-10 dígitos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) confirmPin = it },
                        label = { Text("Confirmar Nuevo PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length in 4..10 && newPin == confirmPin) {
                            storedPin = newPin
                            PreferencesHelper.setPin(context, newPin)
                            lockMode = "PIN"
                            PreferencesHelper.setLockMode(context, "PIN")
                            showPinDialog = false
                            newPin = ""
                            confirmPin = ""
                            Toast.makeText(context, "PIN guardado exitosamente", Toast.LENGTH_SHORT).show()
                        } else if (newPin.length < 4) {
                            Toast.makeText(context, "El PIN debe tener al menos 4 dígitos", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Los PINs no coinciden", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PatternPurple)
                ) {
                    Text("Guardar PIN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    newPin = ""
                    confirmPin = ""
                }) {
                    Text("Cancelar", color = Color(0xFF6B7280))
                }
            }
        )
    }
}


@Composable
private fun SettingsSection(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PastelCard)) {
        SettingsLink(title, subtitle, icon, { expanded = !expanded }, expanded)
        AnimatedVisibility(expanded) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun SettingsLink(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, expanded: Boolean = false) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).background(PastelLavender, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = PatternPurple, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = PastelText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = PastelMuted, fontSize = 12.sp)
        }
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
            if (expanded) "Contraer" else "Abrir", tint = PastelMuted)
    }
}
