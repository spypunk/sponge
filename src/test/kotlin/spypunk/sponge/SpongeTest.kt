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
import org.apache.commons.io.FilenameUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.file.Path

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val outputDirectory = Path.of("output").toAbsolutePath()
    private val fileName = "test.txt"
    private val imageFileName = "test.png"
    private val inputUri = "https://test.com"

    private val spongeInput = SpongeInput(
        inputUri.toNormalizedUri(),
        outputDirectory,
        setOf(ContentType.TEXT_PLAIN.mimeType),
        setOf("png")
    )

    private val spongeInputWithSubdomains = spongeInput.copy(includeSubdomains = true)
    private val spongeInputWithDepthTwo = spongeInput.copy(maxDepth = 2)

    @BeforeEach
    fun beforeEach() {
        FileUtils.deleteDirectory(outputDirectory.toFile())
    }

    @Test
    fun testEmptyDocument() {
        givenDocument(inputUri, "<html></html>")

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(inputUri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testUnsupportedDocument() {
        givenDocument(inputUri, "", ContentType.IMAGE_TIFF)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(inputUri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testDocumentWithLink() {
        val fileUri = "${inputUri}/$fileName"

        givenDocument(
            inputUri,
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

        verify(exactly = 1) { spongeService.request(inputUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndImage() {
        val fileUri = "${inputUri}/$fileName"
        val imageFileUri = "${inputUri}/$imageFileName"

        givenDocument(
            inputUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                            <img src="$imageFileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)
        givenFile(imageFileUri, ContentType.IMAGE_PNG.mimeType)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(inputUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
        verify(exactly = 1) { spongeService.request(imageFileUri) }
        verify(exactly = 1) { spongeService.download(imageFileUri, getOutputFilePath(imageFileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainDisabled() {
        val fileUri = "https://www.test.test.com/$fileName"

        givenDocument(
            inputUri,
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

        verify(exactly = 1) { spongeService.request(inputUri) }
        verify(exactly = 0) { spongeService.request(fileUri) }
        verify(exactly = 0) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainEnabled() {
        val fileUri = "https://test.test.com/$fileName"

        givenDocument(
            spongeInputWithSubdomains.uri.toString(),
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

        verify(exactly = 1) { spongeService.request(spongeInputWithSubdomains.uri.toString()) }
        verify(exactly = 1) { spongeService.request(fileUri) }

        verify(exactly = 1) {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithChildDocumentAndLink() {
        val childDocumentUri = "https://test.com/test"
        val fileUri = "${spongeInputWithDepthTwo.uri}/$fileName"

        givenDocument(
            spongeInputWithDepthTwo.uri.toString(),
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

        verify(exactly = 1) { spongeService.request(spongeInputWithDepthTwo.uri.toString()) }
        verify(exactly = 1) { spongeService.request(childDocumentUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }

        verify(exactly = 1) {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithLinkAndFailedConnection() {
        val fileUri = "${inputUri}/$fileName"
        val failingFileName = "test2.txt"
        val failingFileUri = "${inputUri}/$failingFileName"

        givenDocument(
            inputUri,
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

        verify(exactly = 1) { spongeService.request(inputUri) }
        verify(exactly = 1) { spongeService.request(failingFileUri) }

        verify(exactly = 0) {
            spongeService.download(failingFileUri, getOutputFilePath(failingFileUri))
        }

        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
    }

    private fun getOutputFilePath(uri: String): Path {
        return URI(uri).let {
            spongeInput.outputDirectory
                .resolve(it.host)
                .resolve(FilenameUtils.getPath(it.path))
                .resolve(FilenameUtils.getName(it.path))
        }
    }

    private fun givenDocument(uri: String, htmlContent: String, contentType: ContentType = ContentType.TEXT_HTML) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType.mimeType
        every { response.body() } returns htmlContent
        every { response.url() } returns URL(uri)

        every { spongeService.request(uri) } returns response
    }

    private fun givenFile(uri: String, mimeType: String = ContentType.TEXT_PLAIN.mimeType) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns mimeType
        every { spongeService.request(uri) } returns response
    }

    private fun givenUriFailsConnection(uri: String) {
        every { spongeService.request(uri) } throws IOException("Error!")
    }

    private fun executeSponge(spongeInput: SpongeInput) {
        Sponge(spongeService, spongeInput).execute()
    }
}
