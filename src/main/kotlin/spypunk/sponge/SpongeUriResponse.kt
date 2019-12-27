/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

interface SpongeUriResponse {
    val children
        get() = setOf<SpongeUri>()
}

object DownloadSpongeUriResponse : SpongeUriResponse
object IgnoreSpongeUriResponse : SpongeUriResponse

class VisitSpongeUriResponse(override val children: Set<SpongeUri>) : SpongeUriResponse
