/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import java.nio.file.Path

const val DEFAULT_REFERRER = "https://www.google.com"
const val DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/80.0.3987.132 Safari/537.36"

const val DEFAULT_OVERWRITE_EXISTING_FILES = false

data class SpongeServiceConfig(
    val outputDirectory: Path,
    val referrer: String = DEFAULT_REFERRER,
    val userAgent: String = DEFAULT_USER_AGENT,
    val overwriteExistingFiles: Boolean = DEFAULT_OVERWRITE_EXISTING_FILES
)
