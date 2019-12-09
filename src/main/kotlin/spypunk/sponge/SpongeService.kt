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
import java.net.URI

class SpongeService {
    private companion object {
        private const val CONNECTION_TIMEOUT = 5000
        private const val READ_TIMEOUT = 2000
    }

    fun connect(uri: URI): Connection.Response {
        return Jsoup.connect(uri.toString())
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .referrer("https://www.google.com")
                .timeout(CONNECTION_TIMEOUT)
                .maxBodySize(0)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(true)
                .execute()
    }

    fun download(uri: URI, file: File) = FileUtils.copyURLToFile(uri.toURL(), file, CONNECTION_TIMEOUT, READ_TIMEOUT)
}
