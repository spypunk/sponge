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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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

    fun execute() {
        FileUtils.forceMkdir(outputDirectory)

        visit(uri)
    }

    private fun visit(uri: URI, depth: Int = 0, parents: MutableSet<URI> = mutableSetOf()) {
        try {
            val response = Jsoup.connect(uri.toString())
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                    .ignoreContentType(true)
                    .execute()

            val contentType = response.contentType()
            val depthPrefix = "\t".repeat(depth)

            if (documentContentType.matcher(contentType).matches()) {
                if (depth < maxDepth) {
                    visitDocument(uri, depth, response.parse(), depthPrefix, parents)
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
            document: Document,
            depthPrefix: String,
            parents: MutableSet<URI>
    ) {
        println("$depthPrefix$uri")

        parents.add(uri)

        document.getElementsByTag("a").asSequence()
                .map { it.attr("abs:href") }
                .filterNot(String::isNullOrEmpty)
                .map { it.toCleanUri() }
                .distinct()
                .filterNot { parents.contains(it) }
                .forEach { visit(it, depth + 1, parents) }

        parents.remove(uri)
    }

    private fun visitFile(uri: URI, depthPrefix: String) {
        val fileName = FilenameUtils.getName(uri.path)
        val file = File(outputDirectory, fileName)

        if (fileExtensions.contains(file.extension) && !file.exists()) {
            FileUtils.copyURLToFile(uri.toURL(), file)

            println("$depthPrefix$uri -> ${file.absolutePath} [${file.humanSize()}]")
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
