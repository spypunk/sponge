/**
 * Copyright © 2019-2020 spypunk <spypunk@gmail.com>
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
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.entity.ContentType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

private data class VisitResult(val download: Boolean = false, val children: Set<SpongeUri> = setOf())

private val skipVisitResult = VisitResult()
private val downloadVisitResult = VisitResult(download = true)

private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)

private val attributeKeys = mapOf(
    "a[href]" to "abs:href",
    "img[src]" to "abs:src"
)

val Throwable.rootMessage: String
    get() = ExceptionUtils.getRootCauseMessage(this)

private fun toSpongeUri(element: Element, attributeKey: String): SpongeUri? {
    return try {
        element.attr(attributeKey)?.let {
            SpongeUri(it)
        }
    } catch (ignored: Exception) {
        null
    }
}

class Sponge(private val spongeService: SpongeService, private val spongeConfig: SpongeConfig) {
    private val requestContext = newFixedThreadPoolContext(spongeConfig.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeConfig.concurrentDownloads, "download")
    private val visitResults = ConcurrentHashMap<SpongeUri, VisitResult>()
    private val downloadedUris = CopyOnWriteArraySet<SpongeUri>()
    private val visitedCount = AtomicInteger()

    fun execute() = runBlocking { visit() }

    private suspend fun visit(spongeUri: SpongeUri = spongeConfig.spongeUri, parents: Set<SpongeUri> = setOf()) {
        if (visitedCount.incrementAndGet() > spongeConfig.maximumUris) return

        try {
            val visitResult = getVisitResult(spongeUri)

            if (visitResult !== skipVisitResult) {
                downloadOrVisitChildren(visitResult, spongeUri, parents)
            }
        } catch (t: Throwable) {
            System.err.println("⚠ Processing failed for $spongeUri: ${t.rootMessage}")
        }
    }

    private fun getVisitResult(spongeUri: SpongeUri) =
        visitResults.computeIfAbsent(spongeUri) { createVisitResult(spongeUri) }

    private fun createVisitResult(spongeUri: SpongeUri): VisitResult {
        val extension = FilenameUtils.getExtension(spongeUri.path)
        var visitResult = skipVisitResult

        if (spongeConfig.fileExtensions.contains(extension)) {
            visitResult = downloadVisitResult
        } else {
            val response = spongeService.request(spongeUri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (htmlMimeTypes.contains(mimeType)) {
                val document = Jsoup.parse(response.body(), response.url().toExternalForm())
                val children = getChildren(spongeUri, document)

                if (children.isNotEmpty()) {
                    visitResult = VisitResult(children = children)
                }
            } else if (spongeConfig.mimeTypes.contains(mimeType)) {
                visitResult = downloadVisitResult
            }
        }

        return visitResult
    }

    private fun getChildren(spongeUri: SpongeUri, document: Document): Set<SpongeUri> {
        return attributeKeys.entries
            .asSequence()
            .map { getChildren(spongeUri, document, it.key, it.value) }
            .flatMap { it }
            .toSet()
    }

    private fun getChildren(
        spongeUri: SpongeUri,
        document: Document,
        cssQuery: String,
        attributeKey: String
    ): Sequence<SpongeUri> {
        return document.select(cssQuery)
            .asSequence()
            .distinct()
            .mapNotNull { toSpongeUri(it, attributeKey) }
            .filter { it != spongeUri && isHostVisitable(it.host) }
    }

    private fun isHostVisitable(host: String): Boolean {
        return host == spongeConfig.spongeUri.host ||
            spongeConfig.includeSubdomains && host.endsWith(spongeConfig.spongeUri.host)
    }

    private suspend fun downloadOrVisitChildren(
        visitResult: VisitResult,
        spongeUri: SpongeUri,
        parents: Set<SpongeUri>
    ) {
        if (visitResult.download) {
            download(spongeUri)
        } else if (parents.size < spongeConfig.maximumDepth) {
            visit(visitResult.children, parents + spongeUri)
        }
    }

    private suspend fun download(spongeUri: SpongeUri) {
        if (downloadedUris.add(spongeUri)) {
            withContext(downloadContext) { spongeService.download(spongeUri) }
        }
    }

    private suspend fun visit(spongeUris: Set<SpongeUri>, parents: Set<SpongeUri>) {
        spongeUris.minus(parents)
            .map { GlobalScope.async(requestContext) { visit(it, parents) } }
            .awaitAll()
    }
}
