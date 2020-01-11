/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpongeServiceTest {
    private val spongeService = SpongeService("https://test.com", "test")
    private val filePath = Paths.get("test.txt").toAbsolutePath()
    private val fileContent = "test"
    private val port = 12_345

    private val server = startClientAndServer(port)
        .also {
            it.`when`(
                request()
                    .withPath("/${filePath.fileName}")
            )
                .respond(
                    response()
                        .withStatusCode(200)
                        .withBody(fileContent)
                )
        }

    @BeforeAll
    fun before() {
        Files.deleteIfExists(filePath)
    }

    @Test
    fun testDownload() {
        spongeService.download("http://localhost:$port/${filePath.fileName}".toSpongeUri(), filePath)

        Assertions.assertEquals(fileContent, FileUtils.readFileToString(filePath.toFile(), StandardCharsets.UTF_8))
    }

    @AfterAll
    fun after() = server.stop()
}
