package com.example.moodwave.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moodwave.data.MoodDao
import com.example.moodwave.model.MoodEntry
import com.example.moodwave.analytics.MoodAnalytics
import com.example.moodwave.analytics.SyntheticDataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// El ViewModel recibe el DAO para poder darle órdenes a la base de datos
class MoodViewModel(private val dao: MoodDao) : ViewModel() {

    // Variable que contiene toda la lista de registros (se actualiza sola gracias a Flow)
    val allMoods: Flow<List<MoodEntry>> = dao.getAllMoods()

    // Función para guardar un registro (cumple el requisito de crear registro del MVP)
    fun saveMood(mood: MoodEntry) {
        viewModelScope.launch {
            dao.insertMood(mood)
        }
    }

    /** Registros 100% sintéticos: no sustituyen ni mezclan los registros personales. */
    fun generateDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.removeDemoMoods(MoodAnalytics.DEMO_PREFIX)
            dao.insertMoods(SyntheticDataGenerator.generate())
        }
    }

    fun clearDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.removeDemoMoods(MoodAnalytics.DEMO_PREFIX)
        }
    }

    suspend fun importBackup(rows: List<MoodEntry>): Int = withContext(Dispatchers.IO) {
        // Se anexan nuevos registros con IDs nuevos; jamás se reemplazan registros existentes.
        dao.restore(rows)
    }

    fun eraseAllData() {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteAllMoods() }
    }

    // Función para borrar un registro (cumple el requisito de eliminar registro del MVP)
    fun deleteMood(mood: MoodEntry) {
        viewModelScope.launch {
            dao.deleteMood(mood)
        }
    }
}

// Esta clase "Factory" es obligatoria para poder pasarle el DAO a nuestro ViewModel
class MoodViewModelFactory(private val dao: MoodDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MoodViewModel(dao) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}
