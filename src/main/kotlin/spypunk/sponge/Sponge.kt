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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.http.entity.ContentType
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private companion object {
        private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)
    }

    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val urisChildren = ConcurrentHashMap<URI, Set<URI>>()
    private val failedUris = CopyOnWriteArraySet<URI>()
    private val mimeTypes = ConcurrentHashMap<String, String>()

    fun execute() {
        Files.createDirectories(spongeInput.outputDirectory)

        runBlocking { visitUri() }
    }

    private suspend fun visitUri(uri: URI = spongeInput.uri, depth: Int = 0) {
        if (failedUris.contains(uri)) return

        try {
            val response = spongeService.request(uri)
            val mimeType = mimeTypes.computeIfAbsent(response.contentType()) { ContentType.parse(it).mimeType }

            if (canDownload(mimeType, uri)) {
                download(uri)
            } else if (canVisitChildren(depth, mimeType)) {
                visitChildren(uri, depth, response)
            }
        } catch (e: Exception) {
            System.err.println("⚠ Processing failed for $uri: ${e.message}")

            failedUris.add(uri)
        }
    }

    private fun canVisitChildren(depth: Int, mimeType: String): Boolean {
        return depth < spongeInput.maxDepth && htmlMimeTypes.contains(mimeType)
    }

    private suspend fun visitChildren(uri: URI, depth: Int, response: Connection.Response) {
        getChildren(uri, response)
                .map { GlobalScope.async(requestContext) { visitUri(it, depth + 1) } }
                .awaitAll()
    }

    private fun getChildren(uri: URI, response: Connection.Response): Set<URI> {
        return urisChildren.computeIfAbsent(uri) {
            println("↺ $uri")

            val document = Jsoup.parse(response.body(), response.url().toExternalForm())
            val links = getLinks(document) + getImageLinks(document)

            links.mapNotNull { it.toUri() }
                    .filter(this::isHostEligible)
                    .toSet()
        }
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
                .filterNot { it.isEmpty() }
    }

    private fun isHostEligible(uri: URI): Boolean {
        return uri.host == spongeInput.uri.host
                || (spongeInput.includeSubdomains && uri.host.endsWith(spongeInput.uri.host))
    }

    private fun canDownload(mimeType: String, uri: URI): Boolean {
        return spongeInput.mimeTypes.contains(mimeType)
                || spongeInput.fileExtensions.contains(FilenameUtils.getExtension(uri.path))
    }

    private suspend fun download(uri: URI) {
        val fileName = FilenameUtils.getName(uri.path)
        val filePath = spongeInput.outputDirectory.resolve(fileName).toAbsolutePath()

        println("⇩ Download scheduled: $uri")

        withContext(downloadContext) {
            spongeService.download(uri, filePath)

            println("↓ Download completed: $filePath [${filePath.humanSize()}]")
        }
    }

    private fun String.toUri(): URI? {
        return try {
            toNormalizedUri()
        } catch (e: Exception) {
            null
        }
    }

    private fun Path.humanSize() = FileUtils.byteCountToDisplaySize(Files.size(this))
}
