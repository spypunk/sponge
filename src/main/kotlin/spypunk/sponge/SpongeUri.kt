/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.apache.http.client.utils.URIBuilder
import java.net.URI
import java.net.URL

class SpongeUri private constructor(private val uri: String) {
    companion object {
        private const val WWW_PREFIX = "www."

        private val supportedSchemes = setOf("http", "https")

        operator fun invoke(uri: String): SpongeUri {
            val url = URL(uri)

            return URIBuilder()
                .apply {
                    scheme = url.protocol
                    port = url.port
                    userInfo = url.userInfo
                    path = url.path

                    setCustomQuery(url.query)

                    host = if (url.host.startsWith(WWW_PREFIX)) {
                        url.host.substring(WWW_PREFIX.length)
                    } else {
                        url.host
                    }
                }
                .build()
                .normalize()
                .let {
                    if (!supportedSchemes.contains(it.scheme)) error("Unsupported scheme: ${it.scheme}")
                    if (it.host.isNullOrEmpty()) error("Hostname cannot be empty")

                    SpongeUri(it.toASCIIString())
                }
        }
    }

    override fun toString() = uri

    override fun equals(other: Any?): Boolean {
        return if (other is SpongeUri) {
            uri == other.uri
        } else {
            false
        }
    }

    override fun hashCode() = uri.hashCode()

    fun toUri() = URI(uri)
}

fun String.toSpongeUri() = SpongeUri(this)
