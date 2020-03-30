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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.entity.ContentType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

private const val NS_TO_S = 1_000_000_000.0
private const val KB_TO_B = 1_000.0

private val skipAction: Action = { _: SpongeUri, _: Set<SpongeUri> -> }
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

typealias Action = suspend (spongeUri: SpongeUri, parents: Set<SpongeUri>) -> Unit

class Sponge(private val spongeService: SpongeService, private val spongeConfig: SpongeConfig) {
    private val requestContext = newFixedThreadPoolContext(spongeConfig.concurrentRequests, "request")
    private val downloadContext = newFixedThreadPoolContext(spongeConfig.concurrentDownloads, "download")
    private val actions = ConcurrentHashMap<SpongeUri, Action>()
    private val downloadedUris = CopyOnWriteArraySet<SpongeUri>()
    private val visitedCount = AtomicInteger()
    private val completedDownloadCount = AtomicInteger()
    private val downloadAction: Action = { spongeUri: SpongeUri, _: Set<SpongeUri> -> download(spongeUri) }

    fun execute() = runBlocking { visit() }

    private suspend fun visit(spongeUri: SpongeUri = spongeConfig.spongeUri, parents: Set<SpongeUri> = setOf()) {
        if (visitedCount.incrementAndGet() > spongeConfig.maximumUris) return

        try {
            val action = actions.computeIfAbsent(spongeUri) { createAction(spongeUri) }

            action(spongeUri, parents)
        } catch (t: Throwable) {
            System.err.println("⚠ Processing failed for $spongeUri: ${t.rootMessage}")
        }
    }

    private fun createAction(spongeUri: SpongeUri): Action {
        val extension = FilenameUtils.getExtension(spongeUri.path)
        var action: Action = skipAction

        if (spongeConfig.fileExtensions.contains(extension)) {
            action = downloadAction
        } else {
            val response = spongeService.request(spongeUri.uri)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (htmlMimeTypes.contains(mimeType)) {
                val document = Jsoup.parse(response.body(), response.url().toExternalForm())
                val children = getChildren(spongeUri, document)

                if (children.isNotEmpty()) {
                    action = createVisitAction(children)
                }
            } else if (spongeConfig.mimeTypes.contains(mimeType)) {
                action = downloadAction
            }
        }

        return action
    }

    private fun createVisitAction(spongeUris: Set<SpongeUri>): Action {
        return { spongeUri: SpongeUri, parents: Set<SpongeUri> ->
            if (parents.size < spongeConfig.maximumDepth) {
                visit(spongeUris, parents + spongeUri)
            }
        }
    }

    private fun getChildren(spongeUri: SpongeUri, document: Document): Set<SpongeUri> {
        return attributeKeys.entries
            .asSequence()
            .map { getChildren(spongeUri, document, it.key, it.value) }
            .flatten()
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
            .filter { isVisitable(it, spongeUri) }
    }

    private fun isVisitable(spongeUri: SpongeUri, parent: SpongeUri) =
        spongeUri != parent && isHostVisitable(spongeUri.host)

    private fun isHostVisitable(host: String): Boolean {
        return host == spongeConfig.spongeUri.host ||
            spongeConfig.includeSubdomains && host.endsWith(spongeConfig.spongeUri.host)
    }

    private suspend fun visit(spongeUris: Set<SpongeUri>, parents: Set<SpongeUri>) {
        spongeUris.minus(parents)
            .map { GlobalScope.async(requestContext) { visit(it, parents) } }
            .awaitAll()
    }

    private suspend fun download(spongeUri: SpongeUri) {
        if (downloadedUris.add(spongeUri)) {
            val path = getDownloadPath(spongeUri)

            if (!spongeConfig.overwriteExistingFiles && Files.exists(path)) {
                println("∃ $path")
            } else {
                download(spongeUri.uri, path)
            }
        }
    }

    private fun getDownloadPath(spongeUri: SpongeUri): Path {
        return spongeConfig.outputDirectory.resolve(spongeUri.host)
            .resolve(FilenameUtils.getPath(spongeUri.path))
            .resolve(FilenameUtils.getName(spongeUri.path))
            .toAbsolutePath()
    }

    private suspend fun download(uri: String, path: Path) {
        withContext(downloadContext) {
            val spongeDownload = spongeService.download(uri, path)

            printSpongeDownload(path, spongeDownload)
        }
    }

    private fun printSpongeDownload(path: Path, spongeDownload: SpongeDownload) {
        val humanSize = FileUtils.byteCountToDisplaySize(spongeDownload.size)
        val speed = NS_TO_S * spongeDownload.size / (KB_TO_B * spongeDownload.duration)
        val humanSpeed = "%.2f kB/s".format(speed)
        val completedDownloads = "${completedDownloadCount.incrementAndGet()}/${downloadedUris.size}"

        println("↓ $path [$humanSize] [$humanSpeed] [$completedDownloads]")
    }
}
