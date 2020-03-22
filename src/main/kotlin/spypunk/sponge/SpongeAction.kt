/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

val doNothingSpongeAction = object : SpongeAction {
    override suspend fun execute(spongeUri: SpongeUri, parents: Set<SpongeUri>) {}
}

interface SpongeAction {
    suspend fun execute(spongeUri: SpongeUri, parents: Set<SpongeUri>)
}
