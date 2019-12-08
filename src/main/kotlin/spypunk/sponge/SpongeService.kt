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
        private const val downloadTimeout = 1000
    }

    fun connect(uri: URI): Connection.Response {
        return Jsoup.connect(uri.toString())
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .execute()
    }

    fun download(uri: URI, file: File) = FileUtils.copyURLToFile(uri.toURL(), file, downloadTimeout, downloadTimeout)
}
