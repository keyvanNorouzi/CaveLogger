package com.cave.cavelogger.data.caveIntercepter

import java.io.Serializable

data class CaveInterceptorDTO(
    var url: String? = null,
    var message: String? = null,
    var method: String? = null,
    var extraHeaders: String? = null,
    var statusCode: Int? = null,
    var contentType: String? = null,
    var contentLength: String? = null,
    var headers: HashMap<String, String>? = null,
    var body: String? = null,
    var startTime: Long ? = null
) : Serializable
