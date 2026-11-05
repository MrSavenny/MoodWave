package com.example.moodwave.data

import java.io.InputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable v1 envelope: MWBK, version, PBKDF2 iterations, salt, nonce, ciphertext and GCM tag. */
object EncryptedBackup {
    const val MAX_PLAINTEXT_BYTES = 15_000_000
    private const val ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val HEADER_BYTES = 4 + 1 + 4 + SALT_BYTES + NONCE_BYTES
    private const val TAG_BYTES = 16
    private val magic = byteArrayOf(77, 87, 66, 75)

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size >= magic.size && bytes.copyOfRange(0, magic.size).contentEquals(magic)

    fun read(stream: InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val size = stream.read(buffer)
            if (size < 0) break
            require(output.size() + size <= MAX_PLAINTEXT_BYTES + HEADER_BYTES + TAG_BYTES) {
                "El respaldo supera el límite de 15 MB."
            }
            output.write(buffer, 0, size)
        }
        return output.toByteArray()
    }

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(password.size in 12..128) { "Usa una contraseña de 12 a 128 caracteres." }
        require(plain.size <= MAX_PLAINTEXT_BYTES) { "El respaldo supera el límite de 15 MB." }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES).put(magic).put(1.toByte())
            .putInt(ITERATIONS).put(salt).put(nonce).array()
        return header + crypt(Cipher.ENCRYPT_MODE, plain, password, salt, nonce, header)
    }

    fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
        require(bytes.size in (HEADER_BYTES + TAG_BYTES)..(MAX_PLAINTEXT_BYTES + HEADER_BYTES + TAG_BYTES)) {
            "El respaldo está incompleto o supera el tamaño permitido."
        }
        require(password.size in 1..128) { "Contraseña no válida." }
        val buffer = ByteBuffer.wrap(bytes)
        val prefix = ByteArray(4).also { buffer.get(it) }
        require(prefix.contentEquals(magic)) { "No es un respaldo cifrado de MoodWave." }
        require(buffer.get().toInt() == 1 && buffer.int == ITERATIONS) { "Versión de respaldo no compatible." }
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val nonce = ByteArray(NONCE_BYTES).also { buffer.get(it) }
        val header = bytes.copyOfRange(0, HEADER_BYTES)
        return crypt(Cipher.DECRYPT_MODE, bytes.copyOfRange(HEADER_BYTES, bytes.size), password, salt, nonce, header)
    }

    private fun crypt(mode: Int, bytes: ByteArray, password: CharArray, salt: ByteArray,
                      nonce: ByteArray, header: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        val key = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
            finally { spec.clearPassword() }
        try {
            return Cipher.getInstance("AES/GCM/NoPadding").run {
                init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                updateAAD(header)
                doFinal(bytes)
            }
        } finally { key.fill(0) }
    }
}
