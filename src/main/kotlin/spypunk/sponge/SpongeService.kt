/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
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
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class SpongeService() {
    fun request(uri: URI): Connection.Response {
        val connection = Jsoup.connect(uri.toString())
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .referrer("https://www.google.com")
                .maxBodySize(0)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(true)

        return retry(IOException::class) { connection.execute() }
    }

    fun download(uri: URI, path: Path) {
        try {
            FileUtils.copyURLToFile(uri.toURL(), path.toFile())

            println("⬇ $path [${path.humanSize()}]")
        } catch (e: Exception) {
            System.err.println("⚠ Error encountered while downloading $uri: ${e.message}")
        }
    }

    private fun Path.humanSize() = FileUtils.byteCountToDisplaySize(Files.size(this))

    private inline fun <T> retry(clazz: KClass<out Exception>, retryCount: Int = 3, block: () -> T): T {
        var exception: Exception? = null

        for (i in 0..retryCount) {
            try {
                return block.invoke()
            } catch (e: Exception) {
                if (!clazz.isSuperclassOf(e::class)) {
                    throw e
                }

                exception = e
            }
        }

        throw exception!!
    }
}
