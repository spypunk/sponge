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
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArraySet

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private class UriMetadata(val canDownload: Boolean = false, val children: Set<URI> = setOf())

    private companion object {
        private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)
        private val defaultUriMetadata = UriMetadata()
    }

    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val visitedUris = HashSet<URI>()
    private val failedUris = CopyOnWriteArraySet<URI>()
    private val uriMetadatas = HashMap<URI, UriMetadata>()
    private val processedDownloads = HashSet<String>()

    fun execute() {
        Files.createDirectories(spongeInput.outputDirectory)

        runBlocking { visitUri() }
    }

    private suspend fun visitUri(uri: URI = spongeInput.uri, parents: Set<URI> = setOf()) {
        if (failedUris.contains(uri)) return

        try {
            val uriMetadata = getUriMetadata(uri)

            if (uriMetadata.canDownload) {
                download(uri)
            } else if (parents.size < spongeInput.maxDepth) {
                visitUris(uriMetadata.children, parents + uri)
            }
        } catch (e: Exception) {
            failedUris.add(uri)

            System.err.println("⚠ Processing failed for $uri: ${e.javaClass.name} - ${e.message}")
        }
    }

    private fun getUriMetadata(uri: URI): UriMetadata {
        synchronized(visitedUris) {
            if (visitedUris.contains(uri)) return uriMetadatas.getValue(uri)

            visitedUris.add(uri)
        }

        val response = spongeService.request(uri)
        val mimeType = ContentType.parse(response.contentType()).mimeType

        return uriMetadatas.getOrPut(uri) {
            when {
                canDownload(uri, mimeType) -> UriMetadata(canDownload = true)
                htmlMimeTypes.contains(mimeType) -> UriMetadata(children = getChildren(uri, response))
                else -> defaultUriMetadata
            }
        }
    }

    private suspend fun visitUris(uris: Set<URI>, parents: Set<URI>) {
        uris.asSequence()
                .filterNot(parents::contains)
                .map { GlobalScope.async(requestContext) { visitUri(it, parents) } }
                .toList()
                .awaitAll()
    }

    private fun getChildren(uri: URI, response: Connection.Response): Set<URI> {
        val document = Jsoup.parse(response.body(), response.url().toExternalForm())
        val links = getLinks(document) + getImageLinks(document)

        return links.mapNotNull { it.toUri() }
                .filterNot(uri::equals)
                .filter(this::isHostEligible)
                .toSet()
                .also { if (it.isNotEmpty()) println("↺ $uri") }
    }

    private fun getLinks(document: Document): Sequence<String> {
        return getAttributeValues(document, "a[href]", "abs:href")
    }

    private fun getImageLinks(document: Document): Sequence<String> {
        return getAttributeValues(document, "img[src]", "abs:src")
    }

    private fun getAttributeValues(document: Document, cssQuery: String, attributeKey: String): Sequence<String> {
        return document.select(cssQuery).asSequence()
                .mapNotNull { it.attr(attributeKey) }
                .filterNot(String::isEmpty)
    }

    private fun isHostEligible(uri: URI): Boolean {
        return uri.host == spongeInput.uri.host
                || (spongeInput.includeSubdomains && uri.host.endsWith(spongeInput.uri.host))
    }

    private fun canDownload(uri: URI, mimeType: String): Boolean {
        return spongeInput.fileExtensions.contains(FilenameUtils.getExtension(uri.path))
                || spongeInput.mimeTypes.contains(mimeType)
    }

    private suspend fun download(uri: URI) {
        val fileName = FilenameUtils.getName(uri.path)

        synchronized(processedDownloads) {
            if (processedDownloads.contains(fileName)) return

            processedDownloads.add(fileName)
        }

        val filePath = spongeInput.outputDirectory.resolve(fileName).toAbsolutePath()

        withContext(downloadContext) { spongeService.download(uri, filePath) }
    }

    private fun String.toUri(): URI? {
        return try {
            toNormalizedUri()
        } catch (e: Exception) {
            null
        }
    }
}
