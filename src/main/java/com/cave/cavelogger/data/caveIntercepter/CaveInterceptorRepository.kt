package com.cave.cavelogger.data.caveIntercepter

import com.cave.cavelogger.data.caveIntercepter.database.CaveLogger
import kotlinx.coroutines.flow.Flow
import okhttp3.Response

interface CaveInterceptorRepository {
    fun saveHeader(caveInterceptorDTO: CaveInterceptorDTO)
    fun updateResponse(response: Response)
    fun getLogs(): Flow<List<CaveLogger>?>
}
