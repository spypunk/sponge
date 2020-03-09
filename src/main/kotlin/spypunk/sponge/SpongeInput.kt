/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import java.nio.file.Path

data class SpongeInput(
    val spongeUri: SpongeUri,
    val outputDirectory: Path,
    val mimeTypes: Set<String>,
    val fileExtensions: Set<String>,
    val maximumDepth: Int = 1,
    val maximumUris: Int = Int.MAX_VALUE,
    val includeSubdomains: Boolean = false,
    val concurrentRequests: Int = 1,
    val concurrentDownloads: Int = 1,
    val overwriteExistingFiles: Boolean = false
)
