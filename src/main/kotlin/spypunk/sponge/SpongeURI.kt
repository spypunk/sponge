/**
 * Copyright © 2019-2023 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import java.net.URI
import java.net.URL

private val supportedSchemes = setOf("http", "https")

fun String.toSpongeURI() = SpongeURI(this)

data class SpongeURI(val uri: String, val host: String, val path: String) {
    override fun toString() = uri
    override fun equals(other: Any?) = other is SpongeURI && uri == other.uri
    override fun hashCode() = uri.hashCode()

    companion object {
        operator fun invoke(input: String): SpongeURI {
            val url = URL(input)
            val uri = URI(url.protocol, url.userInfo, url.host, url.port, url.path, url.query, null)
                .normalize()

            if (!supportedSchemes.contains(uri.scheme)) error("Unsupported scheme: ${uri.scheme}")
            if (uri.host.isNullOrEmpty()) error("Hostname cannot be empty")

            return SpongeURI(uri.toASCIIString(), uri.host, uri.path)
        }
    }
}
