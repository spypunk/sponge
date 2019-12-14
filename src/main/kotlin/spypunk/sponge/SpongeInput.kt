/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import java.net.URI
import java.nio.file.Path

data class SpongeInput(
        val uri: URI,
        val outputDirectory: Path,
        val mimeTypes: Set<String>,
        val maxDepth: Int = 1,
        val includeSubdomains: Boolean = false,
        val concurrentRequests: Int = 1,
        val concurrentDownloads: Int = 1
)
