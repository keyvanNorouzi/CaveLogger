package com.cave.cavelogger.list

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.cave.cavelogger.data.caveIntercepter.CaveInterceptorRepository
import com.cave.cavelogger.data.caveIntercepter.CaveInterceptorRepositoryImp
import com.cave.cavelogger.data.caveIntercepter.database.CaveLogger
import com.cave.cavelogger.data.caveIntercepter.database.CaveLoggerDAO
import com.cave.cavelogger.data.caveIntercepter.database.CaveLoggerDB
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class CaveLoggerViewModel(private val context: Context) :
    ViewModel(), CoroutineScope {

    fun provideDataBase(application: Context): CaveLoggerDB {
        return Room.databaseBuilder(application, CaveLoggerDB::class.java, "caveLoggerDB")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val caveLoggerDAO: CaveLoggerDAO
        get() =
            provideDataBase(context).caveLoggerDao

    fun provideDao(caveLoggerDB: CaveLoggerDB): CaveLoggerDAO {
        return caveLoggerDB.caveLoggerDao
    }

    private val externalFilesDir: String = ""

    private val caveLoggerRepository: CaveInterceptorRepository
        get() =
            CaveInterceptorRepositoryImp(caveLoggerDAO, externalFilesDir)

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val ioScope = CoroutineScope(Dispatchers.Default)

    private val job = Job()

    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    private val _logsData = MutableLiveData<List<CaveLogger>?>()
    val logsData: LiveData<List<CaveLogger>?> = _logsData

    fun getLogs() {
        viewModelScope.launch {
            caveLoggerRepository.getLogs().collect {
                _logsData.value = it
            }
        }
    }
}
