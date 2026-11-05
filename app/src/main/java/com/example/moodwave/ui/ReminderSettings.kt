package com.example.moodwave.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moodwave.reminders.DailyReminder
import java.text.DateFormatSymbols
import java.util.Locale

@Composable
fun ReminderSettings() {
    val context = LocalContext.current
    val prefs = remember { DailyReminder.preferences(context) }
    var enabled by rememberSaveable { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var hour by rememberSaveable { mutableStateOf(prefs.getInt("hour", 20)) }
    var minute by rememberSaveable { mutableStateOf(prefs.getInt("minute", 0)) }
    fun update(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean("enabled", value).putInt("hour", hour).putInt("minute", minute).apply()
        DailyReminder.schedule(context)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        update(granted)
        if (!granted) Toast.makeText(context, "Puedes permitir las notificaciones desde Ajustes de Android.", Toast.LENGTH_LONG).show()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text("Recordatorio diario", Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = {
                if (it && Build.VERSION.SDK_INT >= 33 && !DailyReminder.permitted(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else update(it)
            })
        }
        OutlinedButton(onClick = {
            TimePickerDialog(context, { _, h, m -> hour = h; minute = m; update(enabled) }, hour, minute, false).show()
        }) {
            val amPm = DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings[if (hour < 12) 0 else 1]
            val displayHour = (hour % 12).let { if (it == 0) 12 else it }
            Text("Hora: " + String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm))
        }
        Text("Android puede retrasar el aviso para ahorrar batería. La hora sigue la zona horaria del teléfono.")
        TextButton(onClick = {
            val message = if (DailyReminder.show(context)) "Notificación de prueba enviada"
                else "Las notificaciones están desactivadas en Android."
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }) { Text("Probar notificación") }
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("Permisos de notificación") }
    }
}
