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

fun String.toSpongeUri() = SpongeUri(this)

class SpongeUri private constructor(private val uri: String) {
    var children = setOf<SpongeUri>()

    fun toUri() = URI(uri)

    override fun toString() = uri

    override fun equals(other: Any?) = other is SpongeUri && uri == other.uri

    override fun hashCode() = uri.hashCode()

    companion object {
        operator fun invoke(value: String): SpongeUri {
            val url = URL(value)
            val uri = URI(url.protocol, url.userInfo, url.host, url.port, url.path, url.query, null)
                .normalize()

            if (!supportedSchemes.contains(uri.scheme)) error("Unsupported scheme: ${uri.scheme}")
            if (uri.host.isNullOrEmpty()) error("Hostname cannot be empty")

            return SpongeUri(uri.toASCIIString())
        }
    }
}
