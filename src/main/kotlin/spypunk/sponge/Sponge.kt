/**
 * Copyright © 2019 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private data class UriMetadata(val download: Boolean = false, val children: Set<String> = setOf())

    private companion object {
        private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)
        private val ignoredUriMetadata = UriMetadata()
    }

    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val uriMetadatas = ConcurrentHashMap<String, UriMetadata>()
    private val processedDownloads = CopyOnWriteArraySet<String>()

    fun execute() = runBlocking { visitUri(spongeInput.uri) }

    private suspend fun visitUri(uri: URI, parents: Set<URI> = setOf()) {
        val uriMetadata = uriMetadatas.computeIfAbsent(uri.toString()) { getUriMetadata(uri) }

        if (uriMetadata.download) {
            download(uri)
        } else if (parents.size < spongeInput.maxDepth) {
            visitUris(uriMetadata.children, parents + uri)
        }
    }

    private fun getUriMetadata(uri: URI): UriMetadata {
        return try {
            val response = spongeService.request(uri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            when {
                canDownload(uri, mimeType) -> UriMetadata(download = true)
                htmlMimeTypes.contains(mimeType) -> UriMetadata(children = getChildren(uri, response))
                else -> ignoredUriMetadata
            }
        } catch (e: Exception) {
            System.err.println("⚠ Processing failed for $uri: ${e.javaClass.name} - ${e.message}")

            ignoredUriMetadata
        }
    }

    private suspend fun visitUris(uris: Set<String>, parents: Set<URI>) {
        uris.asSequence()
            .mapNotNull(String::toUri)
            .filterNot(parents::contains)
            .map { GlobalScope.async(requestContext) { visitUri(it, parents) } }
            .toList()
            .awaitAll()
    }

    private fun getChildren(uri: URI, response: Connection.Response): Set<String> {
        println("↺ $uri")

        val document = Jsoup.parse(response.body(), response.url().toExternalForm())
        val links = getLinks(document) + getImageLinks(document)

        return links.mapNotNull(String::toUri)
            .filter(this::isHostEligible)
            .map(URI::toString)
            .filterNot(uri::equals)
            .toSet()
    }

    private fun getLinks(document: Document) = getAttributeValues(document, "a[href]", "abs:href")

    private fun getImageLinks(document: Document) = getAttributeValues(document, "img[src]", "abs:src")

    private fun getAttributeValues(document: Document, cssQuery: String, attributeKey: String): Sequence<String> {
        return document.select(cssQuery).asSequence()
            .mapNotNull { it.attr(attributeKey) }
            .filterNot(String::isEmpty)
    }

    private fun isHostEligible(uri: URI): Boolean {
        return uri.host == spongeInput.uri.host ||
            spongeInput.includeSubdomains && uri.host.endsWith(spongeInput.uri.host)
    }

    private fun canDownload(uri: URI, mimeType: String): Boolean {
        return spongeInput.fileExtensions.contains(FilenameUtils.getExtension(uri.path)) ||
            spongeInput.mimeTypes.contains(mimeType)
    }

    private suspend fun download(uri: URI) {
        val path = spongeInput.outputDirectory
            .resolve(uri.host)
            .resolve(FilenameUtils.getPath(uri.path))
            .resolve(FilenameUtils.getName(uri.path))
            .toAbsolutePath()

        if (!processedDownloads.add(path.toString())) return

        withContext(downloadContext) { spongeService.download(uri, path) }
    }
}

private fun String.toUri(): URI? {
    return try {
        toNormalizedUri()
    } catch (ignored: Exception) {
        null
    }
}
