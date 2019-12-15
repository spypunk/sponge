/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge.service

import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import spypunk.sponge.SpongeService
import java.io.File
import java.nio.charset.StandardCharsets

class SpongeServiceTest {
    private companion object {
        private const val CONTENT = "content"
    }

    private val spongeService = SpongeService()
    private val sourceFile = File("sourceFile.txt")
    private val destinationFile = File("destinationFile.txt")

    @BeforeEach
    fun beforeEach() {
        FileUtils.deleteQuietly(sourceFile)
        FileUtils.deleteQuietly(destinationFile)
    }

    @Test
    fun testDownload() {
        FileUtils.write(sourceFile, CONTENT, StandardCharsets.UTF_8)

        spongeService.download(sourceFile.toURI(), destinationFile.toPath())

        val content = FileUtils.readFileToString(destinationFile, StandardCharsets.UTF_8)

        Assertions.assertEquals(CONTENT, content)
    }
}
