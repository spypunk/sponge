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
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val outputDirectory = Path.of("output").toAbsolutePath()

    private val spongeInput = SpongeInput(
            URI("https://www.test.com"),
            outputDirectory,
            setOf(ContentType.TEXT_PLAIN.mimeType)
    )

    @BeforeEach
    fun beforeEach() {
        Files.deleteIfExists(outputDirectory)
    }

    @Test
    fun testEmptyDocument() {
        val htmlContent = "<html></html>"

        every { spongeService.connect(spongeInput.uri) } returns
                response(htmlContent, spongeInput.uri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testDocumentWithTextLink() {
        val fileName = "test.txt"

        val htmlContent =
                """
                    <html>
                        <body>
                            <a href="/$fileName" />
                        </body>
                    </html>
                """

        every { spongeService.connect(spongeInput.uri) } returns
                response(htmlContent, spongeInput.uri)

        val fileUri = URI("${spongeInput.uri}/$fileName")
        val fileResponse = response(ContentType.TEXT_PLAIN.mimeType)

        every { spongeService.connect(fileUri) } returns fileResponse

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri) }
        verify(exactly = 1) { spongeService.connect(fileUri) }
        verify(exactly = 1) { spongeService.download(fileResponse, spongeInput.outputDirectory.resolve(fileName)) }
    }

    @Test
    fun testDocumentWithInvalidAndValidTextLink() {

    }

    private fun response(htmlContent: String, baseUri: URI): Connection.Response {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns ContentType.TEXT_HTML.mimeType
        every { response.parse() } returns Jsoup.parse(htmlContent, baseUri.toString())

        return response
    }

    private fun response(contentType: String): Connection.Response {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType

        return response
    }

    private fun executeSponge(spongeInput: SpongeInput) {
        Sponge(spongeService, spongeInput).execute()
    }
}
