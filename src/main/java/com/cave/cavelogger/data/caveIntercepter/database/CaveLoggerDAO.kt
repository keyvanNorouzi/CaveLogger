package com.cave.cavelogger.data.caveIntercepter.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CaveLoggerDAO {
    @Query("SELECT * FROM caveLoggerTable ORDER BY id DESC")
    fun getAllLogs(): LiveData<List<CaveLogger>?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLog(caveLogger: CaveLogger)

    @Query("UPDATE caveLoggerTable SET statusCode =:statusCode, response =:response, endTime = :endTime where url =:url AND method = :method")
    fun updateLog(url: String, method: String, statusCode: Int, response: String? = null, endTime: Long)

    @Delete
    fun deleteLog(caveLogger: CaveLogger)
}
