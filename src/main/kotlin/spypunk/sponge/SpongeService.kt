/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.input.CountingInputStream
import org.apache.http.HttpHeaders
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import kotlin.system.measureNanoTime

private const val DEFAULT_REQUEST_TIMEOUT = 30_000
private const val ENCODING = "gzip, deflate"
private const val NS_TO_S = 1_000_000_000.0
private const val KB_TO_B = 1_000.0

private fun download(inputStream: InputStream, path: Path) {
    Files.createDirectories(path.parent)

    val countingInputStream = CountingInputStream(inputStream)

    val duration = countingInputStream.use {
        measureNanoTime {
            Files.copy(countingInputStream, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    val size = countingInputStream.byteCount
    val humanSize = FileUtils.byteCountToDisplaySize(size)
    val speed = NS_TO_S * size / (KB_TO_B * duration)
    val humanSpeed = "%.2f kB/s".format(speed)

    println("↓ $path [$humanSize] [$humanSpeed]")
}

private fun <T> retry(clazz: KClass<out Exception>, retryCount: Int = 3, block: () -> T): T {
    var exception: Exception? = null

    for (ignored in 0..retryCount) {
        exception = try {
            return block()
        } catch (e: Exception) {
            if (!clazz.isSuperclassOf(e::class)) throw e

            e
        }
    }

    throw exception!!
}

class SpongeService(private val spongeServiceConfig: SpongeServiceConfig) {
    fun request(spongeUri: SpongeUri, timeout: Int = DEFAULT_REQUEST_TIMEOUT): Connection.Response {
        return Jsoup.connect(spongeUri.uri)
            .timeout(timeout)
            .header(HttpHeaders.ACCEPT_ENCODING, ENCODING)
            .referrer(spongeServiceConfig.referrer)
            .userAgent(spongeServiceConfig.userAgent)
            .maxBodySize(0)
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .followRedirects(true)
            .let { retry(IOException::class) { it.execute() } }
    }

    fun download(spongeUri: SpongeUri) {
        val path = getDownloadPath(spongeUri)

        if (!spongeServiceConfig.overwriteExistingFiles && Files.exists(path)) {
            println("∃ $path")
        } else {
            request(spongeUri, 0).bodyStream()
                .use { download(it, path) }
        }
    }

    private fun getDownloadPath(spongeUri: SpongeUri): Path {
        return spongeServiceConfig.outputDirectory.resolve(spongeUri.host)
            .resolve(FilenameUtils.getPath(spongeUri.path))
            .resolve(FilenameUtils.getName(spongeUri.path))
            .toAbsolutePath()
    }
}
