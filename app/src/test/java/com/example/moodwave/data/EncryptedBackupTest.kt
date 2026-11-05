package com.example.moodwave.data

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException

class EncryptedBackupTest {
    private val password = "mi clave larga ñ 2026".toCharArray()
    private val plain = """{"diaryText":"Reflexión privada 🌸"}""".toByteArray(Charsets.UTF_8)

    @Test fun ciphertextHidesContentAndRoundTripsUnicode() {
        val first = EncryptedBackup.encrypt(plain, password)
        val second = EncryptedBackup.encrypt(plain, password)
        assertTrue(EncryptedBackup.isEncrypted(first))
        assertFalse(first.contentEquals(second))
        assertFalse(String(first, Charsets.UTF_8).contains("Reflexión"))
        assertArrayEquals(plain, EncryptedBackup.decrypt(first, password))
        assertArrayEquals(plain, EncryptedBackup.decrypt(second, password))
    }

    @Test fun incorrectPasswordAndAlteredPayloadAreRejected() {
        val bytes = EncryptedBackup.encrypt(plain, password)
        assertThrows(AEADBadTagException::class.java) { EncryptedBackup.decrypt(bytes, "otra contraseña".toCharArray()) }
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { EncryptedBackup.decrypt(bytes, password) }
    }

    @Test fun headerAndTruncationAreRejected() {
        val bytes = EncryptedBackup.encrypt(plain, password)
        val alteredSalt = bytes.copyOf().also { it[9] = (it[9].toInt() xor 1).toByte() }
        assertThrows(AEADBadTagException::class.java) { EncryptedBackup.decrypt(alteredSalt, password) }
        val version = bytes.copyOf().also { it[4] = 2 }
        assertThrows(IllegalArgumentException::class.java) { EncryptedBackup.decrypt(version, password) }
        val workFactor = bytes.copyOf().also { it[5] = 127 }
        assertThrows(IllegalArgumentException::class.java) { EncryptedBackup.decrypt(workFactor, password) }
        assertThrows(IllegalArgumentException::class.java) { EncryptedBackup.decrypt(bytes.copyOf(10), password) }
        assertThrows(AEADBadTagException::class.java) { EncryptedBackup.decrypt(bytes.copyOf(bytes.size - 1), password) }
    }

    @Test fun exportRejectsShortPasswordsAndOversizedContent() {
        assertThrows(IllegalArgumentException::class.java) { EncryptedBackup.encrypt(plain, "1234".toCharArray()) }
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackup.encrypt(ByteArray(EncryptedBackup.MAX_PLAINTEXT_BYTES + 1), password)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackup.read(ByteArray(EncryptedBackup.MAX_PLAINTEXT_BYTES + 54).inputStream())
        }
    }

    @Test fun legacyJsonIsNotMisidentifiedAsEncrypted() {
        assertFalse(EncryptedBackup.isEncrypted(plain))
        assertFalse(EncryptedBackup.isEncrypted(byteArrayOf()))
        assertArrayEquals(plain, EncryptedBackup.read(plain.inputStream()))
    }
}
