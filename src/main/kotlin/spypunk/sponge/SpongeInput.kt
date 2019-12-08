/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.google.common.net.InternetDomainName
import java.io.File
import java.net.URI

class SpongeInput(
        val uri: URI,
        val outputDirectory: File,
        val mimeTypes: Set<String>,
        val maxDepth: Int = 1,
        val includeSubdomains: Boolean = false
) {
    val domain = uri.domain()
    val topPrivateDomain: InternetDomainName = domain.topPrivateDomain()
}

fun URI.domain(): InternetDomainName = InternetDomainName.from(host)
