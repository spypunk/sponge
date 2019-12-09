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
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private val traversedUris = mutableSetOf<URI>()
    private val urisChildren = mutableMapOf<URI, Set<URI>>()

    fun execute() {
        FileUtils.forceMkdir(spongeInput.outputDirectory)

        visitUri()
    }

    private fun visitUri(uri: URI = spongeInput.uri, depth: Int = 0) {
        if (traversedUris.contains(uri)) {
            return
        }

        try {
            val response = spongeService.connect(uri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (mimeType.isHtmlMimeType()) {
                if (depth < spongeInput.maxDepth) {
                    visitDocument(uri, depth, response)
                }
            } else if (spongeInput.mimeTypes.contains(mimeType)) {
                visitFile(uri, response)
            }
        } catch (e: IOException) {
            System.err.println("⚠ Processing failed for $uri: ${e.message}")

            traversedUris.add(uri)
        }
    }

    private fun visitDocument(uri: URI, depth: Int, response: Connection.Response) {
        val children: Set<URI>

        if (depth < spongeInput.maxDepth) {
            traversedUris.add(uri)

            println("﹫ $uri")

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

        children.forEach { visitUri(it, depth + 1) }
    }

    private fun getChildren(response: Connection.Response): Set<URI> {
        return response.parse().getElementsByTag("a").asSequence()
                .map { it.attr("abs:href") }
                .filterNot { it.isNullOrEmpty() }
                .distinct()
                .map { it.toURI() }
                .filterNotNull()
                .filter(this::shouldVisitUri)
                .toSet()
    }

    private fun shouldVisitUri(uri: URI): Boolean {
        val domain = uri.domain() ?: return false

        return domain == spongeInput.domain
                || spongeInput.includeSubdomains && domain.endsWith(spongeInput.domain)
    }

    private fun visitFile(uri: URI, response: Connection.Response) {
        traversedUris.add(uri)

        val fileName = FilenameUtils.getName(uri.path)
        val file = File(spongeInput.outputDirectory, fileName)

        if (!file.exists()) {
            spongeService.download(response, file)
        }
    }

    private fun String.isHtmlMimeType() = ContentType.TEXT_HTML.mimeType == this
            || ContentType.APPLICATION_XHTML_XML.mimeType == this

    private fun String.toURI(): URI? {
        return try {
            URI(this)
        } catch (e: URISyntaxException) {
            System.err.println("⚠ URI parsing failed for $this: ${e.message}")
            null
        }
    }
}
