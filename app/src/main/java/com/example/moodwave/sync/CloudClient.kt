package com.example.moodwave.sync

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import java.io.StringReader

class CloudHttpException(val status: Int) : IOException("El servidor respondió HTTP $status.")
class CloudClient(private val config: CloudConfig, private val client: OkHttpClient = defaultClient) {
    companion object {
        val defaultClient = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(20,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).writeTimeout(30,TimeUnit.SECONDS).build()
        val filePattern = Regex("""MoodWave_[A-Za-z0-9_.-]+\.mwbackup""")
        fun validateUrl(value: String): String {
            val uri = URI(value.trim())
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) {
                "Escribe la URL HTTPS de una carpeta WebDAV, sin usuario ni contraseña en la dirección."
            }
            return value.trim().trimEnd('/') + "/"
        }
        fun retryable(status: Int) = status == 408 || status == 429 || status in 500..599
    }
    private fun request(name: String = ""): Request.Builder {
        require(name.isEmpty() || filePattern.matches(name)) { "Nombre de respaldo no válido." }
        return Request.Builder().url(config.url.trimEnd('/') + "/" + name)
            .header("Authorization", Credentials.basic(config.user, config.token, Charsets.UTF_8))
    }
    fun upload(name: String, bytes: ByteArray) {
        client.newCall(request(name).header("If-None-Match","*")
            .put(bytes.toRequestBody("application/octet-stream".toMediaType())).build()).execute().use { response ->
            if (response.code == 412) {
                check(download(name).contentEquals(bytes)) { "Ya existe un respaldo diferente con ese nombre. Se conservó el archivo remoto." }
            } else if (!response.isSuccessful) throw CloudHttpException(response.code)
        }
    }
    fun download(name: String): ByteArray {
        return client.newCall(request(name).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw CloudHttpException(response.code)
            requireNotNull(response.body).byteStream().use(com.example.moodwave.data.EncryptedBackup::read)
        }
    }
    fun list(): List<String> {
        val xml = """<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>"""
        return client.newCall(request().header("Depth","1").method("PROPFIND", xml.toRequestBody("application/xml".toMediaType())).build())
            .execute().use { response ->
                if (response.code != 207 && !response.isSuccessful) throw CloudHttpException(response.code)
                val bytes = requireNotNull(response.body).byteStream().use { stream ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val n = stream.read(buffer); if (n < 0) break
                        require(out.size() + n <= 1_000_000) { "La carpeta contiene demasiados archivos." }
                        out.write(buffer,0,n)
                    }
                    out.toByteArray()
                }
                val text = bytes.toString(Charsets.UTF_8)
                require(!text.uppercase().contains("<!DOCTYPE")) { "Respuesta XML no admitida." }
                val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true; isExpandEntityReferences = false }
                val builder = factory.newDocumentBuilder().apply { setEntityResolver { _, _ -> InputSource(StringReader("")) } }
                val nodes = builder.parse(bytes.inputStream()).getElementsByTagNameNS("*","href")
                (0 until nodes.length).mapNotNull {
                    runCatching { URI(nodes.item(it).textContent).path.substringAfterLast('/') }.getOrNull()
                }.filter(filePattern::matches).distinct().sortedDescending()
            }
    }
    fun delete(name: String) {
        client.newCall(request(name).delete().build()).execute().use {
            if (!it.isSuccessful && it.code != 404) throw CloudHttpException(it.code)
        }
    }
}

