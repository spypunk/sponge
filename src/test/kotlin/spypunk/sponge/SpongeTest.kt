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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

private fun String.toSpongeUri() = SpongeUri(this)

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val outputDirectory = Paths.get("testOutput").toAbsolutePath()
    private val fileName = "test.txt"
    private val spongeDownload = SpongeDownload(1_000_000, 1_000_000_000)

    private val spongeConfig = SpongeConfig(
        "https://test.com".toSpongeUri(),
        outputDirectory,
        setOf(ContentType.TEXT_PLAIN.mimeType),
        setOf("png")
    )

    private val spongeConfigWithSubdomains = spongeConfig.copy(includeSubdomains = true)
    private val spongeConfigWithDepthTwo = spongeConfig.copy(maximumDepth = 2)

    @BeforeEach
    fun beforeEach() {
        FileUtils.deleteDirectory(outputDirectory.toFile())

        every { spongeService.download(any(), any()) } returns spongeDownload
    }

    @Test
    fun testEmptyDocument() {
        givenDocument(spongeConfig.spongeUri, "<html></html>")

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testUnsupportedDocument() {
        givenDocument(spongeConfig.spongeUri, "", ContentType.IMAGE_TIFF)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testDocumentWithLink() {
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    @Test
    fun testDocumentWithLinkAlreadyDownloaded() {
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        val downloadPath = getDownloadPath(fileUri)

        FileUtils.touch(downloadPath.toFile())

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify(exactly = 0) { spongeService.download(fileUri.uri, downloadPath) }
    }

    @Test
    fun testDocumentWithLinkAlreadyDownloadedAndOverwrite() {
        val config = spongeConfig.copy(overwriteExistingFiles = true)
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            config.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        val downloadPath = getDownloadPath(fileUri)

        FileUtils.touch(downloadPath.toFile())

        executeSponge(config)

        verify { spongeService.request(config.spongeUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, downloadPath) }
    }

    @Test
    fun testDocumentWithLinkAndImage() {
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()
        val imageFileUri = "${spongeConfig.spongeUri}/test.png".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
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

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
        verify(exactly = 0) { spongeService.request(imageFileUri.uri) }
        verify { spongeService.download(imageFileUri.uri, getDownloadPath(imageFileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainDisabled() {
        val fileUri = "https://www.test.test.com/$fileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify(exactly = 0) { spongeService.request(fileUri.uri) }
        verify(exactly = 0) { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainEnabled() {
        val fileUri = "https://test.test.com/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithSubdomains.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfigWithSubdomains)

        verify { spongeService.request(spongeConfigWithSubdomains.spongeUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    @Test
    fun testDocumentWithIgnoredLinkAndSubdomainEnabled() {
        val fileUri = "https://test.test2.com/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithSubdomains.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$fileUri" />
                        </body>
                    </html>
                """
        )

        givenFile(fileUri)

        executeSponge(spongeConfigWithSubdomains)

        verify { spongeService.request(spongeConfigWithSubdomains.spongeUri.uri) }
        verify(exactly = 0) { spongeService.request(fileUri.uri) }
    }

    @Test
    fun testDocumentWithChildDocumentAndLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeConfigWithDepthTwo.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithDepthTwo.spongeUri,
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

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeUri.uri) }
        verify { spongeService.request(childDocumentUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    @Test
    fun testDocumentWithChildDocumentAndDuplicateLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeConfigWithDepthTwo.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithDepthTwo.spongeUri,
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

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeUri.uri) }
        verify { spongeService.request(childDocumentUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToOneParent() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()

        givenDocument(
            spongeConfigWithDepthTwo.spongeUri,
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
                           <a href="${spongeConfigWithDepthTwo.spongeUri}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeUri.uri) }
        verify { spongeService.request(childDocumentUri.uri) }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToDirectParent() {
        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="${spongeConfig.spongeUri}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
    }

    @Test
    fun testDocumentWithTooDeepChildDocumentAndLink() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
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

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify { spongeService.request(childDocumentUri.uri) }
        verify(exactly = 0) { spongeService.request(fileUri.uri) }
        verify(exactly = 0) { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    @Test
    fun testDocumentWithIgnoredChildDocument() {
        val childDocumentUri = "https://test2.com".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childDocumentUri" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify(exactly = 0) { spongeService.request(childDocumentUri.uri) }
    }

    @Test
    fun testDocumentWithInvalidChildUri() {
        val childUri = "http://"

        givenDocument(
            spongeConfig.spongeUri,
            """
                    <html>
                        <body>
                            <a href="$childUri" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify { spongeService.request(any()) }
    }

    @Test
    fun testDocumentWithLinkAndFailedConnection() {
        val fileUri = "${spongeConfig.spongeUri}/$fileName".toSpongeUri()
        val failingFileName = "test2.txt"
        val failingFileUri = "${spongeConfig.spongeUri}/$failingFileName".toSpongeUri()

        givenDocument(
            spongeConfig.spongeUri,
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

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeUri.uri) }
        verify { spongeService.request(failingFileUri.uri) }
        verify(exactly = 0) { spongeService.download(failingFileUri.uri, getDownloadPath(failingFileUri)) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    @Test
    fun testDocumentWithLimitedVisitedLinks() {
        val otherConfig = spongeConfig.copy(maximumUris = 2)
        val fileUri = "${otherConfig.spongeUri}/$fileName".toSpongeUri()
        val otherFileUri = "${otherConfig.spongeUri}/test2.txt".toSpongeUri()

        givenDocument(
            otherConfig.spongeUri,
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

        executeSponge(otherConfig)

        verify { spongeService.request(otherConfig.spongeUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }

        verify(exactly = 0) { spongeService.request(otherFileUri.uri) }
        verify(exactly = 0) { spongeService.download(otherFileUri.uri, getDownloadPath(otherFileUri)) }
    }

    @Test
    fun testDocumentWithDuplicateDownloads() {
        val childDocumentUri = "https://test.com/test".toSpongeUri()
        val fileUri = "${spongeConfigWithDepthTwo.spongeUri}/$fileName".toSpongeUri()

        givenDocument(
            spongeConfigWithDepthTwo.spongeUri,
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

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeUri.uri) }
        verify { spongeService.request(childDocumentUri.uri) }
        verify { spongeService.request(fileUri.uri) }
        verify { spongeService.download(fileUri.uri, getDownloadPath(fileUri)) }
    }

    private fun givenDocument(
        spongeUri: SpongeUri,
        htmlContent: String,
        contentType: ContentType = ContentType.TEXT_HTML
    ) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType.mimeType
        every { response.body() } returns htmlContent
        every { response.url() } returns URL(spongeUri.uri)

        every { spongeService.request(spongeUri.uri) } returns response
    }

    private fun givenFile(spongeUri: SpongeUri, mimeType: String = ContentType.TEXT_PLAIN.mimeType) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns mimeType
        every { spongeService.request(spongeUri.uri) } returns response
    }

    private fun givenUriFailsConnection(spongeUri: SpongeUri) {
        every { spongeService.request(spongeUri.uri) } throws IOException("Error!")
    }

    private fun executeSponge(spongeConfig: SpongeConfig) {
        Sponge(spongeService, spongeConfig).execute()
    }

    private fun getDownloadPath(spongeUri: SpongeUri): Path {
        return outputDirectory.resolve(spongeUri.host)
            .resolve(FilenameUtils.getPath(spongeUri.path))
            .resolve(FilenameUtils.getName(spongeUri.path))
            .toAbsolutePath()
    }
}
