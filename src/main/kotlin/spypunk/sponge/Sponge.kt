/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import org.apache.commons.io.FilenameUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {

    private class ResponseData(val response: Connection.Response) {
        val mimeType: String = ContentType.parse(response.contentType()).mimeType
    }

    private val urisResponseData = mutableMapOf<URI, ResponseData>()
    private val urisChildren = mutableMapOf<URI, Set<URI>>()
    private val failedUris = mutableSetOf<URI>()

    fun execute() {
        Files.createDirectories(spongeInput.outputDirectory)

        visitUri()

        urisResponseData.clear()
        urisChildren.clear()
        failedUris.clear()
    }

    private fun visitUri(uri: URI = spongeInput.uri, depth: Int = 0) {
        if (failedUris.contains(uri)) {
            return
        }

        try {
            val responseData = urisResponseData.computeIfAbsent(uri) {
                val response = spongeService.connect(it)

                println("﹫ $uri")

                ResponseData(response)
            }

            if (responseData.mimeType.isHtmlMimeType()) {
                if (depth < spongeInput.maxDepth) {
                    visitChildren(uri, depth, responseData.response)
                }
            } else if (spongeInput.mimeTypes.contains(responseData.mimeType)) {
                visitFile(uri, responseData.response)
            }
        } catch (e: IOException) {
            System.err.println("⚠ Processing failed for $uri: ${e.message}")

            failedUris.add(uri)
        }
    }

    private fun visitChildren(uri: URI, depth: Int, response: Connection.Response) {
        urisChildren.computeIfAbsent(uri) { getChildren(response) }
                .forEach { visitUri(it, depth + 1) }
    }

    private fun getChildren(response: Connection.Response): Set<URI> {
        return response.parse().getElementsByTag("a").asSequence()
                .map { it.attr("abs:href") }
                .filterNot { it.isNullOrEmpty() }
                .distinct()
                .map { it.toURI() }
                .filterNotNull()
                .filter(this::hasValidDomain)
                .toSet()
    }

    private fun hasValidDomain(uri: URI): Boolean {
        val domain = uri.domain() ?: return false

        return domain == spongeInput.domain
                || spongeInput.includeSubdomains && domain.endsWith(spongeInput.domain)
    }

    private fun visitFile(uri: URI, response: Connection.Response) {
        val fileName = FilenameUtils.getName(uri.path)
        val filePath = spongeInput.outputDirectory.resolve(fileName).toAbsolutePath()

        if (!Files.exists(filePath)) {
            spongeService.download(response, filePath)
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
