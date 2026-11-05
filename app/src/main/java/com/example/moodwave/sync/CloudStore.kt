package com.example.moodwave.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CloudConfig(val revision: String, val url: String, val user: String, val token: String, val password: String)

object CloudStore {
    private const val ALIAS = "moodwave-cloud-settings"
    fun status(context: Context) = context.getSharedPreferences("cloud_status", Context.MODE_PRIVATE)
    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "cloud-config"))
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    @Synchronized fun save(context: Context, config: CloudConfig) {
        val data = JSONObject().put("revision", config.revision).put("url", config.url).put("user", config.user)
            .put("token", config.token).put("password", config.password).toString().toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val target = file(context)
        val stream = target.startWrite()
        try { stream.write(cipher.iv + cipher.doFinal(data)); target.finishWrite(stream) }
        catch (e: Exception) { target.failWrite(stream); throw e }
        finally { data.fill(0) }
    }
    @Synchronized fun load(context: Context): CloudConfig? {
        val source = file(context)
        if (!source.baseFile.exists()) return null
        val bytes = source.readFully()
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0,12))) }
        val plain = cipher.doFinal(bytes.copyOfRange(12, bytes.size))
        try {
            val data = JSONObject(plain.toString(Charsets.UTF_8))
            return CloudConfig(data.getString("revision"),data.getString("url"),data.getString("user"),data.getString("token"),data.getString("password"))
        } finally { plain.fill(0) }
    }
    @Synchronized fun clear(context: Context) { file(context).delete() }
}
