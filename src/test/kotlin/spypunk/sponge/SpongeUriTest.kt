/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SpongeUriTest {

    @Test
    fun testUnsupportedScheme() {
        try {
            SpongeUri("ftp://test.com")
        } catch (e: IllegalStateException) {
            Assertions.assertEquals("Unsupported scheme: ftp", e.message)
        }
    }

    @Test
    fun testEmptyHost() {
        try {
            SpongeUri("http:///www.test.com")
        } catch (e: IllegalStateException) {
            Assertions.assertEquals("Hostname cannot be empty", e.message)
        }
    }
}
