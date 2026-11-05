package com.example.moodwave.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.moodwave.health.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HealthScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var refresh by remember { mutableIntStateOf(0) }
    var status by remember { mutableIntStateOf(HealthConnectClient.getSdkStatus(context)) }
    var summary by remember { mutableStateOf<HealthSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permission = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refresh) {
        status = HealthConnectClient.getSdkStatus(context)
        summary = null
        error = null
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            loading = true
            try { summary = withContext(Dispatchers.IO) { HealthRepository.readWeek(context) } }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: SecurityException) { error = "El acceso cambió. Revisa tus permisos y actualiza." }
            catch (_: Exception) { error = "No fue posible consultar Health Connect. Inténtalo de nuevo." }
            finally { loading = false }
        }
    }
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text("Sueño y pasos", style = MaterialTheme.typography.headlineLarge)
        Text(HEALTH_PRIVACY)
        when (status) {
            HealthConnectClient.SDK_UNAVAILABLE -> Text("Health Connect no está disponible en este dispositivo.")
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Text("Instala o actualiza Health Connect para consultar estos datos.")
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))
                }) { Text("Abrir Health Connect en Play Store") }
            }
            else -> {
                Button(onClick = { permission.launch(HealthRepository.permissions) }, enabled = !loading) { Text("Conectar Health Connect") }
                OutlinedButton(onClick = { refresh++ }, enabled = !loading) { Text("Actualizar") }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                summary?.let { result ->
                    Text("Pasos: " + if (HealthRepository.stepsPermission in result.permissions) "acceso concedido" else "sin permiso")
                    Text("Sueño: " + if (HealthRepository.sleepPermission in result.permissions) "acceso concedido" else "sin permiso")
                    Text("Últimos siete días. El sueño se suma dentro de cada día calendario.")
                    result.days.forEach { day ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(day.date.toString())
                                val hasStepsPermission = HealthRepository.stepsPermission in result.permissions
                                val hasSleepPermission = HealthRepository.sleepPermission in result.permissions
                                Text("Pasos: ${day.steps?.toString() ?: if (hasStepsPermission) "Sin registros" else "Sin permiso"}")
                                Text("Sueño: ${day.sleepMinutes?.let { "${it / 60} h ${it % 60} min" } ?: if (hasSleepPermission) "Sin registros" else "Sin permiso"}")
                            }
                        }
                    }
                    if (result.permissions.containsAll(HealthRepository.permissions) && result.days.all { it.steps == null && it.sleepMinutes == null }) {
                        Text("Los permisos están activos, pero Health Connect no devolvió registros para estos siete días. Revisa que los datos estén guardados en Health Connect con una fecha dentro del periodo y toca Actualizar.")
                    }
                    Text("La ausencia de registros no equivale a cero pasos o cero horas de sueño.")
                }
                TextButton(onClick = {
                    scope.launch {
                        try { HealthConnectClient.getOrCreate(context).permissionController.revokeAllPermissions(); refresh++ }
                        catch (_: Exception) { error = "No se pudo revocar el acceso. Usa los ajustes de Health Connect." }
                    }
                }) { Text("Revocar acceso de MoodWave") }
            }
        }
    }
}
