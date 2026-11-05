package com.example.moodwave.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.moodwave.AppSession
import com.example.moodwave.data.EncryptedBackup
import com.example.moodwave.data.MoodBackup
import com.example.moodwave.data.MoodDatabase
import com.example.moodwave.model.MoodEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.crypto.AEADBadTagException

private data class BackupActions(
    val busy: Boolean,
    val export: (List<MoodEntry>, Boolean) -> Unit,
    val import: () -> Unit
)
private val LocalBackupActions = staticCompositionLocalOf<BackupActions> { error("Respaldo no disponible") }

@Composable
fun BackupButtons(entries: List<MoodEntry>) {
    val actions = LocalBackupActions.current
    Button(onClick = { actions.export(entries, true) }, enabled = !actions.busy, modifier = Modifier.fillMaxWidth()) {
        Text("Crear respaldo con contraseña")
    }
    OutlinedButton(onClick = actions.import, enabled = !actions.busy, modifier = Modifier.fillMaxWidth()) {
        Text("Restaurar respaldo")
    }
    TextButton(onClick = { actions.export(entries, false) }, enabled = !actions.busy) {
        Text("Exportar JSON sin cifrar")
    }
    Text("El respaldo incluye registros personales y de demostración. Puedes restaurar archivos con contraseña y los JSON anteriores.")
    if (actions.busy) Text("Procesando respaldo…")
}

