/**
 * Copyright © 2019-2023 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpongeServiceTest {
    private val outputDirectory = Paths.get("testOutput").toAbsolutePath()
    private val spongeService = SpongeService(SpongeServiceConfig())
    private val host = "localhost"
    private val filePath = Paths.get("$outputDirectory/$host/test.txt").toAbsolutePath()
    private val fileContent = "test"
    private val port = 12_345

    private val server = startClientAndServer(port)
        .also {
            it.`when`(request().withPath("/${filePath.fileName}"))
                .respond(
                    response()
                        .withStatusCode(200)
                        .withBody(fileContent)
                )
        }

    @BeforeAll
    fun beforeAll() {
        FileUtils.deleteDirectory(outputDirectory.toFile())
    }

    @BeforeEach
    fun beforeEach() {
        FileUtils.deleteQuietly(filePath.toFile())
    }

    @Test
    fun testDownload() {
        val spongeURI = "http://$host:$port/${filePath.fileName}".toSpongeURI()
        val spongeDownload = spongeService.download(spongeURI, filePath)

        Assertions.assertEquals(fileContent, FileUtils.readFileToString(filePath.toFile(), StandardCharsets.UTF_8))
        Assertions.assertTrue(spongeDownload.duration > 0)
        Assertions.assertEquals(fileContent.length.toLong(), spongeDownload.size)
    }

    @AfterAll
    fun after() = server.stop()
}
