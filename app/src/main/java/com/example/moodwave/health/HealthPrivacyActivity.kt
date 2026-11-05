package com.example.moodwave.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moodwave.ui.theme.MoodWaveTheme

const val HEALTH_PRIVACY = "MoodWave solicita acceso de lectura a pasos y sesiones de sueño para mostrar tu resumen de los últimos siete días. Puedes conceder ambos permisos, solo uno o ninguno. Los datos se consultan en el teléfono al abrir esta pantalla. No se incluyen en los respaldos ni se envían a servidores. Puedes revocar el acceso desde Health Connect. El diario y las estadísticas emocionales funcionan aunque rechaces estos permisos."

class HealthPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoodWaveTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Privacidad de sueño y pasos", style = MaterialTheme.typography.headlineSmall)
                        Text(HEALTH_PRIVACY)
                        Button(onClick = { finish() }) { Text("Cerrar") }
                    }
                }
            }
        }
    }
}
