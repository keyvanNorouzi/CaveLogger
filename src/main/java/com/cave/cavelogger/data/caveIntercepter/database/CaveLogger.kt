package com.cave.cavelogger.data.caveIntercepter.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.io.Serializable

@Entity(tableName = "caveLoggerTable")
data class CaveLogger(
    var url: String? = null,
    var message: String? = null,
    var method: String? = null,
    var statusCode: Int? = null,
    var contentType: String? = null,
    var contentLength: String? = null,
    var response: String? = null,
    var body: String? = null,
    @TypeConverters(MapTypeConverter::class)
    var extraHeaders: Map<String, String>? = null,
    var startTime: Long? = null,
    var endTime: Long? = null
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    val during: String
        get() = if (startTime != null && endTime != null) {
            (endTime!! - startTime!!).toString() + " mil"
        } else {
            ""
        }
}
