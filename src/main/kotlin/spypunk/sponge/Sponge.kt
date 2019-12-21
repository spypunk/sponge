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
        private val defaultUriMetadata = UriMetadata()
    }

    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val uriMetadatas = ConcurrentHashMap<String, UriMetadata>()
    private val processedDownloads = CopyOnWriteArraySet<String>()

    fun execute() = runBlocking { visitUri(spongeInput.uri.toString()) }

    private suspend fun visitUri(uri: String, parents: Set<String> = setOf()) {
        try {
            val uriMetadata = getUriMetadata(uri)

            if (uriMetadata.download) {
                download(uri)
            } else if (parents.size < spongeInput.maxDepth) {
                visitUris(uriMetadata.children, parents + uri)
            }
        } catch (e: Exception) {
            uriMetadatas[uri] = defaultUriMetadata

            System.err.println("⚠ Processing failed for $uri: ${e.javaClass.name} - ${e.message}")
        }
    }

    private fun getUriMetadata(uri: String): UriMetadata {
        return uriMetadatas.computeIfAbsent(uri) {
            val response = spongeService.request(uri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            when {
                checkDownloadEligibility(uri, mimeType) -> UriMetadata(download = true)
                htmlMimeTypes.contains(mimeType) -> UriMetadata(children = getChildren(uri, response))
                else -> defaultUriMetadata
            }
        }
    }

    private suspend fun visitUris(uris: Set<String>, parents: Set<String>) {
        uris.asSequence()
            .filterNot(parents::contains)
            .map { GlobalScope.async(requestContext) { visitUri(it, parents) } }
            .toList()
            .awaitAll()
    }

    private fun getChildren(uri: String, response: Connection.Response): Set<String> {
        println("↺ $uri")

        val document = Jsoup.parse(response.body(), response.url().toExternalForm())
        val links = getLinks(document) + getImageLinks(document)

        return links.mapNotNull { it.toUri() }
            .filter(this::checkHostEligibility)
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

    private fun checkHostEligibility(uri: URI): Boolean {
        return uri.host == spongeInput.uri.host ||
            spongeInput.includeSubdomains && uri.host.endsWith(spongeInput.uri.host)
    }

    private fun checkDownloadEligibility(uri: String, mimeType: String): Boolean {
        return spongeInput.fileExtensions.contains(FilenameUtils.getExtension(uri)) ||
            spongeInput.mimeTypes.contains(mimeType)
    }

    private suspend fun download(uri: String) {
        val path = URI(uri).let {
            spongeInput.outputDirectory
                .resolve(it.host)
                .resolve(FilenameUtils.getPath(it.path))
                .resolve(FilenameUtils.getName(it.path))
                .toAbsolutePath()
        }

        if (!processedDownloads.add(path.toString())) return

        withContext(downloadContext) { spongeService.download(uri, path) }
    }

    private fun String.toUri(): URI? {
        return try {
            toNormalizedUri()
        } catch (ignored: Exception) {
            null
        }
    }
}
