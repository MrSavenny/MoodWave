package com.example.moodwave.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.moodwave.sync.*
import com.example.moodwave.data.*
import com.example.moodwave.model.MoodEntry
import kotlinx.coroutines.*
import java.util.UUID

@Composable
fun CloudScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var configured by remember { mutableStateOf(runCatching { CloudStore.load(context) }.getOrNull()) }
    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf(CloudStore.status(context).getString("message", "Sin servicio conectado") ?: "") }
    var files by remember { mutableStateOf<List<String>>(emptyList()) }
    var restore by remember { mutableStateOf<List<MoodEntry>?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        val prefs = CloudStore.status(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> message = p.getString("message", "") ?: "" }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun execute(action: suspend () -> Unit) {
        busy = true
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "No se pudo completar. Revisa la conexión, las credenciales y la contraseña del respaldo." }
            finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text("Respaldo en la nube", style = MaterialTheme.typography.headlineMedium)
        Text("Tu diario funciona sin nube. Si conectas una carpeta WebDAV, enviaremos únicamente respaldos cifrados. Los datos de Health Connect y tu foto de perfil no se incluyen.")
        Text(message)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        val config = configured
        if (config == null) {
            Text("Necesitas un servicio WebDAV y una carpeta existente con acceso de lectura y escritura.")
            OutlinedTextField(url, { url = it }, label = { Text("URL HTTPS de la carpeta WebDAV") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(user, { user = it }, label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            CloudSecret(token, { token = it }, "Contraseña de aplicación del servidor")
            CloudSecret(password, { if (it.length <= 128) password = it }, "Contraseña del respaldo (12 caracteres mínimo)")
            CloudSecret(repeat, { if (it.length <= 128) repeat = it }, "Repetir contraseña del respaldo")
            Text("Guarda la contraseña del respaldo: la necesitarás en otros dispositivos. MoodWave no puede recuperarla.")
            Row { Checkbox(consent, { consent = it }); Text("Autorizo enviar mi diario cifrado a este servidor cada día, cuando haya conexión.") }
            Button(enabled = !busy && consent && user.isNotBlank() && token.isNotBlank() && password.length >= 12 && password == repeat, onClick = {
                val validated = runCatching { CloudClient.validateUrl(url) }.getOrNull()
                if (validated == null) message = "Utiliza una URL HTTPS sin credenciales ni parámetros."
                else execute {
                    val value = CloudConfig(UUID.randomUUID().toString(),validated,user,token,password)
                    withContext(Dispatchers.IO) { CloudStore.save(context,value) }
                    configured = value; token = ""; password = ""; repeat = ""
                    CloudScheduler.enable(context); CloudScheduler.now(context)
                }
            }) { Text("Conectar y activar respaldo diario") }
        } else {
            Text("Carpeta: ${config.url}")
            Text("Se conservan versiones independientes. Una restauración añade las entradas que faltan sin reemplazar las actuales.")
            Button(enabled = !busy, onClick = { CloudScheduler.now(context) }) { Text("Respaldar ahora") }
            OutlinedButton(enabled = !busy, onClick = { execute {
                files = withContext(Dispatchers.IO) { CloudClient(config).list() }
                message = "${files.size} respaldos disponibles"
            } }) { Text("Ver respaldos del servidor") }
            TextButton(enabled = !busy, onClick = {
                CloudScheduler.disable(context); configured = null; files = emptyList(); consent = false
            }) { Text("Desconectar y detener envíos") }
            Text("Desconectar conserva los archivos remotos. Puedes eliminarlos individualmente aquí antes de desconectar.")
            files.take(100).forEach { name ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(name)
                    TextButton(enabled = !busy, onClick = { execute {
                        restore = withContext(Dispatchers.IO) {
                            val bytes = CloudClient(config).download(name)
                            val chars = config.password.toCharArray()
                            try {
                                val plain = EncryptedBackup.decrypt(bytes,chars)
                                try { MoodBackup.decode(plain.toString(Charsets.UTF_8)) } finally { plain.fill(0) }
                            } finally { chars.fill('\u0000'); bytes.fill(0) }
                        }
                    } }) { Text("Restaurar esta copia") }
                    TextButton(enabled = !busy, onClick = { deleting = name }) { Text("Eliminar del servidor") }
                } }
            }
            if (files.size > 100) Text("Se muestran las 100 copias más recientes. Las demás siguen en el servidor.")
        }
    }
    if (restore != null) AlertDialog(onDismissRequest = { restore = null }, title = { Text("Restaurar ${restore!!.size} registros") },
        text = { Text("Se añadirán los registros que faltan y se conservarán tus entradas actuales.") },
        confirmButton = { TextButton(onClick = {
            val rows = restore!!; restore = null
            execute { val count = withContext(Dispatchers.IO) { MoodDatabase.getDatabase(context).moodDao().restore(rows) }; message = "Restaurados $count registros" }
        }) { Text("Restaurar") } }, dismissButton = { TextButton(onClick = { restore = null }) { Text("Cancelar") } })
    if (deleting != null) AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Eliminar copia remota") },
        text = { Text("Se eliminará definitivamente ${deleting!!}. Tus entradas locales se conservan.") },
        confirmButton = { TextButton(onClick = {
            val name = deleting!!; deleting = null
            val config = configured ?: return@TextButton
            execute { withContext(Dispatchers.IO) { CloudClient(config).delete(name) }; files = files - name; message = "Copia remota eliminada" }
        }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } })
}

@Composable private fun CloudSecret(value: String, change: (String) -> Unit, label: String) {
    OutlinedTextField(value,change,label = { Text(label) },singleLine = true,visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,autoCorrectEnabled = false),modifier = Modifier.fillMaxWidth())
}
