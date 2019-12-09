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
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.concurrent.Executors

class SpongeService {
    private val executorService = Executors.newSingleThreadExecutor()

    fun connect(uri: URI): Connection.Response {
        return Jsoup.connect(uri.toString())
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .referrer("https://www.google.com")
                .maxBodySize(0)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(true)
                .execute()
    }

    fun download(response: Connection.Response, file: File) {
        executorService.execute {
            try {
                response.bodyStream().use { FileUtils.copyToFile(it, file) }

                println("⬇ ${file.absolutePath} [${file.humanSize()}]")
            } catch (e: IOException) {
                System.err.println("⚠ Error encountered while downloading ${response.url()}: ${e.message}")
            }
        }
    }

    fun stop() {
        executorService.shutdown()
    }

    private fun File.humanSize(): String = FileUtils.byteCountToDisplaySize(length())
}
