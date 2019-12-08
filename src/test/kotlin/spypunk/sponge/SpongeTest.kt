/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jsoup.Connection
import org.jsoup.helper.DataUtil
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets

class SpongeTest {
    private val spongeService = mockk<SpongeService>()

    @Test
    fun testDocumentWithoutLinks() {
        val spongeInput = SpongeInput(
                URI("https://www.test.com"),
                File("output"),
                setOf("text/plain")
        )

        every { spongeService.connect(spongeInput.uri) } returns
                response("text/html", "<html></html>", spongeInput.uri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    private fun response(contentType: String, content: String, uri: URI): Connection.Response {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType

        every { response.parse() } returns
                DataUtil.load(
                        ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8.name(),
                        uri.toString()
                )

        return response
    }

    private fun executeSponge(spongeInput: SpongeInput) {
        Sponge(spongeService, spongeInput).execute()
    }
}
