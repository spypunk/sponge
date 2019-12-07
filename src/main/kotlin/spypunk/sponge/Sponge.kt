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
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import java.io.File
import java.net.URI
import java.net.URISyntaxException

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private val traversedUris = mutableSetOf<URI>()
    private val urisChildren = mutableMapOf<URI, Set<URI>>()

    fun execute() {
        FileUtils.forceMkdir(spongeInput.outputDirectory)

        visit(spongeInput.uri)
    }

    private fun visit(uri: URI, depth: Int = 0) {
        try {
            val response = spongeService.connect(uri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (mimeType.isHtmlMimeType()) {
                if (depth < spongeInput.maxDepth) {
                    visitDocument(uri, depth, response)
                }
            } else if (spongeInput.mimeTypes.contains(mimeType)) {
                visitFile(uri)
            }
        } catch (e: Throwable) {
            System.err.println("⚠ Processing failed for $uri: ${e.message}")

            traversedUris.add(uri)
        }
    }

    private fun visitDocument(uri: URI, depth: Int, response: Connection.Response) {
        val children: Set<URI>

        if (depth < spongeInput.maxDepth - 1) {
            traversedUris.add(uri)

            if (urisChildren.contains(uri)) {
                children = urisChildren.getValue(uri)

                urisChildren.remove(uri)
            } else {
                children = getChildren(response)
            }
        } else if (urisChildren.contains(uri)) {
            return
        } else {
            children = getChildren(response)

            urisChildren[uri] = children
        }

        if (children.isNotEmpty()) {
            println("﹫ $uri")

            visitChildren(children, depth)
        }
    }

    private fun getChildren(response: Connection.Response): Set<URI> {
        return response.parse()
                .getElementsByTag("a")
                .asSequence()
                .map { it.attr("abs:href") }
                .filterNot { it.isNullOrEmpty() }
                .map { it.toURI() }
                .filterNotNull()
                .distinct()
                .toSet()
    }

    private fun visitChildren(children: Set<URI>, depth: Int) {
        children.asSequence()
                .filterNot { traversedUris.contains(it) }
                .forEach { visit(it, depth + 1) }
    }

    private fun visitFile(uri: URI) {
        val fileName = FilenameUtils.getName(uri.path)
        val file = File(spongeInput.outputDirectory, fileName)

        if (!file.exists()) {
            spongeService.download(uri, file)

            println("⬇ ${file.absolutePath} [${file.humanSize()}]")

            traversedUris.add(uri)
        }
    }

    private fun File.humanSize(): String = FileUtils.byteCountToDisplaySize(length())

    private fun String.isHtmlMimeType() = ContentType.TEXT_HTML.mimeType == this
            || ContentType.APPLICATION_XHTML_XML.mimeType == this

    private fun String.toURI(): URI? {
        return try {
            URIBuilder(this)
                    .apply {
                        fragment = null
                    }
                    .build()
        } catch (e: URISyntaxException) {
            System.err.println("⚠ URI parsing failed for $this: ${e.message}")
            null
        }
    }
}
