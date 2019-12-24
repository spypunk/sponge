/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

interface SpongeUriResponse {
    val children: Set<SpongeUri>
}

abstract class EmptySpongeUriResponse : SpongeUriResponse {
    override val children
        get() = setOf<SpongeUri>()
}

object DownloadSpongeUriResponse : EmptySpongeUriResponse()
object IgnoreSpongeUriResponse : EmptySpongeUriResponse()

class VisitSpongeUriResponse(override val children: Set<SpongeUri>) : SpongeUriResponse
