package com.example.moodwave
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moodwave.sync.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class CloudStoreIntegrationTest {
    @Test fun keystoreConfigurationIsEncryptedAndCanBeRemoved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = CloudConfig("revision-test","https://example.com/backups/","user","unique-secret-token","unique-backup-password")
        try {
            CloudStore.save(context,config)
            assertEquals(config,CloudStore.load(context))
            val bytes = File(context.noBackupFilesDir,"cloud-config").readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(bytes.contains(config.token)); assertFalse(bytes.contains(config.password))
        } finally { CloudStore.clear(context) }
        assertNull(CloudStore.load(context))
    }
}
