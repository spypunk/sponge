/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import com.google.common.net.MediaType
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.net.URI

class Sponge(
        private val uri: URI,
        private val outputDirectory: File,
        private val mediaTypes: Set<MediaType>,
        private val maxDepth: Int
) {
    private companion object {
        private const val downloadTimeout = 1000
    }

    private val traversedUris: MutableSet<URI> = mutableSetOf()
    private val urisChildren: MutableMap<URI, Set<URI>> = mutableMapOf()

    fun execute() {
        FileUtils.forceMkdir(outputDirectory)

        visit(uri)
    }

    private fun visit(uri: URI, depth: Int = 0) {
        if (depth == maxDepth) {
            return
        }

        try {
            val response = connect(uri)

            if (HttpStatus.SC_OK != response.statusCode()) return

            val mediaType = MediaType.parse(response.contentType()).withoutParameters()
            val depthPrefix = "\t".repeat(depth)

            if (mediaType.isHtml()) {
                visitDocument(uri, depth, response, depthPrefix)
            } else if (mediaTypes.contains(mediaType)) {
                visitFile(uri, depthPrefix)
            }
        } catch (e: Throwable) {
            // ignore
        }
    }

    private fun connect(uri: URI): Connection.Response {
        return Jsoup.connect(uri.toString())
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .ignoreContentType(true)
                .execute()
    }

    private fun visitDocument(
            uri: URI,
            depth: Int,
            response: Connection.Response,
            depthPrefix: String
    ) {
        val children: Set<URI>

        if (depth < maxDepth - 1) {
            traversedUris.add(uri)

            if (urisChildren.contains(uri)) {
                children = urisChildren.getValue(uri)

                urisChildren.remove(uri)
            } else {
                children = getChildren(response)
            }
        } else {
            if (urisChildren.contains(uri)) return

            children = getChildren(response)

            urisChildren[uri] = children
        }

        if (children.isEmpty()) return

        println("$depthPrefix$uri")

        visitChildren(children, depth)
    }

    private fun getChildren(response: Connection.Response): Set<URI> {
        return response.parse()
                .getElementsByTag("a")
                .asSequence()
                .map { it.attr("abs:href") }
                .filterNot(String::isNullOrEmpty)
                .map { it.toCleanUri() }
                .distinct()
                .toSet()
    }

    private fun visitChildren(children: Set<URI>, depth: Int) {
        children.asSequence()
                .filterNot { traversedUris.contains(it) }
                .forEach { visit(it, depth + 1) }
    }

    private fun visitFile(uri: URI, depthPrefix: String) {
        val fileName = FilenameUtils.getName(uri.path)
        val file = File(outputDirectory, fileName)

        if (!file.exists()) {
            FileUtils.copyURLToFile(uri.toURL(), file, downloadTimeout, downloadTimeout)

            println("$depthPrefix$uri -> ${file.absolutePath} [${file.humanSize()}]")

            traversedUris.add(uri)
        }
    }

    private fun String.toCleanUri(): URI {
        return URIBuilder(this)
                .apply {
                    fragment = null
                    removeQuery()
                }
                .build()
    }

    private fun File.humanSize(): String = FileUtils.byteCountToDisplaySize(length())

    private fun MediaType.isHtml(): Boolean {
        return MediaType.HTML_UTF_8.withoutParameters() == this.withoutParameters()
                || MediaType.XHTML_UTF_8.withoutParameters() == this.withoutParameters()
    }
}
