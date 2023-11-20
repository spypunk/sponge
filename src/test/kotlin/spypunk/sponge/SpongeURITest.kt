/**
 * Copyright © 2019-2023 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SpongeURITest {
    @Test
    fun testUnsupportedScheme() {
        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            SpongeURI("ftp://test.com")
        }

        Assertions.assertEquals("Unsupported scheme: ftp", exception.message)
    }

    @Test
    fun testEmptyHost() {
        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            SpongeURI("http:///www.test.com")
        }

        Assertions.assertEquals("Hostname cannot be empty", exception.message)
    }
}
