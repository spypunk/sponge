/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.apache.commons.io.FileUtils
import org.apache.http.HttpHeaders
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

private const val DEFAULT_REQUEST_TIMEOUT = 30_000
private const val ENCODING = "gzip, deflate"

class SpongeService(private val referrer: String, private val userAgent: String) {
    fun request(spongeUri: SpongeUri, timeout: Int = DEFAULT_REQUEST_TIMEOUT): Connection.Response {
        return Jsoup.connect(spongeUri.toString())
            .timeout(timeout)
            .header(HttpHeaders.ACCEPT_ENCODING, ENCODING)
            .referrer(referrer)
            .userAgent(userAgent)
            .maxBodySize(0)
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .followRedirects(true)
            .let { retry(IOException::class) { it.execute() } }
    }

    fun download(spongeUri: SpongeUri, path: Path) {
        Files.createDirectories(path.parent)

        request(spongeUri, 0).bodyStream()
            .use { Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING) }

        println("↓ $path [${path.humanSize()}]")
    }

    private inline fun <T> retry(clazz: KClass<out Exception>, retryCount: Int = 3, block: () -> T): T {
        var exception: Exception? = null

        for (ignored in 0..retryCount) {
            try {
                return block.invoke()
            } catch (e: Exception) {
                if (!clazz.isSuperclassOf(e::class)) throw e

                exception = e
            }
        }

        throw exception!!
    }
}

private fun Path.humanSize() = FileUtils.byteCountToDisplaySize(Files.size(this))
