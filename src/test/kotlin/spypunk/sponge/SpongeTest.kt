/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.commons.io.FilenameUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
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
    private val spongeInputWithDepthTwo = spongeInput.copy(maximumDepth = 2)

    @BeforeEach
    fun beforeEach() {
        Files.deleteIfExists(outputDirectory)
    }

    @Test
    fun testEmptyDocument() {
        givenDocument(spongeInput.spongeUri, "<html></html>")

        executeSponge(spongeInput)

        verify { spongeService.request(spongeInput.spongeUri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testUnsupportedDocument() {
        givenDocument(spongeInput.spongeUri, "", ContentType.IMAGE_TIFF)

        executeSponge(spongeInput)

        verify { spongeService.request(spongeInput.spongeUri) }
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

        verify { spongeService.request(spongeInput.spongeUri) }
        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
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

        verify { spongeService.request(spongeInput.spongeUri) }
        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
        verify(exactly = 0) { spongeService.request(imageFileUri) }
        verify { spongeService.download(imageFileUri, getOutputFilePath(imageFileUri)) }
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

        verify { spongeService.request(spongeInput.spongeUri) }
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

        verify { spongeService.request(spongeInputWithSubdomains.spongeUri) }
        verify { spongeService.request(fileUri) }

        verify {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithIgnoredLinkAndSubdomainEnabled() {
        val fileUri = "https://test.test2.com/$fileName".toSpongeUri()

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

        verify { spongeService.request(spongeInputWithSubdomains.spongeUri) }
        verify(exactly = 0) { spongeService.request(fileUri) }
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

        verify { spongeService.request(spongeInputWithDepthTwo.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
        verify { spongeService.request(fileUri) }

        verify {
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

        verify { spongeService.request(spongeInputWithDepthTwo.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
        verify { spongeService.request(fileUri) }

        verify {
            spongeService.download(fileUri, getOutputFilePath(fileUri))
        }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToOneParent() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()

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
                           <a href="${spongeInputWithDepthTwo.spongeUri}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeInputWithDepthTwo)

        verify { spongeService.request(spongeInputWithDepthTwo.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToDirectParent() {
        givenDocument(
            spongeInput.spongeUri,
            """
                    <html>
                        <body>
                            <a href="${spongeInput.spongeUri}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeInput)

        verify { spongeService.request(spongeInput.spongeUri) }
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
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeInput)

        verify { spongeService.request(spongeInput.spongeUri) }
        verify { spongeService.request(childDocumentUri) }
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

        verify { spongeService.request(spongeInput.spongeUri) }
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

        verify { spongeService.request(spongeInput.spongeUri) }
        verify { spongeService.request(any()) }
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

        verify { spongeService.request(spongeInput.spongeUri) }
        verify { spongeService.request(failingFileUri) }

        verify(exactly = 0) {
            spongeService.download(failingFileUri, getOutputFilePath(failingFileUri))
        }

        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri, getOutputFilePath(fileUri)) }
    }

    @Test
    fun testDocumentWithLimitedVisitedLinks() {
        val input = spongeInput.copy(maximumUris = 2)
        val fileUri = "${input.spongeUri}/$fileName".toSpongeUri()
        val otherFileUri = "${input.spongeUri}/test2.txt".toSpongeUri()

        givenDocument(
            input.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                            <a href="$otherFileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)
        givenFile(otherFileUri)

        executeSponge(input)

        verify { spongeService.request(input.spongeUri) }
        verify { spongeService.request(fileUri) }
        verify { spongeService.download(fileUri, getOutputFilePath(fileUri)) }

        verify(exactly = 0) { spongeService.request(otherFileUri) }
        verify(exactly = 0) { spongeService.download(otherFileUri, getOutputFilePath(otherFileUri)) }
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
