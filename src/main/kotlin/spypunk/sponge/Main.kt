/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import kotlin.system.exitProcess

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val spongeService = SpongeService()

        try {
            SpongeCommand(spongeService).main(args)
        } catch (t: Throwable) {
            System.err.println("Unexpected error encountered: ${t.message}")
            exitProcess(1)
        } finally {
            spongeService.stop()
        }
    }
}
