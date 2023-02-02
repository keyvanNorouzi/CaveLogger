package com.cave.cavelogger.data.caveIntercepter

import androidx.lifecycle.asFlow
import com.cave.cavelogger.data.caveIntercepter.database.CaveLogger
import com.cave.cavelogger.data.caveIntercepter.database.CaveLoggerDAO
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.Flow
import okhttp3.Response
import okhttp3.logging.internal.isProbablyUtf8
import okio.Buffer
import okio.GzipSource

class CaveInterceptorRepositoryImp(
    private val caveLoggerDAO: CaveLoggerDAO,
    private val externalFilesDir: String
) : CaveInterceptorRepository {

    override fun saveHeader(caveInterceptorDTO: CaveInterceptorDTO) {
        val caveLogger = CaveLogger()
        caveLogger.apply {
            url = caveInterceptorDTO.url
            method = caveInterceptorDTO.method
            extraHeaders = caveInterceptorDTO.headers
            message = caveInterceptorDTO.message
            statusCode = caveInterceptorDTO.statusCode
            contentType = caveInterceptorDTO.contentType
            contentLength = caveInterceptorDTO.contentLength
            body = caveInterceptorDTO.body
            startTime = caveInterceptorDTO.startTime
        }
        caveLoggerDAO.addLog(caveLogger)
    }

    override fun getLogs(): Flow<List<CaveLogger>?> {
        return caveLoggerDAO.getAllLogs().asFlow()
    }

    override fun updateResponse(response: Response) {
        val endTime = System.currentTimeMillis()
        val folder = File(externalFilesDir, "avalakki")
        folder.mkdirs()
        val file = File(folder, "file_name.txt")
        val headers = response.headers
        val body = response.body?.let { responseBody ->
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // Buffer the entire body.
            var buffer = source.buffer
            var gzippedLength: Long? = null
            if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                gzippedLength = buffer.size
                GzipSource(buffer.clone()).use { gzippedResponseBody ->
                    buffer = Buffer()
                    buffer.writeAll(gzippedResponseBody)
                }
            }
            val contentLength = responseBody.contentLength()
            val contentType = responseBody.contentType()
            val charset: Charset =
                contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            if (!buffer.isProbablyUtf8()) {
                "<-- END HTTP (binary ${buffer.size}-byte body omitted)"
            } else if (contentLength != 0L) {
                buffer.clone().readString(charset)
            } else {
                null
            }
        }

        caveLoggerDAO.updateLog(
            response.request.url.toString(),
            response.request.method,
            response.code,
            body,
            endTime = endTime
        )
    }
}
