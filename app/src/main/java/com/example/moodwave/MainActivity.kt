package com.example.moodwave

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.lifecycle.lifecycleScope
import com.example.moodwave.data.ProfilePhoto
import com.example.moodwave.data.PreferencesHelper.Appearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moodwave.data.MoodDatabase
import com.example.moodwave.data.PreferencesHelper
import com.example.moodwave.ui.AddMoodScreen
import com.example.moodwave.ui.HomeScreen
import com.example.moodwave.ui.MoodViewModel
import com.example.moodwave.ui.MoodViewModelFactory
import com.example.moodwave.ui.SettingsScreen
import com.example.moodwave.ui.components.FacebookStyleEmoji
import com.example.moodwave.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppSession {
    var isUnlocked by mutableStateOf(false)
    var isSplashCompleted by mutableStateOf(false)
}

class MainActivity : FragmentActivity() {
    private var openEntry by mutableStateOf(false)
    private var avatar by mutableStateOf("")
    private var avatarRevision = 0

    private fun changeAvatar(value: String) {
        avatarRevision++
        val previous = avatar
        PreferencesHelper.setProfileImage(this, value)
        avatar = value
        if (previous != value) ProfilePhoto.discard(this, previous)
    }

    private val gallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val revision = ++avatarRevision
            lifecycleScope.launch {
                try {
                    val saved = withContext(Dispatchers.IO) { ProfilePhoto.save(this@MainActivity, uri) }
                    if (revision == avatarRevision) changeAvatar(saved) else ProfilePhoto.discard(this@MainActivity, saved)
                } catch (e: java.util.concurrent.CancellationException) { throw e }
                catch (_: Exception) { Toast.makeText(this@MainActivity, "No se pudo leer la foto. Prueba con otra imagen.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        openEntry = intent.getBooleanExtra("open_entry", false)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val lockMode = PreferencesHelper.getLockMode(this)
        AppSession.isUnlocked = (lockMode == "NONE")

        com.example.moodwave.reminders.DailyReminder.schedule(this)
        avatar = PreferencesHelper.getProfileImage(this)
        setContent {
            var appearance by remember { mutableStateOf(PreferencesHelper.appearance(this)) }
            val dark = when (appearance) {
                Appearance.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                Appearance.DARK -> true
                Appearance.LIGHT -> false
            }
            val screens = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            com.example.moodwave.ui.MonthlyReportExportHost { MoodWaveTheme(darkTheme = dark) {
                com.example.moodwave.ui.BackupHost {
                val context = LocalContext.current
                var currentLockMode by remember { mutableStateOf(PreferencesHelper.getLockMode(context)) }
                val isUnlocked = AppSession.isUnlocked
                val isSplashCompleted = AppSession.isSplashCompleted

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PastelBg
                ) {
                    if (!isSplashCompleted) {
                        SplashScreen(
                            onSplashFinished = { AppSession.isSplashCompleted = true }
                        )
                    } else if (!isUnlocked && currentLockMode != "NONE") {
                        LockScreen(
                            onUnlock = { AppSession.isUnlocked = true }
                        )
                    } else screens.SaveableStateProvider("unlocked") {
                        val navController = rememberNavController()
                        LaunchedEffect(openEntry) {
                            if (openEntry) {
                                navController.navigate("add_mood") { launchSingleTop = true }
                                openEntry = false
                                intent.removeExtra("open_entry")
                            }
                        }
                        val database = MoodDatabase.getDatabase(context)
                        val viewModel: MoodViewModel = viewModel(
                            factory = MoodViewModelFactory(database.moodDao())
                        )



                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToAdd = { navController.navigate("add_mood") },
                                    onLockApp = {
                                        val mode = PreferencesHelper.getLockMode(context)
                                        if (mode == "NONE") {
                                            navController.navigate("settings")
                                            Toast.makeText(context, "Crea un PIN en Ajustes primero para activar el bloqueo", Toast.LENGTH_SHORT).show()
                                        } else {
                                            currentLockMode = mode
                                            AppSession.isUnlocked = false
                                        }
                                    },
                                    onNavigateToSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("add_mood") {
                                AddMoodScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("health") { com.example.moodwave.ui.HealthScreen { navController.popBackStack() } }
                            composable("cloud") { com.example.moodwave.ui.CloudScreen { navController.popBackStack() } }
                            composable("settings") {
                                SettingsScreen(
                                    onHealth = { navController.navigate("health") },
                                    onCloud = { navController.navigate("cloud") },
                                    selectedAvatar = avatar, onAvatarSelected = ::changeAvatar,
                                    onChoosePhoto = {
                                        try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                        catch (_: android.content.ActivityNotFoundException) {
                                            Toast.makeText(this@MainActivity, "No hay un selector de fotos disponible.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    appearance = appearance, onAppearanceChange = {
                                        PreferencesHelper.setAppearance(this@MainActivity, it)
                                        appearance = it
                                    },
                                    onNavigateBack = {
                                        currentLockMode = PreferencesHelper.getLockMode(context)
                                        if (currentLockMode == "NONE") {
                                            AppSession.isUnlocked = true
                                        }
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openEntry = intent.getBooleanExtra("open_entry", false)
    }

    override fun onStop() {
        super.onStop()
        if (PreferencesHelper.getLockMode(this) != "NONE") {
            AppSession.isUnlocked = false
        }
    }
}

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0.6f) }
    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650)
            )
        }
        delay(1700)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (AppThemeState.isDarkMode) listOf(PastelBg, PastelCard) else listOf(Color(0xFFEEF2FF), Color(0xFFFFFDFB), Color(0xFFE0E7FF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scaleAnim.value,
                    scaleY = scaleAnim.value,
                    alpha = alphaAnim.value
                )
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, Color(0xFF818CF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FacebookStyleEmoji(emoji = "🌸", fontSize = 54.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MoodWave",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PastelText,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tu lienzo de bienestar personal",
                fontSize = 14.sp,
                color = PastelMuted,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(44.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF818CF8),
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Ingresa tu PIN") }
    val isBiometricEnabled = remember { PreferencesHelper.isBiometricEnabled(context) }

    fun triggerBiometric() {
        if (!isBiometricEnabled) return
        val activity = context as? FragmentActivity
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlock()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(activity, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear MoodWave")
                .setSubtitle("Usa tu huella digital para acceder")
                .setNegativeButtonText("Cancelar")
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            triggerBiometric()
        }
    }

    fun verifyPin() {
        if (pin.length < 4) {
            showError = true
            errorMessage = "El PIN debe tener al menos 4 dígitos"
            return
        }
        val correctPin = PreferencesHelper.getPin(context)
        if (pin == correctPin) {
            onUnlock()
        } else {
            showError = true
            errorMessage = "PIN incorrecto"
            pin = ""
        }
    }

    fun addPin(digit: String) {
        if (pin.length < 10) {
            pin += digit
            showError = false
            errorMessage = "Ingresa tu PIN"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PastelBg)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Seguridad",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PastelText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (showError) errorMessage else "Ingresa tu PIN",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = if (showError) Color(0xFFFB7185) else PastelMuted,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic PIN Dots
            val expectedLength = remember { PreferencesHelper.getPin(context).length }
            val dotsCount = maxOf(expectedLength, pin.length)

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until dotsCount) {
                    Box(
                        modifier = Modifier
                            .size(if (i < pin.length) 16.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (i < pin.length) Color(0xFF818CF8) else Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pastel Keypad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("delete", "0", "check")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(280.dp)
            ) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { item ->
                            val bg = when (item) {
                                "check" -> PastelMint
                                "delete" -> PastelPeach
                                else -> Color.White
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.3f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bg)
                                    .clickable {
                                        when (item) {
                                            "delete" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            "check" -> verifyPin()
                                            else -> addPin(item)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (item) {
                                    "delete" -> Icon(imageVector = Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFF4B5563), modifier = Modifier.size(22.dp))
                                    "check" -> Icon(imageVector = Icons.Default.Check, contentDescription = "Confirmar", tint = Color(0xFF047857), modifier = Modifier.size(22.dp))
                                    else -> Text(text = item, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                }
                            }
                        }
                    }
                }
            }

            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(20.dp))

                // Single Fingerprint Button at the bottom
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { triggerBiometric() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Huella",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
