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
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class Sponge(private val spongeService: SpongeService, private val spongeInput: SpongeInput) {
    private val requestContext = newFixedThreadPoolContext(spongeInput.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeInput.concurrentDownloads, "download")
    private val urisChildren = ConcurrentHashMap<URI, Set<URI>>()
    private val failedUris = CopyOnWriteArraySet<URI>()

    fun execute() {
        Files.createDirectories(spongeInput.outputDirectory)

        runBlocking { processUri() }
    }

    private suspend fun processUri(uri: URI = spongeInput.uri, depth: Int = 0) {
        if (failedUris.contains(uri)) return

        try {
            val response = spongeService.request(uri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (mimeType.isHtmlMimeType()) {
                processChildren(uri, depth, response)
            } else if (spongeInput.mimeTypes.contains(mimeType)) {
                downloadFile(uri)
            }
        } catch (e: Exception) {
            System.err.println("⚠ Processing failed for $uri: ${e.message}")

            failedUris.add(uri)
        }
    }

    private suspend fun processChildren(uri: URI, depth: Int, response: Connection.Response) {
        cacheChildren(uri, response)

        if (depth < spongeInput.maxDepth) {
            urisChildren.getValue(uri)
                    .map { GlobalScope.async(requestContext) { processUri(it, depth + 1) } }
                    .awaitAll()
        }
    }

    private fun cacheChildren(uri: URI, response: Connection.Response) {
        urisChildren.computeIfAbsent(uri) {
            println("﹫ $uri")

            getChildren(response)
        }
    }

    private fun getChildren(response: Connection.Response): Set<URI> {
        val body = response.body()
        val externalForm = response.url().toExternalForm()

        return Jsoup.parse(body, externalForm).select("a[href]").asSequence()
                .map { it.attr("abs:href") }
                .filterNot { it.isNullOrEmpty() }
                .mapNotNull { it.toUri() }
                .filter(this::isHostEligible)
                .toSet()
    }

    private fun isHostEligible(uri: URI): Boolean {
        return uri.host == spongeInput.uri.host
                || spongeInput.includeSubdomains && uri.host.endsWith(spongeInput.uri.host)
    }

    private suspend fun downloadFile(uri: URI) {
        val fileName = FilenameUtils.getName(uri.path)
        val filePath = spongeInput.outputDirectory.resolve(fileName).toAbsolutePath()

        if (!Files.exists(filePath)) withContext(downloadContext) { spongeService.download(uri, filePath) }
    }

    private fun String.isHtmlMimeType(): Boolean {
        return ContentType.TEXT_HTML.mimeType == this || ContentType.APPLICATION_XHTML_XML.mimeType == this
    }

    private fun String.toUri(): URI? {
        return try {
            toNormalizedUri()
        } catch (e: Exception) {
            System.err.println("⚠ URI parsing failed for $this: ${e.message}")
            null
        }
    }
}
