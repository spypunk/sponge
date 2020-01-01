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

class SpongeUri private constructor(private val uri: String) {
    var children = setOf<SpongeUri>()

    fun toUri() = URI(uri)

    override fun toString() = uri

    override fun equals(other: Any?) = other is SpongeUri && uri == other.uri

    override fun hashCode() = uri.hashCode()

    companion object {
        private val supportedSchemes = setOf("http", "https")

        operator fun invoke(uri: String): SpongeUri {
            return URL(uri)
                .let { URI(it.protocol, it.userInfo, it.host, it.port, it.path, it.query, null) }
                .normalize()
                .let {
                    if (!supportedSchemes.contains(it.scheme)) error("Unsupported scheme: ${it.scheme}")
                    if (it.host.isNullOrEmpty()) error("Hostname cannot be empty")

                    SpongeUri(it.toASCIIString())
                }
        }
    }
}

fun String.toSpongeUri() = SpongeUri(this)
