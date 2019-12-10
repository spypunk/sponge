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

private val urlPathSegmentEscaper: Escaper = UrlEscapers.urlPathSegmentEscaper()
private val urlFragmentEscaper: Escaper = UrlEscapers.urlFragmentEscaper()

fun String.toUri(): URI {
    val url = URL(this)

    return URIBuilder()
            .apply {
                scheme = url.protocol
                host = url.host
                pathSegments = URLEncodedUtils.parsePathSegments(url.path).map {
                    urlPathSegmentEscaper.escape(it)
                }

                if (url.ref != null) {
                    fragment = urlFragmentEscaper.escape(url.ref)
                }

                port = url.port
                userInfo = url.userInfo
            }
            .build()
}
