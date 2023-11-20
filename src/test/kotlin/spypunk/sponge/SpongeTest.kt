/**
 * Copyright © 2019-2023 spypunk <spypunk@gmail.com>
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

class SpongeTest {
    private val spongeService = mockk<SpongeService>(relaxed = true)
    private val outputDirectory = Paths.get("testOutput").toAbsolutePath()
    private val fileName = "test.txt"
    private val spongeDownload = SpongeDownload(1_000_000, 1_000_000_000)

    private val spongeConfig = SpongeConfig(
        "https://test.com".toSpongeURI(),
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
        givenDocument(spongeConfig.spongeURI, "<html></html>")

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testUnsupportedDocument() {
        givenDocument(spongeConfig.spongeURI, "", ContentType.IMAGE_TIFF)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify(exactly = 0) { spongeService.download(any(), any()) }
    }

    @Test
    fun testDocumentWithLink() {
        val fileURI = "${spongeConfig.spongeURI}/$fileName".toSpongeURI()

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    @Test
    fun testDocumentWithLinkAlreadyDownloaded() {
        val fileURI = "${spongeConfig.spongeURI}/$fileName".toSpongeURI()

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        val downloadPath = getDownloadPath(fileURI)

        FileUtils.touch(downloadPath.toFile())

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify { spongeService.request(fileURI) }
        verify(exactly = 0) { spongeService.download(fileURI, downloadPath) }
    }

    @Test
    fun testDocumentWithLinkAlreadyDownloadedAndOverwrite() {
        val config = spongeConfig.copy(overwriteExistingFiles = true)
        val fileURI = "${spongeConfig.spongeURI}/$fileName".toSpongeURI()

        givenDocument(
            config.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        val downloadPath = getDownloadPath(fileURI)

        FileUtils.touch(downloadPath.toFile())

        executeSponge(config)

        verify { spongeService.request(config.spongeURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, downloadPath) }
    }

    @Test
    fun testDocumentWithLinkAndImage() {
        val fileURI = "${spongeConfig.spongeURI}/$fileName".toSpongeURI()
        val imageFileURI = "${spongeConfig.spongeURI}/test.png".toSpongeURI()

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                            <img src="$imageFileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)
        givenFile(imageFileURI, ContentType.IMAGE_PNG.mimeType)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }
        verify(exactly = 0) { spongeService.request(imageFileURI) }
        verify { spongeService.download(imageFileURI, getDownloadPath(imageFileURI)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainDisabled() {
        val fileURI = "https://www.test.test.com/$fileName".toSpongeURI()

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify(exactly = 0) { spongeService.request(fileURI) }
        verify(exactly = 0) { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    @Test
    fun testDocumentWithLinkAndSubdomainEnabled() {
        val fileURI = "https://test.test.com/$fileName".toSpongeURI()

        givenDocument(
            spongeConfigWithSubdomains.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfigWithSubdomains)

        verify { spongeService.request(spongeConfigWithSubdomains.spongeURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    @Test
    fun testDocumentWithIgnoredLinkAndSubdomainEnabled() {
        val fileURI = "https://test.test2.com/$fileName".toSpongeURI()

        givenDocument(
            spongeConfigWithSubdomains.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfigWithSubdomains)

        verify { spongeService.request(spongeConfigWithSubdomains.spongeURI) }
        verify(exactly = 0) { spongeService.request(fileURI) }
    }

    @Test
    fun testDocumentWithChildDocumentAndLink() {
        val childDocumentURI = "https://test.com/test".toSpongeURI()
        val fileURI = "${spongeConfigWithDepthTwo.spongeURI}/$fileName".toSpongeURI()

        givenDocument(
            spongeConfigWithDepthTwo.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$childDocumentURI" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeURI) }
        verify { spongeService.request(childDocumentURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    @Test
    fun testDocumentWithChildDocumentAndDuplicateLink() {
        val childDocumentURI = "https://test.com/test".toSpongeURI()
        val fileURI = "${spongeConfigWithDepthTwo.spongeURI}/$fileName".toSpongeURI()

        givenDocument(
            spongeConfigWithDepthTwo.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                            <a href="$childDocumentURI" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeURI) }
        verify { spongeService.request(childDocumentURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToOneParent() {
        val childDocumentURI = "https://test.com/test".toSpongeURI()

        givenDocument(
            spongeConfigWithDepthTwo.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$childDocumentURI" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentURI,
            """
                    <html>
                        <body>
                           <a href="${spongeConfigWithDepthTwo.spongeURI}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeURI) }
        verify { spongeService.request(childDocumentURI) }
    }

    @Test
    fun testDocumentWithChildDocumentEqualsToDirectParent() {
        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="${spongeConfig.spongeURI}" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
    }

    @Test
    fun testDocumentWithTooDeepChildDocumentAndLink() {
        val childDocumentURI = "https://test.com/test".toSpongeURI()
        val fileURI = "${spongeConfig.spongeURI}/$fileName".toSpongeURI()

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$childDocumentURI" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify { spongeService.request(childDocumentURI) }
        verify(exactly = 0) { spongeService.request(fileURI) }
        verify(exactly = 0) { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    @Test
    fun testDocumentWithIgnoredChildDocument() {
        val childDocumentURI = "https://test2.com".toSpongeURI()

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$childDocumentURI" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify(exactly = 0) { spongeService.request(childDocumentURI) }
    }

    @Test
    fun testDocumentWithInvalidChildURI() {
        val childURI = "http://"

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$childURI" />
                        </body>
                    </html>
                """
        )

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify { spongeService.request(any()) }
    }

    @Test
    fun testDocumentWithLinkAndFailedConnection() {
        val fileURI = "${spongeConfig.spongeURI}/$fileName".toSpongeURI()
        val failingFileName = "test2.txt"
        val failingFileURI = "${spongeConfig.spongeURI}/$failingFileName".toSpongeURI()

        givenDocument(
            spongeConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$failingFileURI" />
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenURIFailsConnection(failingFileURI)

        givenFile(fileURI)

        executeSponge(spongeConfig)

        verify { spongeService.request(spongeConfig.spongeURI) }
        verify { spongeService.request(failingFileURI) }
        verify(exactly = 0) { spongeService.download(failingFileURI, getDownloadPath(failingFileURI)) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    @Test
    fun testDocumentWithLimitedVisitedLinks() {
        val otherConfig = spongeConfig.copy(maximumURIs = 2)
        val fileURI = "${otherConfig.spongeURI}/$fileName".toSpongeURI()
        val otherFileURI = "${otherConfig.spongeURI}/test2.txt".toSpongeURI()

        givenDocument(
            otherConfig.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                            <a href="$otherFileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)
        givenFile(otherFileURI)

        executeSponge(otherConfig)

        verify { spongeService.request(otherConfig.spongeURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }

        verify(exactly = 0) { spongeService.request(otherFileURI) }
        verify(exactly = 0) { spongeService.download(otherFileURI, getDownloadPath(otherFileURI)) }
    }

    @Test
    fun testDocumentWithDuplicateDownloads() {
        val childDocumentURI = "https://test.com/test".toSpongeURI()
        val fileURI = "${spongeConfigWithDepthTwo.spongeURI}/$fileName".toSpongeURI()

        givenDocument(
            spongeConfigWithDepthTwo.spongeURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                            <a href="$childDocumentURI" />
                        </body>
                    </html>
                """
        )

        givenDocument(
            childDocumentURI,
            """
                    <html>
                        <body>
                            <a href="$fileURI" />
                        </body>
                    </html>
                """
        )

        givenFile(fileURI)

        executeSponge(spongeConfigWithDepthTwo)

        verify { spongeService.request(spongeConfigWithDepthTwo.spongeURI) }
        verify { spongeService.request(childDocumentURI) }
        verify { spongeService.request(fileURI) }
        verify { spongeService.download(fileURI, getDownloadPath(fileURI)) }
    }

    private fun givenDocument(
        spongeURI: SpongeURI,
        htmlContent: String,
        contentType: ContentType = ContentType.TEXT_HTML
    ) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns contentType.mimeType
        every { response.body() } returns htmlContent
        every { response.url() } returns URL(spongeURI.uri)

        every { spongeService.request(spongeURI) } returns response
    }

    private fun givenFile(spongeURI: SpongeURI, mimeType: String = ContentType.TEXT_PLAIN.mimeType) {
        val response = mockk<Connection.Response>()

        every { response.contentType() } returns mimeType
        every { spongeService.request(spongeURI) } returns response
    }

    private fun givenURIFailsConnection(spongeURI: SpongeURI) {
        every { spongeService.request(spongeURI) } throws IOException("Error!")
    }

    private fun executeSponge(spongeConfig: SpongeConfig) {
        Sponge(spongeService, spongeConfig).execute()
    }

    private fun getDownloadPath(spongeURI: SpongeURI): Path {
        return outputDirectory.resolve(spongeURI.host)
            .resolve(FilenameUtils.getPath(spongeURI.path))
            .resolve(FilenameUtils.getName(spongeURI.path))
            .toAbsolutePath()
    }
}
