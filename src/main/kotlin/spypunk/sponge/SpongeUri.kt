/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import java.net.URI
import java.net.URL

private val supportedSchemes = setOf("http", "https")

class SpongeUri(input: String) {
    val uri: String
    val host: String
    val path: String

    var download = false
    var children: Set<SpongeUri> = setOf()

    init {
        val url = URL(input)

        URI(url.protocol, url.userInfo, url.host, url.port, url.path, url.query, null)
            .normalize()
            .let {
                if (!supportedSchemes.contains(it.scheme)) error("Unsupported scheme: ${it.scheme}")
                if (it.host.isNullOrEmpty()) error("Hostname cannot be empty")

                uri = it.toASCIIString()
                host = it.host
                path = it.path
            }
    }

    override fun toString() = uri
    override fun equals(other: Any?) = other is SpongeUri && uri == other.uri
    override fun hashCode() = uri.hashCode()
}
