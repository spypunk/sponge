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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
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

        verify(exactly = 1) { spongeService.request(spongeInput.uri) }
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

        givenFile(fileUri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.uri) }
        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, spongeInput.outputDirectory.resolve(fileName)) }
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

        givenFile(fileUri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.uri) }
        verify(exactly = 0) { spongeService.request(fileUri) }
        verify(exactly = 0) { spongeService.download(fileUri, spongeInput.outputDirectory.resolve(fileName)) }
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

        givenFile(fileUri)

        executeSponge(spongeInputWithSubdomains)

        verify(exactly = 1) { spongeService.request(spongeInputWithSubdomains.uri) }
        verify(exactly = 1) { spongeService.request(fileUri) }

        verify(exactly = 1) {
            spongeService.download(fileUri, spongeInputWithSubdomains.outputDirectory.resolve(fileName))
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

        givenFile(fileUri)

        executeSponge(spongeInputWithDepthTwo)

        verify(exactly = 1) { spongeService.request(spongeInputWithDepthTwo.uri) }
        verify(exactly = 1) { spongeService.request(childDocumentUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }

        verify(exactly = 1) {
            spongeService.download(fileUri, spongeInputWithDepthTwo.outputDirectory.resolve(fileName))
        }
    }

    @Test
    fun testDocumentWithLinkAndFailedConnection() {
        val fileUri = URI("${spongeInput.uri}/$fileName")
        val failingFileName = "test2.txt"
        val failingFileUri = URI("${spongeInput.uri}/$failingFileName")

        givenDocument(
                spongeInput.uri,
                """
                    <html>
                        <body>
                            <a href="$failingFileUri" />
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenUriFailsConnection(failingFileUri)

        givenFile(fileUri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.uri) }
        verify(exactly = 1) { spongeService.request(failingFileUri) }

        verify(exactly = 0) {
            spongeService.download(failingFileUri, spongeInput.outputDirectory.resolve(failingFileName))
        }

        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, spongeInput.outputDirectory.resolve(fileName)) }
    }

    private fun givenDocument(uri: URI, htmlContent: String) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns ContentType.TEXT_HTML.mimeType
        every { response.body() } returns htmlContent
        every { response.url() } returns uri.toURL()

        every { spongeService.request(uri) } returns response
    }

    private fun givenFile(uri: URI) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns ContentType.TEXT_PLAIN.mimeType
        every { spongeService.request(uri) } returns response
    }

    private fun givenUriFailsConnection(uri: URI) {
        every { spongeService.request(uri) } throws IOException()
    }

    private fun executeSponge(spongeInput: SpongeInput) {
        Sponge(spongeService, spongeInput).execute()
    }
}
