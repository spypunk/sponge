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
import java.nio.file.Path

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val outputDirectory = Path.of("output").toAbsolutePath()
    private val fileName = "test.txt"
    private val imageFileName = "test.png"

    private val spongeInput = SpongeInput(
        "https://test.com".toSpongeUri(),
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
        givenDocument(spongeInput.spongeUri, "<html></html>")

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testUnsupportedDocument() {
        givenDocument(spongeInput.spongeUri, "", ContentType.IMAGE_TIFF)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testDocumentWithLink() {
        val fileUri = "${spongeInput.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeInput.spongeUri,
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

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndImage() {
        val fileUri = "${spongeInput.spongeUri}/$fileName".toSpongeUri()
        val imageFileUri = "${spongeInput.spongeUri}/$imageFileName".toSpongeUri()

        givenDocument(
            spongeInput.spongeUri,
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

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
        verify(exactly = 1) { spongeService.request(imageFileUri) }
        verify(exactly = 1) { spongeService.download(imageFileUri, getOutputFilePath(imageFileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainDisabled() {
        val fileUri = "https://www.test.test.com/$fileName".toSpongeUri()

        givenDocument(
            spongeInput.spongeUri,
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

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 0) { spongeService.request(fileUri) }
        verify(exactly = 0) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainEnabled() {
        val fileUri = "https://test.test.com/$fileName".toSpongeUri()

        givenDocument(
            spongeInputWithSubdomains.spongeUri,
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

        verify(exactly = 1) { spongeService.request(spongeInputWithSubdomains.spongeUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }

        verify(exactly = 1) {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithChildDocumentAndLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeInputWithDepthTwo.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeInputWithDepthTwo.spongeUri,
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

        verify(exactly = 1) { spongeService.request(spongeInputWithDepthTwo.spongeUri) }
        verify(exactly = 1) { spongeService.request(childDocumentUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }

        verify(exactly = 1) {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithChildDocumentAndDuplicateLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeInputWithDepthTwo.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeInputWithDepthTwo.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
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

        verify(exactly = 1) { spongeService.request(spongeInputWithDepthTwo.spongeUri) }
        verify(exactly = 1) { spongeService.request(childDocumentUri) }
        verify(exactly = 1) { spongeService.request(fileUri) }

        verify(exactly = 1) {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithTooDeepChildDocumentAndLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeInput.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeInput.spongeUri,
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
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 1) { spongeService.request(childDocumentUri) }
        verify(exactly = 0) { spongeService.request(fileUri) }

        verify(exactly = 0) {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithIgnoredChildDocument() {
        val childDocumentUri = "https://test2.com".toSpongeUri()

        givenDocument(
            spongeInput.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 0) { spongeService.request(childDocumentUri) }
    }

    @Test
    fun testDocumentWithInvalidChildUri() {
        val childUri = "http://"

        givenDocument(
            spongeInput.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childUri" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeInput)

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 1) { spongeService.request(any()) }
    }

    @Test
    fun testDocumentWithLinkAndFailedConnection() {
        val fileUri = "${spongeInput.spongeUri}/$fileName".toSpongeUri()
        val failingFileName = "test2.txt"
        val failingFileUri = "${spongeInput.spongeUri}/$failingFileName".toSpongeUri()

        givenDocument(
            spongeInput.spongeUri,
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

        verify(exactly = 1) { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 1) { spongeService.request(failingFileUri) }

        verify(exactly = 0) {
            spongeService.download(failingFileUri, getOutputFilePath(failingFileUri))
        }

        verify(exactly = 1) { spongeService.request(fileUri) }
        verify(exactly = 1) { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
    }

    private fun getOutputFilePath(spongeUri: SpongeUri): Path {
        val uri = spongeUri.toUri()

        return spongeInput.outputDirectory
            .resolve(uri.host)
            .resolve(FilenameUtils.getPath(uri.path))
            .resolve(FilenameUtils.getName(uri.path))
    }

    private fun givenDocument(
        spongeUri: SpongeUri,
        htmlContent: String,
        contentType: ContentType = ContentType.TEXT_HTML
    ) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType.mimeType
        every { response.body() } returns htmlContent
        every { response.url() } returns spongeUri.toUri().toURL()

        every { spongeService.request(spongeUri) } returns response
    }

    private fun givenFile(spongeUri: SpongeUri, mimeType: String = ContentType.TEXT_PLAIN.mimeType) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns mimeType
        every { spongeService.request(spongeUri) } returns response
    }

    private fun givenUriFailsConnection(spongeUri: SpongeUri) {
        every { spongeService.request(spongeUri) } throws IOException("Error!")
    }

    private fun executeSponge(spongeInput: SpongeInput) {
        Sponge(spongeService, spongeInput).execute()
    }
}
