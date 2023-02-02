package com.cave.cavelogger.interceptor

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.cave.cavelogger.data.caveIntercepter.CaveInterceptorDTO
import com.cave.cavelogger.data.caveIntercepter.CaveInterceptorRepository
import com.cave.cavelogger.data.caveIntercepter.CaveInterceptorRepositoryImp
import com.cave.cavelogger.data.caveIntercepter.database.CaveLoggerDAO
import com.google.gson.Gson
import com.cave.cavelogger.data.caveIntercepter.database.CaveLoggerDB
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okhttp3.internal.platform.Platform
import okhttp3.logging.internal.isProbablyUtf8
import okio.Buffer
import okio.GzipSource
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.HashMap
import java.util.TreeSet
import java.util.concurrent.TimeUnit

/**
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * [application interceptor][OkHttpClient.interceptors] or as a [OkHttpClient.networkInterceptors].
 *
 * The format of the logs created by this class should not be considered stable and may
 * change slightly between releases. If you need a stable logging format, use your own interceptor.
 */
class CaveInterceptor @JvmOverloads constructor(
    private val context: Context,
    private val logger: Logger = Logger.DEFAULT
) : Interceptor {

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

    private val caveInterceptorRepository: CaveInterceptorRepository
        get() =
            CaveInterceptorRepositoryImp(caveLoggerDAO, externalFilesDir)

    private var currentHeaders = mutableSetOf<String>()

    @Volatile
    private var headersToRedact = emptySet<String>()

    @set:JvmName("level")
    @Volatile
    var level = Level.NONE

    enum class Level {
        /** No logs. */
        NONE,

        /**
         * Logs request and response lines.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * ```
         */
        BASIC,

        /**
         * Logs request and response lines and their respective headers.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * ```
         */
        HEADERS,

        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * ```
         */
        BODY
    }

    interface Logger {
        fun log(message: String)
        fun caveLog(
            caveInterceptorDTO: CaveInterceptorDTO,
            caveInterceptorRepository: CaveInterceptorRepository
        )

        companion object {
            /** A [Logger] defaults output appropriate for the current platform. */
            @JvmField
            val DEFAULT: Logger = DefaultLogger()

            private class DefaultLogger : Logger {
                override fun log(message: String) {
                    Platform.get().log(message)
                }

                override fun caveLog(
                    caveInterceptorDTO: CaveInterceptorDTO,
                    caveInterceptorRepository: CaveInterceptorRepository
                ) {
                    val gson = Gson()
                    caveInterceptorRepository.saveHeader(caveInterceptorDTO)
                    Log.i("KeyvanInterceptor", "${gson.toJson(caveInterceptorDTO)}")
                }
            }
        }
    }

    fun redactHeader(name: String) {
        val newHeadersToRedact = TreeSet(String.CASE_INSENSITIVE_ORDER)
        newHeadersToRedact += headersToRedact
        newHeadersToRedact += name
        headersToRedact = newHeadersToRedact
    }

    /**
     * Sets the level and returns this.
     *
     * This was deprecated in OkHttp 4.0 in favor of the [level] val. In OkHttp 4.3 it is
     * un-deprecated because Java callers can't chain when assigning Kotlin vals. (The getter remains
     * deprecated).
     */
    fun setLevel(level: Level) = apply {
        this.level = level
    }

    @JvmName("-deprecated_level")
    @Deprecated(
        message = "moved to var",
        replaceWith = ReplaceWith(expression = "level"),
        level = DeprecationLevel.ERROR
    )
    fun getLevel(): Level = level

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val level = this.level

        val request = chain.request()
        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val logBody = level == Level.BODY
        val logHeaders = logBody || level == Level.HEADERS

        val requestBody = request.body

        val connection = chain.connection()
        var extraHeaders = ""
        var requestStartMessage =
            ("--> ${request.method} ${request.url}${if (connection != null) " " + connection.protocol() else ""}")
        if (!logHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body) cave3"
            extraHeaders += " (${requestBody.contentLength()}-byte body) cave4"
        }
        logger.log(requestStartMessage)

        val caveLog = CaveInterceptorDTO()
        caveLog.apply {
            url = request.url.toString()
            method = request.method
            startTime = System.currentTimeMillis()
            this.extraHeaders = extraHeaders
        }

        currentHeaders.add(request.url.toString() + request.method)

        if (logHeaders) {
            val headers = request.headers

            if (requestBody != null) {
                // Request body headers are only present when installed as a network interceptor. When not
                // already present, force them to be included (if available) so their values are known.
                requestBody.contentType()?.let {
                    if (headers["Content-Type"] == null) {
                        logger.log("Content-Type: $it")
                        caveLog.contentType = it.toString()
                    }
                }
                if (requestBody.contentLength() != -1L) {
                    if (headers["Content-Length"] == null) {
                        logger.log("Content-Length: ${requestBody.contentLength()}")
                        caveLog.contentLength = requestBody.contentLength().toString()
                    }
                }
            }

            val hashMap = HashMap<String, String>()
            for (i in 0 until headers.size) {
                val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
                hashMap[headers.name(i)] = value
                logHeader(headers, i)
            }
            caveLog.headers = hashMap

            if (!logBody || requestBody == null) {
                logger.log("--> END ${request.method}")
            } else if (bodyHasUnknownEncoding(request.headers)) {
                logger.log("--> END ${request.method} (encoded body omitted)")
            } else if (requestBody.isDuplex()) {
                logger.log("--> END ${request.method} (duplex request body omitted)")
            } else if (requestBody.isOneShot()) {
                logger.log("--> END ${request.method} (one-shot body omitted)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                val contentType = requestBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                logger.log("")
                if (buffer.isProbablyUtf8()) {
                    val body = buffer.readString(charset)
                    logger.log(body)
                    caveLog.body = body
                    logger.log("--> END ${request.method} (${requestBody.contentLength()}-byte body) cave")
                } else {
                    logger.log(
                        "--> END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)"
                    )
                }
            }
            logger.caveLog(caveLog, caveInterceptorRepository)
        } // end of Header

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logger.log("<-- HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        logger.log(
            "<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logHeaders) ", $bodySize body" else ""}) caveeeeeeee"
        )

        val uniqeResponseForSearch = (response.request.url.toString() + response.request.method)

        if (logHeaders) {
            val headers = response.headers
            for (i in 0 until headers.size) {
                logHeader(headers, i)
            }

            if (!logBody || !response.promisesBody()) {
                logger.log("<-- END HTTP cave8")
            } else if (bodyHasUnknownEncoding(response.headers)) {
                logger.log("<-- END HTTP (encoded body omitted) cave9")
            } else {
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

                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                if (!buffer.isProbablyUtf8()) {
                    logger.log("")
                    logger.log("<-- END HTTP (binary ${buffer.size}-byte body omitted)")
                    return response
                }

                if (contentLength != 0L) {
                    logger.log("")
                    logger.log(buffer.clone().readString(charset))
                    if (currentHeaders.contains(uniqeResponseForSearch)) {
                        Log.i("keyvan", "contain")
                        caveInterceptorRepository.updateResponse(response)
                        currentHeaders.remove(uniqeResponseForSearch)
                    } else {
                        Log.i("keyvan", "not contain")
                    }
                }

                if (gzippedLength != null) {
                    logger.log("<-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body) cave5")
                } else {
                    logger.log("<-- END HTTP (${buffer.size}-byte body) cave2")
                }
            }
        }

        return response
    }

    private fun logHeader(headers: Headers, i: Int) {
        val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
        logger.log(headers.name(i) + ": " + value)
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
            !contentEncoding.equals("gzip", ignoreCase = true)
    }
}