/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.google.common.escape.Escaper
import com.google.common.net.UrlEscapers
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils
import java.net.URI
import java.net.URL

private const val WWW_PREFIX = "www."

private val urlPathSegmentEscaper: Escaper = UrlEscapers.urlPathSegmentEscaper()

fun URI.normalizedHost(): String? {
    return when {
        host.isNullOrEmpty() -> null
        host.startsWith(WWW_PREFIX) -> host.substring(WWW_PREFIX.length)
        else -> host
    }
}

fun String.toUri(): URI {
    val url = URL(this)

    return URIBuilder()
            .apply {
                scheme = url.protocol
                host = url.host
                pathSegments = URLEncodedUtils.parsePathSegments(url.path).map {
                    urlPathSegmentEscaper.escape(it)
                }
                port = url.port
                userInfo = url.userInfo

                if (url.query != null) {
                    setCustomQuery(url.query)
                }
            }
            .build()
            .normalize()
}