/** Lives above navigation and the lock screen so file-picker results survive backgrounding. */
@Composable
fun BackupHost(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingFile by rememberSaveable { mutableStateOf<String?>(null) }
    var incomingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var exportRows by remember { mutableStateOf<List<MoodEntry>?>(null) }
    var exportEncrypted by remember { mutableStateOf(true) }
    var encryptedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importRows by remember { mutableStateOf<List<MoodEntry>?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun notify(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val file = pendingFile?.let { File(context.cacheDir, it) }
        if (file != null) {
            busy = true
            scope.launch {
                try {
                    if (uri != null) {
                        withContext(Dispatchers.IO) {
                            check(file.length() > 0) { "El respaldo preparado ya no está disponible. Créalo de nuevo." }
                            requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { out ->
                                file.inputStream().use { check(it.copyTo(out) == file.length()) }
                                out.flush()
                            }
                        }
                        notify("Respaldo guardado")
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { notify("No se pudo guardar: ${e.localizedMessage}") }
                finally { pendingFile = null; busy = false; file.delete() }
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            incomingUri = uri.toString()
        }
    }
    LaunchedEffect(incomingUri) {
        val uri = incomingUri?.let(Uri::parse) ?: return@LaunchedEffect
        busy = true
        try {
            val bytes = withContext(Dispatchers.IO) {
                requireNotNull(context.contentResolver.openInputStream(uri)).use(EncryptedBackup::read)
            }
            if (EncryptedBackup.isEncrypted(bytes)) {
                encryptedBytes = bytes
                passwordError = null
            } else {
                importRows = withContext(Dispatchers.Default) {
                    require(bytes.size <= EncryptedBackup.MAX_PLAINTEXT_BYTES) { "Archivo demasiado grande." }
                    MoodBackup.decode(bytes.toString(Charsets.UTF_8))
                }
                bytes.fill(0)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { notify("Respaldo inválido: ${e.localizedMessage}") }
        finally {
            incomingUri = null
            busy = false
            runCatching { context.contentResolver.releasePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    fun prepareExport(password: CharArray?) {
        val rows = exportRows ?: return
        exportRows = null
        busy = true
        scope.launch {
            val file = File(context.cacheDir, "backup-${java.util.UUID.randomUUID()}.bin")
            try {
                withContext(Dispatchers.Default) {
                    val plain = MoodBackup.export(rows).toByteArray(Charsets.UTF_8)
                    val bytes = try { if (password == null) plain else EncryptedBackup.encrypt(plain, password) }
                        finally { if (password != null) plain.fill(0) }
                    try { withContext(Dispatchers.IO) { file.writeBytes(bytes) } }
                    finally { bytes.fill(0) }
                }
                pendingFile = file.name
                save.launch("MoodWave_${LocalDate.now()}.${if (password == null) "json" else "mwbackup"}")
            } catch (e: CancellationException) { file.delete(); throw e }
            catch (e: Exception) { file.delete(); pendingFile = null; notify("No se pudo crear el respaldo: ${e.localizedMessage}") }
            finally { password?.fill('\u0000'); busy = false }
        }
    }

    val actions = BackupActions(busy || pendingFile != null || incomingUri != null || exportRows != null || encryptedBytes != null || importRows != null,
        export = { rows, encrypted -> exportRows = rows.toList(); exportEncrypted = encrypted; passwordError = null },
        import = { open.launch(arrayOf("*/*")) })
    CompositionLocalProvider(LocalBackupActions provides actions, content = content)

    // Passwords are remembered only while this dialog is visible, never in saved instance state.
    if (AppSession.isUnlocked) {
        if (exportRows != null && exportEncrypted) BackupPasswordDialog(
            title = "Proteger mi respaldo", creating = true, busy = busy, error = passwordError,
            onDismiss = { exportRows = null }, onConfirm = ::prepareExport
        )
        if (exportRows != null && !exportEncrypted) AlertDialog(
            onDismissRequest = { exportRows = null },
            title = { Text("Exportar sin contraseña") },
            text = { Text("El archivo JSON contiene tu diario en texto legible. Cualquier persona que lo tenga podrá leerlo.") },
            confirmButton = { TextButton(onClick = { prepareExport(null) }) { Text("Exportar JSON") } },
            dismissButton = { TextButton(onClick = { exportRows = null }) { Text("Cancelar") } }
        )
        if (encryptedBytes != null) BackupPasswordDialog(
            title = "Abrir respaldo", creating = false, busy = busy, error = passwordError,
            onDismiss = { if (!busy) { encryptedBytes = null; passwordError = null } },
            onConfirm = { password ->
                val bytes = encryptedBytes!!
                busy = true
                passwordError = null
                scope.launch {
                    try {
                        importRows = withContext(Dispatchers.Default) {
                            val plain = EncryptedBackup.decrypt(bytes, password)
                            try { MoodBackup.decode(plain.toString(Charsets.UTF_8)) } finally { plain.fill(0) }
                        }
                        encryptedBytes = null
                    } catch (e: CancellationException) { throw e }
                    catch (_: AEADBadTagException) { passwordError = "La contraseña no coincide o el archivo fue alterado." }
                    catch (e: Exception) { passwordError = e.localizedMessage ?: "No se pudo abrir el respaldo." }
                    finally { password.fill('\u0000'); busy = false }
                }
            }
        )
        if (importRows != null) AlertDialog(
            onDismissRequest = { importRows = null },
            title = { Text("Restaurar ${importRows!!.size} registros") },
            text = { Text("Se añadirán los registros que faltan. Se conservarán tus entradas actuales y se omitirán las copias idénticas.") },
            confirmButton = { TextButton(onClick = {
                val rows = importRows!!
                importRows = null
                busy = true
                scope.launch {
                    try {
                        val count = withContext(Dispatchers.IO) { MoodDatabase.getDatabase(context).moodDao().restore(rows) }
                        notify("Restaurados $count registros")
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { notify("No se pudo restaurar: ${e.localizedMessage}") }
                    finally { busy = false }
                }
            }) { Text("Restaurar") } },
            dismissButton = { TextButton(onClick = { importRows = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun BackupPasswordDialog(title: String, creating: Boolean, busy: Boolean, error: String?,
                                 onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (creating) "Elige una contraseña de al menos 12 caracteres. Guárdala en un lugar seguro. La necesitarás para restaurar en cualquier dispositivo y MoodWave no puede recuperarla."
                 else "Escribe la contraseña que elegiste al crear este archivo.")
            OutlinedTextField(value = password, onValueChange = { if (it.length <= 128) password = it },
                label = { Text("Contraseña del respaldo") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, autoCorrectEnabled = false),
                enabled = !busy, singleLine = true)
            if (creating) OutlinedTextField(value = repeat, onValueChange = { if (it.length <= 128) repeat = it },
                label = { Text("Repetir contraseña") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, autoCorrectEnabled = false),
                enabled = !busy, singleLine = true)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            if (busy) Text("Abriendo respaldo…")
        } },
        confirmButton = { TextButton(enabled = !busy && password.isNotEmpty() && (!creating || (password.length >= 12 && password == repeat)),
            onClick = { val chars = password.toCharArray(); password = ""; repeat = ""; onConfirm(chars) }) {
            Text(if (creating) "Crear respaldo" else "Desbloquear")
        } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
}
