package com.example.moodwave.sync

import android.content.Context
import androidx.work.*
import com.example.moodwave.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import java.time.Instant

object CloudScheduler {
    private const val TAG = "cloud-backup"
    fun enable(context: Context) {
        val work = PeriodicWorkRequestBuilder<CloudWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).addTag(TAG).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG,ExistingPeriodicWorkPolicy.UPDATE,work)
    }
    fun now(context: Context) {
        val work = OneTimeWorkRequestBuilder<CloudWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).addTag(TAG).build()
        WorkManager.getInstance(context).enqueueUniqueWork("$TAG-now",ExistingWorkPolicy.KEEP,work)
        CloudStore.status(context).edit().putString("message","En espera de conexión").apply()
    }
    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        CloudStore.clear(context)
        CloudStore.status(context).edit().putString("message","Desconectado. Los archivos del servidor se conservan.").apply()
    }
}

class CloudWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context,params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = CloudStore.status(applicationContext)
        val file = File(applicationContext.noBackupFilesDir,"cloud-$id.payload")
        val nameFile = File(applicationContext.noBackupFilesDir,"cloud-$id.name")
        var preserveForRetry = false
        try {
            val config = CloudStore.load(applicationContext) ?: return@withContext Result.success()
            if (!file.exists() || !nameFile.exists() || nameFile.readLines().firstOrNull() != config.revision) {
                val rows = MoodDatabase.getDatabase(applicationContext).moodDao().snapshot()
                val plain = MoodBackup.export(rows).toByteArray(Charsets.UTF_8)
                val password = config.password.toCharArray()
                try { file.writeBytes(EncryptedBackup.encrypt(plain,password)) }
                finally { plain.fill(0); password.fill(0.toChar()) }
                nameFile.writeText(config.revision + "\nMoodWave_${System.currentTimeMillis()}_$id.mwbackup")
            }
            if (CloudStore.load(applicationContext)?.revision != config.revision) return@withContext Result.success()
            prefs.edit().putString("message","Enviando respaldo cifrado").apply()
            CloudClient(config).upload(nameFile.readLines().last(),file.readBytes())
            if (CloudStore.load(applicationContext)?.revision == config.revision) {
                prefs.edit().putString("message","Último respaldo enviado: ${Instant.now()}").putString("lastFile",nameFile.readLines().last()).apply()
            }
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            val retry = runAttemptCount < 4 && (e is java.io.IOException && (e !is CloudHttpException || CloudClient.retryable(e.status)))
            preserveForRetry = retry
            prefs.edit().putString("message", if (retry) "Conexión interrumpida. Se reintentará automáticamente." else "No se pudo sincronizar. Revisa la conexión, la carpeta y las credenciales.").apply()
            if (retry) Result.retry() else Result.failure()
        } finally {
            if (!preserveForRetry) { file.delete(); nameFile.delete() }
        }
    }
}

