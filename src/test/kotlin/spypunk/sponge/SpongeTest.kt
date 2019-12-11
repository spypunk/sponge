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
import org.apache.commons.io.FileUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Path

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val outputDirectory = Path.of("output").toAbsolutePath()
    private val fileName = "test.txt"

    private val spongeInput = SpongeInput(
            URI("https://www.test.com"),
            outputDirectory,
            setOf(ContentType.TEXT_PLAIN.mimeType)
    )

    private val spongeInputWithSubdomains = spongeInput.copy(includeSubdomains = true)
    private val spongeInputWithDepthTwo = spongeInput.copy(maxDepth = 2)


    @BeforeEach
    fun beforeEach() {
        FileUtils.deleteDirectory(outputDirectory.toFile())
    }

    @Test
    fun testEmptyDocument() {
        givenDocument(spongeInput.uri, "<html></html>")

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri.toString()) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testDocumentWithLink() {
        val fileUri = URI("${spongeInput.uri}/$fileName")

        givenDocument(
                spongeInput.uri,
                """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        val fileResponse = givenFile(fileUri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri.toString()) }
        verify(exactly = 1) { spongeService.connect(fileUri.toString()) }
        verify(exactly = 1) { spongeService.download(fileResponse, spongeInput.outputDirectory.resolve(fileName)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainDisabled() {
        val fileUri = URI("https://www.test.test.com/$fileName")

        givenDocument(
                spongeInput.uri,
                """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        val fileResponse = givenFile(fileUri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.connect(spongeInput.uri.toString()) }
        verify(exactly = 0) { spongeService.connect(fileUri.toString()) }
        verify(exactly = 0) { spongeService.download(fileResponse, spongeInput.outputDirectory.resolve(fileName)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainEnabled() {
        val fileUri = URI("https://www.test.test.com/$fileName")

        givenDocument(
                spongeInputWithSubdomains.uri,
                """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        val fileResponse = givenFile(fileUri)

        executeSponge(spongeInputWithSubdomains)

        verify(exactly = 1) { spongeService.connect(spongeInputWithSubdomains.uri.toString()) }
        verify(exactly = 1) { spongeService.connect(fileUri.toString()) }

        verify(exactly = 1) {
            spongeService.download(fileResponse, spongeInputWithSubdomains.outputDirectory.resolve(fileName))
        }
    }

    @Test
    fun testDocumentWithChildDocumentAndLink() {
        val childDocumentUri = URI("https://www.test.com/test")
        val fileUri = URI("${spongeInputWithDepthTwo.uri}/$fileName")

        givenDocument(
                spongeInputWithDepthTwo.uri,
                """
                    <html>
                        <body>
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        givenDocument(
                childDocumentUri,
                """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        val fileResponse = givenFile(fileUri)

        executeSponge(spongeInputWithDepthTwo)

        verify(exactly = 1) { spongeService.connect(spongeInputWithDepthTwo.uri.toString()) }
        verify(exactly = 1) { spongeService.connect(childDocumentUri.toString()) }
        verify(exactly = 1) { spongeService.connect(fileUri.toString()) }

        verify(exactly = 1) {
            spongeService.download(fileResponse, spongeInputWithDepthTwo.outputDirectory.resolve(fileName))
        }
    }

    private fun givenDocument(uri: URI, htmlContent: String) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns ContentType.TEXT_HTML.mimeType
        every { response.parse() } returns Jsoup.parse(htmlContent, uri.toString())

        every { spongeService.connect(uri.toString()) } returns response
    }

    private fun givenFile(uri: URI): Connection.Response {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns ContentType.TEXT_PLAIN.mimeType
        every { spongeService.connect(uri.toString()) } returns response

        return response
    }

    private fun executeSponge(spongeInput: SpongeInput) {
        Sponge(spongeService, spongeInput).execute()
    }
}
