package com.example.moodwave

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moodwave.analytics.SyntheticDataGenerator
import com.example.moodwave.data.MoodBackup
import com.example.moodwave.util.PdfReportGenerator
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class MonthlyExportTest {
    @Test fun monthlyPdfReopensAndBackupRoundTrips() {
        val rows = SyntheticDataGenerator.generate(LocalDate.of(2024, 3, 1))
        val restored = MoodBackup.read(MoodBackup.export(rows).byteInputStream())
        assertEquals(rows.map { it.copy(id = 0) }, restored)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.moodwave.data.MoodDatabase::class.java).build()
        try {
            kotlinx.coroutines.runBlocking {
                assertEquals(rows.size, db.moodDao().restore(restored))
                assertEquals(0, db.moodDao().restore(restored))
                assertEquals(rows.size, db.moodDao().snapshot().size)
            }
        } finally {
            db.close()
        }
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val file = File.createTempFile("monthly-", ".pdf", dir)
        try {
            file.outputStream().use { PdfReportGenerator.write(it, restored, YearMonth.of(2024, 2), true) }
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let {
                file.copyTo(File(it, "Febrero-demo.pdf"), overwrite = true)
            }
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use {
                assertEquals(1, it.pageCount)
                it.openPage(0).use { page -> assertEquals(595, page.width) }
            }
        } finally {
            file.delete()
        }
    }
}
