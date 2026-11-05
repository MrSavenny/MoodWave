package com.example.moodwave.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moodwave.model.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries")
    suspend fun snapshot(): List<MoodEntry>

    @androidx.room.Transaction
    suspend fun restore(rows: List<MoodEntry>): Int {
        val additions = missingBackupRows(rows, snapshot())
        if (additions.isNotEmpty()) insertMoods(additions)
        return additions.size
    }

    // Inserta un nuevo registro. Si ya existe uno con el mismo ID, lo reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoods(moods: List<MoodEntry>)

    // Marcador reservado para los registros sintéticos, sin tocar datos personales.
    @Query("DELETE FROM mood_entries WHERE substr(diaryText, 1, length(:prefix)) = :prefix")
    suspend fun removeDemoMoods(prefix: String): Int

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAllMoods(): Int

    // Actualiza un registro existente (cumple el requisito de editar registro)
    @Update
    suspend fun updateMood(mood: MoodEntry)

    // Elimina un registro (cumple el requisito de eliminar registro)
    @Delete
    suspend fun deleteMood(mood: MoodEntry)

    // Obtiene todos los registros ordenados por fecha de más reciente a más antiguo
    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    fun getAllMoods(): Flow<List<MoodEntry>>

    // Busca si ya hay un registro en una fecha específica
    @Query("SELECT * FROM mood_entries WHERE date = :date LIMIT 1")
    fun getMoodByDate(date: String): Flow<MoodEntry?>
}

/** Preserve repeated identical entries up to their multiplicity in the backup. */
internal fun missingBackupRows(rows: List<MoodEntry>, existing: List<MoodEntry>): List<MoodEntry> {
    val counts = existing.groupingBy { it.copy(id = 0) }.eachCount().toMutableMap()
    return rows.map { it.copy(id = 0) }.filter { entry ->
        val remaining = counts[entry] ?: 0
        if (remaining > 0) counts[entry] = remaining - 1
        remaining == 0
    }
}
