/**
 * Copyright © 2019-2023 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.sponge

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.entity.ContentType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private const val NS_TO_S = 1_000_000_000.0
private const val KB_TO_B = 1_000.0

private val htmlMimeTypes = setOf(ContentType.TEXT_HTML.mimeType, ContentType.APPLICATION_XHTML_XML.mimeType)

private val attributeKeys = mapOf(
    "a[href]" to "abs:href",
    "img[src]" to "abs:src"
)

val Throwable.rootMessage: String
    get() = ExceptionUtils.getRootCauseMessage(this)

private fun String.toSpongeURIOrNull(): SpongeURI? {
    return try {
        toSpongeURI()
    } catch (ignored: Exception) {
        null
    }
}

private fun createDispatcher(threadCount: Int) = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()

@OptIn(DelicateCoroutinesApi::class)
class Sponge(private val spongeService: SpongeService, private val spongeConfig: SpongeConfig) {
    private val visitDispatcher = createDispatcher(spongeConfig.concurrentRequests)
    private val downloadDispatcher = createDispatcher(spongeConfig.concurrentDownloads)
    private val visitedURIs = ConcurrentHashMap<SpongeURI, Int>()
    private val downloadedURIs = CopyOnWriteArraySet<SpongeURI>()
    private val completedDownloadCount = AtomicInteger()

    fun execute() {
        try {
            runBlocking {
                visit(spongeConfig.spongeURI, setOf())
            }
        } finally {
            downloadDispatcher.close()
            visitDispatcher.close()
        }
    }

    private suspend fun visit(spongeURI: SpongeURI, parents: Set<SpongeURI>) {
        try {
            if (!canVisit(spongeURI, parents)) return

            val extension = FilenameUtils.getExtension(spongeURI.path)

            if (spongeConfig.fileExtensions.contains(extension)) {
                download(spongeURI)
                return
            }

            val response = spongeService.request(spongeURI)
            val mimeType = ContentType.parse(response.contentType()).mimeType

            if (htmlMimeTypes.contains(mimeType) && parents.size < spongeConfig.maximumDepth) {
                val updatedParents = parents + spongeURI
                val document = Jsoup.parse(response.body(), response.url().toExternalForm())

                getChildren(document, updatedParents).map {
                    GlobalScope.async(visitDispatcher) { visit(it, updatedParents) }
                }.awaitAll()
            } else if (spongeConfig.mimeTypes.contains(mimeType)) {
                download(spongeURI)
            }
        } catch (t: Throwable) {
            System.err.println("⚠ Processing failed for $spongeURI: ${t.rootMessage}")
        }
    }

    private fun canVisit(spongeURI: SpongeURI, parents: Set<SpongeURI>): Boolean {
        synchronized(visitedURIs) {
            if (visitedURIs.containsKey(spongeURI)) {
                if (visitedURIs.getValue(spongeURI) >= parents.size) {
                    return false
                }
            } else if (visitedURIs.size >= spongeConfig.maximumURIs) {
                return false
            }

            visitedURIs[spongeURI] = parents.size

            return true
        }
    }

    private fun getChildren(document: Document, parents: Set<SpongeURI>): Set<SpongeURI> {
        return attributeKeys.entries
            .map { getChildren(document, parents, it.key, it.value) }
            .flatMap { it }
            .toSet()
    }

    private fun getChildren(
        document: Document,
        parents: Set<SpongeURI>,
        cssQuery: String,
        attributeKey: String
    ): Sequence<SpongeURI> {
        return document.select(cssQuery)
            .asSequence()
            .distinct()
            .mapNotNull { it.attr(attributeKey).toSpongeURIOrNull() }
            .filter { !parents.contains(it) && isHostVisitable(it.host) }
    }

    private fun isHostVisitable(host: String): Boolean {
        return host == spongeConfig.spongeURI.host ||
                spongeConfig.includeSubdomains && host.endsWith(spongeConfig.spongeURI.host)
    }

    private suspend fun download(spongeURI: SpongeURI) {
        if (downloadedURIs.add(spongeURI)) {
            val path = getDownloadPath(spongeURI)

            if (!spongeConfig.overwriteExistingFiles && Files.exists(path)) return

            download(spongeURI, path)
        }
    }

    private fun getDownloadPath(spongeURI: SpongeURI): Path {
        return spongeConfig.outputDirectory.resolve(spongeURI.host)
            .resolve(FilenameUtils.getPath(spongeURI.path))
            .resolve(FilenameUtils.getName(spongeURI.path))
            .toAbsolutePath()
    }

    private suspend fun download(spongeURI: SpongeURI, path: Path) =
        GlobalScope.async(visitDispatcher) {
            try {
                val spongeDownload = spongeService.download(spongeURI, path)
                printDownload(path, spongeDownload)
            } catch (t: Throwable) {
                System.err.println("⚠ Downloading failed for $spongeURI: ${t.rootMessage}")
            }
        }.await()

    private fun printDownload(path: Path, spongeDownload: SpongeDownload) {
        val humanSize = FileUtils.byteCountToDisplaySize(spongeDownload.size)
        val speed = NS_TO_S * spongeDownload.size / (KB_TO_B * spongeDownload.duration)
        val humanSpeed = "%.2f kB/s".format(speed)
        val completedDownloads = "${completedDownloadCount.incrementAndGet()}/${downloadedURIs.size}"

        println("↓ $path [$humanSize] [$humanSpeed] [$completedDownloads]")
    }
}
