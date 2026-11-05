package com.example.moodwave

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moodwave.analytics.SyntheticDataGenerator
import com.example.moodwave.data.EncryptedBackup
import com.example.moodwave.data.MoodBackup
import com.example.moodwave.data.MoodDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EncryptedBackupIntegrationTest {
    @Test fun encryptedRoundTripAndLegacyImportPreserveRoomRecords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MoodDatabase::class.java).build()
        val password = "mi respaldo privado 2026".toCharArray()
        val entries = SyntheticDataGenerator.generate(LocalDate.of(2026, 11, 4))
        try {
            val json = MoodBackup.export(entries)
            val encrypted = EncryptedBackup.encrypt(json.toByteArray(Charsets.UTF_8), password)
            val decrypted = EncryptedBackup.decrypt(EncryptedBackup.read(encrypted.inputStream()), password)
            val restored = MoodBackup.decode(decrypted.toString(Charsets.UTF_8))
            assertEquals(entries, restored)
            assertEquals(90, database.moodDao().restore(restored))
            assertEquals(0, database.moodDao().restore(restored))
            assertEquals(0, database.moodDao().restore(MoodBackup.read(json.byteInputStream())))
            val snapshot = database.moodDao().snapshot()
            assertTrue(runCatching {
                val invalid = EncryptedBackup.decrypt(encrypted, "contraseña equivocada".toCharArray())
                database.moodDao().restore(MoodBackup.decode(invalid.toString(Charsets.UTF_8)))
            }.isFailure)
            assertEquals(snapshot, database.moodDao().snapshot())
        } finally { password.fill(0.toChar()); database.close() }
    }
}
