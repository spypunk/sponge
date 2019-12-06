/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.http.HttpHeaders
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.net.URI
import java.util.regex.Pattern

class Sponge(
        private val uri: URI,
        private val outputDirectory: File,
        private val fileExtensions: Set<String>,
        private val maxDepth: Int
) {
    private companion object {
        private val documentContentType = Pattern.compile("text/html.*|(application|text)/\\w*\\+?xml.*")
    }

    private val traversedUris: MutableSet<URI> = mutableSetOf()
    private val visitedUris: MutableSet<URI> = mutableSetOf()
    private val urisChildren: MutableMap<URI, Set<URI>> = mutableMapOf()

    fun execute() {
        FileUtils.forceMkdir(outputDirectory)

        visit(uri)
    }

    private fun visit(uri: URI, depth: Int = 0) {
        try {
            val response = Jsoup.connect(uri.toString())
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                    .ignoreContentType(true)
                    .execute()

            val contentType = response.contentType()
            val depthPrefix = "\t".repeat(depth)

            if (documentContentType.matcher(contentType).matches()) {
                if (depth < maxDepth) {
                    visitDocument(uri, depth, response, depthPrefix)
                }
            } else {
                visitFile(uri, depthPrefix)
            }
        } catch (e: Throwable) {
            // ignore
        }
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

            if (visitedUris.contains(uri)) {
                visitedUris.remove(uri)

                children = urisChildren.getValue(uri)

                urisChildren.remove(uri)
            } else {
                children = getChildren(response)
            }
        } else {
            if (visitedUris.contains(uri)) return

            visitedUris.add(uri)

            children = getChildren(response)

            urisChildren[uri] = children
        }

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

        if (fileExtensions.contains(file.extension) && !file.exists()) {
            FileUtils.copyURLToFile(uri.toURL(), file)

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
}
