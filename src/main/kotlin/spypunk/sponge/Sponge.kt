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
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

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
    private inner class VisitChildrenSpongeAction(val children: Set<SpongeUri>) : SpongeAction {
        override suspend fun execute(spongeUri: SpongeUri, parents: Set<SpongeUri>) {
            if (parents.size < spongeConfig.maximumDepth) {
                visit(children, parents + spongeUri)
            }
        }

        private suspend fun visit(spongeUris: Set<SpongeUri>, parents: Set<SpongeUri>) {
            spongeUris.minus(parents)
                .map { GlobalScope.async(requestContext) { visit(it, parents) } }
                .awaitAll()
        }
    }

    private val downloadSpongeAction = object : SpongeAction {
        override suspend fun execute(spongeUri: SpongeUri, parents: Set<SpongeUri>) {
            if (downloadedUris.add(spongeUri)) {
                withContext(downloadContext) { spongeService.download(spongeUri) }
            }
        }
    }

    private val requestContext = newFixedThreadPoolContext(spongeConfig.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeConfig.concurrentDownloads, "download")
    private val spongeActions = ConcurrentHashMap<SpongeUri, SpongeAction>()
    private val downloadedUris = CopyOnWriteArraySet<SpongeUri>()
    private val visitedCount = AtomicInteger()

    fun execute() = runBlocking { visit() }

    private suspend fun visit(spongeUri: SpongeUri = spongeConfig.spongeUri, parents: Set<SpongeUri> = setOf()) {
        if (visitedCount.incrementAndGet() > spongeConfig.maximumUris) return

        try {
            getSpongeAction(spongeUri).execute(spongeUri, parents)
        } catch (t: Throwable) {
            System.err.println("⚠ Processing failed for $spongeUri: ${t.rootMessage}")
        }
    }

    private fun getSpongeAction(spongeUri: SpongeUri) =
        spongeActions.computeIfAbsent(spongeUri) { createSpongeAction(spongeUri) }

    private fun createSpongeAction(spongeUri: SpongeUri): SpongeAction {
        val extension = FilenameUtils.getExtension(spongeUri.path)

        if (spongeConfig.fileExtensions.contains(extension)) return downloadSpongeAction

        val response = spongeService.request(spongeUri)
        val mimeType = ContentType.parse(response.contentType()).mimeType

        return createSpongeAction(spongeUri, mimeType, response)
    }

    private fun createSpongeAction(
        spongeUri: SpongeUri,
        mimeType: String,
        response: Connection.Response
    ): SpongeAction {
        var spongeAction: SpongeAction = doNothingSpongeAction

        if (htmlMimeTypes.contains(mimeType)) {
            val document = Jsoup.parse(response.body(), response.url().toExternalForm())
            val children = getChildren(spongeUri, document)

            if (children.isNotEmpty()) {
                spongeAction = VisitChildrenSpongeAction(children)
            }
        } else if (spongeConfig.mimeTypes.contains(mimeType)) {
            spongeAction = downloadSpongeAction
        }

        return spongeAction
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
}
