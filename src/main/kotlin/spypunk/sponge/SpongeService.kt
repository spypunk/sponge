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
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

class SpongeService {
    private val executorService = Executors.newSingleThreadExecutor()

    fun connect(uri: URI): Connection.Response =
            Jsoup.connect(uri.toString())
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                    .referrer("https://www.google.com")
                    .maxBodySize(0)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .execute()

    fun download(response: Connection.Response, path: Path) =
            executorService.execute {
                try {
                    response.bodyStream().use { Files.copy(it, path) }

                    println("⬇ $path [${path.humanSize()}]")
                } catch (t: Throwable) {
                    System.err.println("⚠ Error encountered while downloading ${response.url()}: ${t.message}")
                }
            }

    fun stop() = executorService.shutdown()

    private fun Path.humanSize() = FileUtils.byteCountToDisplaySize(Files.size(this))
}
